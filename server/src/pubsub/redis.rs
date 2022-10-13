use crate::pubsub::prelude::PubSubCommand::Incoming;
use crate::pubsub::prelude::{PubSubCancellationSender, PubSubCommandSender, PubSubEngine};
use crate::pubsub::private::PubSubStart;
use crate::pubsub::{private, redis};
use crate::{pin_mut, Str, StreamExt};
use async_graphql::futures_util::future::{abortable, AbortHandle};
use async_trait::async_trait;
use fred::clients::SubscriberClient;
use fred::prelude::{ClientLike, PubsubInterface, ReconnectPolicy, RedisConfig, RedisError};
use fred::types::RedisValue;
use log::{info, log, Level};
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;
use tokio::sync::Mutex;

pub struct RedisPubSubEngine {
    url: String,
    redis: Arc<SubscriberClient>,
    abortable: Option<AbortHandle>,
}

impl RedisPubSubEngine {
    pub(crate) async fn new(url: String) -> Box<Mutex<Self>> {
        let config = RedisConfig::from_url(&url).unwrap();
        let redis = Arc::new(SubscriberClient::new(config));
        Box::new(Mutex::new(Self {
            url,
            redis,
            abortable: None,
        }))
    }
}

#[async_trait]
impl PubSubEngine<RedisValue> for RedisPubSubEngine {
    async fn connect(
        &mut self,
        self_ref: Arc<Box<Mutex<dyn PubSubEngine<RedisValue> + Send + Sync>>>,
    ) -> (PubSubCommandSender<RedisValue>, PubSubCancellationSender) {
        let policy = ReconnectPolicy::default();
        self.redis.connect(Some(policy));
        self.redis.wait_for_connect().await.unwrap();
        info!("Connected to redis at {}", self.url);

        let (command_sender, cancellation_sender) = self.start_handler(self_ref).await;
        let command_sender_copy = command_sender.clone();
        let redis_copy = self.redis.clone();
        let (task, handle) = abortable(async move {
            let stream = (&redis_copy).on_message();

            pin_mut!(stream);
            while let Some((channel, message)) = stream.next().await {
                println!("message!");

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
        self.abortable = Some(handle);
        tokio::spawn(task);

        (command_sender, cancellation_sender)
    }
    async fn disconnect(&self) {
        self.redis.quit().await.expect("Failed to quit redis");
    }

    async fn subscribe_to_topic(&self, topic: String) {
        info!("Redis: Subscribing to topic \"{}\"", topic);
        self.redis.subscribe(topic).await.unwrap();
    }

    async fn unsubscribe_from_topic(&self, topic: String) {
        info!("Redis: Unsubscribing from topic \"{}\"", topic);
        self.redis.unsubscribe(topic).await.unwrap();
    }

    async fn publish(&self, topic: String, message: RedisValue) {
        info!("Redis: Publishing message!");
        self.redis
            .publish::<i32, _, _>(topic, message)
            .await
            .unwrap();
    }
}

impl private::PubSubStart<RedisValue> for RedisPubSubEngine {}
