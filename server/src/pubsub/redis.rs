/*
 * Copyright 2022 Julian Ostarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

use std::sync::Arc;

use async_graphql::futures_util::future::{abortable, AbortHandle};
use async_trait::async_trait;
use fred::clients::{RedisClient, SubscriberClient};
use fred::prelude::{ClientLike, PubsubInterface, ReconnectPolicy, RedisConfig};
use fred::types::RedisValue;
use log::{error, info};
use tokio::sync::Mutex;

use crate::pubsub::prelude::PubSubCommand::Incoming;
use crate::pubsub::prelude::{PubSubCancellationSender, PubSubCommandSender, PubSubEngine};
use crate::pubsub::private;
use crate::pubsub::private::PubSubStart;
use crate::{pin_mut, StreamExt};

pub struct RedisPubSubEngine {
    url: String,
    subscriber: Arc<SubscriberClient>,
    publisher: Arc<RedisClient>,
    abort: Option<AbortHandle>,
}

impl RedisPubSubEngine {
    pub(crate) async fn new(url: String) -> Box<Mutex<Self>> {
        let config = RedisConfig::from_url(&url).unwrap();
        let subscriber = Arc::new(SubscriberClient::new(config.clone()));
        let publisher = Arc::new(RedisClient::new(config));
        Box::new(Mutex::new(Self {
            url,
            subscriber,
            publisher,
            abort: None,
        }))
    }
}

#[async_trait]
impl PubSubEngine<RedisValue> for RedisPubSubEngine {
    async fn connect(
        &mut self,
        self_ref: Arc<Box<Mutex<dyn PubSubEngine<RedisValue> + Send + Sync>>>,
    ) -> (PubSubCommandSender<RedisValue>, PubSubCancellationSender) {
        self.subscriber.connect(Some(ReconnectPolicy::default()));
        self.subscriber.wait_for_connect().await.unwrap();
        info!("Redis Subscriber: Connected to {}", self.url);
        self.publisher.connect(Some(ReconnectPolicy::default()));
        self.publisher.wait_for_connect().await.unwrap();
        info!("Redis Publisher: Connected to {}", self.url);

        let (command_sender, cancellation_sender) = self.start_handler(self_ref).await;
        let command_sender_copy = command_sender.clone();
        let redis_copy = self.subscriber.clone();
        let (task, handle) = abortable(async move {
            let stream = (&redis_copy).on_message();

            pin_mut!(stream);
            while let Some((channel, message)) = stream.next().await {
                info!("Redis: Message received");

                command_sender_copy
                    .clone()
                    .send(Incoming {
                        topic: channel,
                        message: message.clone(),
                    })
                    .await
                    .unwrap();
            }
        });
        self.abort = Some(handle);
        tokio::spawn(task);

        (command_sender, cancellation_sender)
    }
    async fn disconnect(&self) {
        self.subscriber
            .quit()
            .await
            .expect("Redis: Failed to quit subscriber");
        self.publisher
            .quit()
            .await
            .expect("Redis: Failed to quit publisher");
    }

    async fn subscribe_to_topic(&self, topic: String) {
        info!("Redis: Subscribing to topic \"{}\"", topic);
        self.subscriber.subscribe(topic).await.unwrap();
    }

    async fn unsubscribe_from_topic(&self, topic: String) {
        info!("Redis: Unsubscribing from topic \"{}\"", topic);
        self.subscriber.unsubscribe(topic).await.unwrap();
    }

    async fn publish(&self, topic: String, message: RedisValue) {
        info!("Redis: Publishing message!");
        if let Some(err) = self
            .publisher
            .publish::<i32, _, _>(topic, message)
            .await
            .err()
        {
            error!("Redis: Publish error: {}", err);
        }
    }
}

impl private::PubSubStart<RedisValue> for RedisPubSubEngine {}
