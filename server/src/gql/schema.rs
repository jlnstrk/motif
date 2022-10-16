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
