use std::sync::Arc;

use async_graphql::futures_util::future::{abortable, AbortHandle};
use async_trait::async_trait;
use fred::clients::{RedisClient, SubscriberClient};
use fred::prelude::{ClientLike, PubsubInterface, ReconnectPolicy, RedisConfig};
use fred::types::RedisValue;
use log::{error, info, log};
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
        self.abort = Some(handle);
        tokio::spawn(task);

        (command_sender, cancellation_sender)
    }
    async fn disconnect(&self) {
        self.subscriber
            .quit()
            .await
            .expect("Failed to quit subscriber redis");
        self.publisher
            .quit()
            .await
            .expect("Failed to quit publisher redis");
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
