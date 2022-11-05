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

use std::collections::{HashMap, HashSet};
use std::fmt::Debug;
use std::sync::Arc;

use async_graphql::futures_util::future::select;
use async_trait::async_trait;
use log::info;
use tokio::sync::{mpsc, oneshot, Mutex};

use crate::pubsub::prelude::{
    PubSubCancellationReceiver, PubSubCancellationSender, PubSubCommand, PubSubCommandSender,
    PubSubEngine, PubSubHandler, PubSubSender, SubscriberMaps,
};

#[async_trait]
pub trait PubSubStart<V: Clone + Debug + Sized + Send + Sync + 'static> {
    async fn start_handler(
        &self,
        engine_ref: Arc<Box<Mutex<dyn PubSubEngine<V> + Sync + Send>>>,
    ) -> (PubSubCommandSender<V>, PubSubCancellationSender)
    where
        Self: Sized,
    {
        let (cancel_sender, cancel_receiver) = oneshot::channel();
        let (register_sender, handler_receiver) = mpsc::channel::<PubSubCommand<V>>(32);
        pubsub_main(engine_ref, handler_receiver, cancel_receiver).await;
        (register_sender, cancel_sender)
    }
}

async fn pubsub_main<V>(
    engine: Arc<Box<Mutex<dyn PubSubEngine<V> + Sync + Send>>>,
    handler_receiver: PubSubHandler<V>,
    cancellation: PubSubCancellationReceiver,
) where
    V: Clone + Debug + Sync + Send + 'static,
{
    tokio::spawn(select(
        Box::pin(async move {
            let mut sub_id_counter = 0i64;
            let mut subscriber_maps = SubscriberMaps {
                sub_ids_to_senders: HashMap::<i64, PubSubSender<V>>::new(),
                sub_ids_to_topics: HashMap::<i64, HashSet<String>>::new(),
                topics_to_sub_ids: HashMap::<String, HashSet<i64>>::new(),
            };

            info!("PubSub: Handler ready to receive commands!");

            let mut handler_receiver = handler_receiver;
            while let Some(command) = handler_receiver.recv().await {
                match command {
                    PubSubCommand::Subscribe { topics, ans } => {
                        info!("PubSub: Subscribe");
                        let subscription_id = sub_id_counter;
                        sub_id_counter += 1;

                        let (sub_sender, sub_receiver) = mpsc::channel::<V>(1);
                        ans.send((subscription_id, sub_receiver)).unwrap();

                        subscriber_maps
                            .sub_ids_to_senders
                            .insert(subscription_id, sub_sender);
                        subscriber_maps
                            .sub_ids_to_topics
                            .insert(subscription_id, HashSet::from_iter(topics.clone()));
                        for topic in &topics {
                            let existing = subscriber_maps.topics_to_sub_ids.get_mut(topic);
                            if let Some(ids) = existing {
                                if ids.is_empty() {
                                    {
                                        let guard = engine.lock().await;
                                        guard.subscribe_to_topic(topic.clone()).await;
                                    }
                                }
                                ids.insert(subscription_id);
                            } else {
                                {
                                    let guard = engine.lock().await;
                                    guard.subscribe_to_topic(topic.clone()).await;
                                }
                                let mut ids = HashSet::new();
                                ids.insert(subscription_id);
                                subscriber_maps.topics_to_sub_ids.insert(topic.clone(), ids);
                            }
                        }
                    }
                    PubSubCommand::Unsubscribe { subscription_id } => {
                        info!("PubSub: Unsubscribe");
                        subscriber_maps.sub_ids_to_senders.remove(&subscription_id);
                        if let Some(topics) =
                            subscriber_maps.sub_ids_to_topics.remove(&subscription_id)
                        {
                            for topic in topics {
                                if let Some(topic_subs) =
                                    subscriber_maps.topics_to_sub_ids.get_mut(&topic)
                                {
                                    topic_subs.remove(&subscription_id);
                                    if topic_subs.is_empty() {
                                        {
                                            let guard = engine.lock().await;
                                            guard.unsubscribe_from_topic(topic.clone()).await;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    PubSubCommand::Outgoing { topic, message } => {
                        info!("PubSub: Outgoing message");
                        let guard = engine.lock().await;
                        guard.publish(topic, message).await;
                    }
                    PubSubCommand::Incoming { topic, message } => {
                        info!("PubSub: Incoming message");
                        if let Some(sub_ids) = subscriber_maps.topics_to_sub_ids.get(&topic) {
                            for dist in sub_ids {
                                if let Some(sender) = subscriber_maps.sub_ids_to_senders.get(dist) {
                                    sender.try_send(message.clone()).unwrap();
                                }
                            }
                        }
                    }
                }
            }
        }),
        cancellation,
    ));
}
