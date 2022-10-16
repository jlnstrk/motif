use std::collections::{HashMap, HashSet};
use std::fmt::Debug;
use std::mem::take;
use std::sync::Arc;

use async_graphql::futures_util::Stream;
use async_stream::stream;
use async_trait::async_trait;
use futures::executor::block_on;
use log::{info, log};
use tokio::sync::{mpsc, oneshot, Mutex};

use crate::pubsub::private;

pub type PubSubSender<V> = mpsc::Sender<V>;
pub type PubSubReceiver<V> = mpsc::Receiver<V>;
pub type PubSubCommandSender<V> = mpsc::Sender<PubSubCommand<V>>;
pub type PubSubHandler<V> = mpsc::Receiver<PubSubCommand<V>>;
pub type PubSubCancellationSender = oneshot::Sender<()>;
pub type PubSubCancellationReceiver = oneshot::Receiver<()>;

#[async_trait]
pub trait PubSubEngine<V: Clone + Debug + Sync + Send + 'static>: private::PubSubStart<V>
where
    V: Clone,
    V: Sync,
{
    async fn connect(
        &mut self,
        arc: Arc<Box<Mutex<dyn PubSubEngine<V> + Send + Sync>>>,
    ) -> (PubSubCommandSender<V>, PubSubCancellationSender);
    async fn disconnect(&self);
    async fn subscribe_to_topic(&self, topic: String);
    async fn unsubscribe_from_topic(&self, topic: String);
    async fn publish(&self, topic: String, message: V);
}

pub struct PubSub<V: Clone + Debug + Sync + Send + 'static> {
    engine: Arc<Box<Mutex<dyn PubSubEngine<V> + Sync + Send>>>,
    command_sender: PubSubCommandSender<V>,
    cancel: Option<PubSubCancellationSender>,
}

#[derive(Clone)]
pub struct PubSubHandle<V: Clone + Debug + Sync + Send> {
    command_sender: PubSubCommandSender<V>,
}

impl<V: Clone + Debug + Sync + Send> PubSubHandle<V> {
    pub(crate) async fn from(pubsub: &PubSub<V>) -> Self {
        Self {
            command_sender: pubsub.command_sender.clone(),
        }
    }

    pub async fn publish(&self, topic: String, message: V) {
        self.command_sender
            .send(PubSubCommand::Outgoing { topic, message })
            .await
            .unwrap();
    }

    pub async fn subscribe(&self, topics: Vec<String>) -> PubSubSubscriptionHandle<V> {
        PubSubSubscriptionHandle::new(self.command_sender.clone(), topics).await
    }
}

impl<V: Clone + Debug + Sync + Send + 'static> PubSub<V> {
    pub(crate) async fn connect(engine: Box<Mutex<dyn PubSubEngine<V> + Sync + Send>>) -> Self {
        let arc: Arc<Box<Mutex<dyn PubSubEngine<V> + Sync + Send>>> = Arc::new(engine);
        let mut guard = arc.lock().await;
        let (register, cancel) = guard.connect(arc.clone()).await;
        Self {
            engine: arc.clone(),
            command_sender: register,
            cancel: Some(cancel),
        }
    }
}

impl<V: Clone + Debug + Sync + Send + 'static> Drop for PubSub<V> {
    fn drop(&mut self) {
        let cancel = take(&mut self.cancel);
        cancel.unwrap().send(()).unwrap();
        let guard = self.engine.try_lock().unwrap();
        block_on(async move { guard.disconnect().await });
    }
}

#[derive(Debug)]
pub enum PubSubCommand<V> {
    Subscribe {
        topics: Vec<String>,
        ans: oneshot::Sender<(i64, PubSubReceiver<V>)>,
    },
    Unsubscribe {
        subscription_id: i64,
    },
    Incoming {
        topic: String,
        message: V,
    },
    Outgoing {
        topic: String,
        message: V,
    },
}

#[derive(Debug)]
pub struct PubSubSubscriptionHandle<V> {
    id: i64,
    receiver: PubSubReceiver<V>,
    unregister: mpsc::Sender<PubSubCommand<V>>,
}

impl<V: Debug> PubSubSubscriptionHandle<V> {
    async fn new(
        command_sender: PubSubCommandSender<V>,
        topics: Vec<String>,
    ) -> PubSubSubscriptionHandle<V> {
        let (ans_sender, ans_recv) = oneshot::channel::<(i64, PubSubReceiver<V>)>();
        command_sender
            .send(PubSubCommand::Subscribe {
                topics: topics.clone(),
                ans: ans_sender,
            })
            .await
            .unwrap();
        let (id, receiver) = ans_recv.await.unwrap();
        info!("PubSub: Obtained handle");
        PubSubSubscriptionHandle {
            id,
            receiver,
            unregister: command_sender,
        }
    }

    pub async fn receive(&mut self) -> Option<V> {
        self.receiver.recv().await
    }

    pub fn stream(mut self) -> impl Stream<Item = V> {
        stream! {
            while let Some(value) = self.receiver.recv().await {
                yield value;
            }
        }
    }
}

impl<V> Drop for PubSubSubscriptionHandle<V> {
    fn drop(&mut self) {
        let unsubscribe = PubSubCommand::Unsubscribe {
            subscription_id: self.id,
        };
        if let None = self.unregister.try_send(unsubscribe).ok() {
            info!("PubSub: Failed to release pubsub subscription!");
        }
        {
            info!("PubSub: Released handle");
        }
    }
}

pub struct SubscriberMaps<V> {
    pub sub_ids_to_senders: HashMap<i64, PubSubSender<V>>,
    pub sub_ids_to_topics: HashMap<i64, HashSet<String>>,
    pub topics_to_sub_ids: HashMap<String, HashSet<i64>>,
}
