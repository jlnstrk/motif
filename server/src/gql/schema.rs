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

use async_graphql::{MergedObject, MergedSubscription, Schema};

use crate::domain::collection::resolver::{CollectionMutation, CollectionQuery};
use crate::domain::comment::resolver::{CommentMutation, CommentQuery};
use crate::domain::like::resolver::{LikeMutation, LikeSubscription};
use crate::domain::motif::resolver::{MotifMutation, MotifQuery, MotifSubscription};
use crate::domain::profile::resolver::{ProfileMutation, ProfileQuery, ProfileSubscription};

#[derive(MergedObject, Default)]
pub struct Query(ProfileQuery, MotifQuery, CommentQuery, CollectionQuery);

#[derive(MergedObject, Default)]
pub struct Mutation(
    ProfileMutation,
    MotifMutation,
    CommentMutation,
    CollectionMutation,
    LikeMutation,
);

#[derive(MergedSubscription, Default)]
pub struct Subscription(ProfileSubscription, MotifSubscription, LikeSubscription);

pub type AppSchema = Schema<Query, Mutation, Subscription>;
