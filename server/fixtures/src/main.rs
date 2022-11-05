use std::env;
use std::error::Error;
use std::iter::IntoIterator;
use std::ops::Range;

use dotenvy::dotenv;
use lipsum::lipsum_words;
use rand::distributions::Uniform;
use rand::Rng;
use sea_orm::prelude::Uuid;
use sea_orm::ActiveValue::Set;
use sea_orm::{ActiveModelTrait, Database, DbErr, NotSet, TransactionTrait};

use entity::{
    comment_likes, comments, motif_likes, motif_listeners, motifs, profile_follows, profiles, users,
};

mod isrc;
mod user;

fn make_motif(creator_id: Uuid, isrc: String) -> motifs::ActiveModel {
    motifs::ActiveModel {
        id: Default::default(),
        isrc: Set(isrc),
        offset: Set(0),
        created_at: Default::default(),
        creator_id: Set(creator_id),
    }
}

fn make_user(fixture: &user::UserFixture) -> users::ActiveModel {
    users::ActiveModel {
        id: Default::default(),
        email: Set(fixture.email.to_owned()),
        created_at: Default::default(),
        updated_at: Default::default(),
    }
}

fn make_profile(user_id: Uuid, fixture: &user::UserFixture) -> profiles::ActiveModel {
    profiles::ActiveModel {
        user_id: Set(user_id),
        username: Set(fixture.username.to_owned()),
        biography: Set(fixture.biography.map(|str| str.to_owned())),
        photo_url: Set(Some(fixture.photo_url.to_owned())),
        display_name: Set(fixture.display_name.to_owned()),
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    dotenv().ok();
    let database_url = env::var("DATABASE_URL").expect("DATABASE_URL must be set");
    let db = Database::connect(database_url).await?;

    db.transaction::<_, (), DbErr>(|txn| {
        Box::pin(async move {
            let mut profiles: Vec<profiles::Model> = Vec::new();
            let mut motifs: Vec<motifs::Model> = Vec::new();
            let mut comments: Vec<comments::Model> = Vec::new();

            for user_fixture in user::fixtures {
                let user = make_user(&user_fixture).insert(txn).await?;
                let profile = make_profile(user.id, &user_fixture).insert(txn).await?;

                for isrc in rnd_of_n(&isrc::fixtures.to_vec(), 4) {
                    let motif = make_motif(profile.user_id, (*isrc).to_string())
                        .insert(txn)
                        .await?;
                    motifs.push(motif);
                }

                profiles.push(profile);
            }

            // Random listens
            for p in &profiles {
                for m in rnd_of_n(&motifs, 10) {
                    let _ = motif_listeners::ActiveModel {
                        motif_id: Set(m.id),
                        listener_id: Set(p.user_id),
                        listened_at: NotSet,
                    }
                    .insert(txn)
                    .await?;
                }
            }

            // Everyone follows each other
            for i in 0..profiles.len() {
                for j in i..profiles.len() {
                    let a = &profiles[i];
                    let b = &profiles[j];
                    if a != b {
                        profile_follows::ActiveModel {
                            follower_id: Set(a.user_id),
                            followed_id: Set(b.user_id),
                        }
                        .insert(txn)
                        .await?;
                        profile_follows::ActiveModel {
                            follower_id: Set(b.user_id),
                            followed_id: Set(a.user_id),
                        }
                        .insert(txn)
                        .await?;
                    }
                }
            }

            // Everyone likes something
            for p in &profiles {
                for m in &motifs {
                    motif_likes::ActiveModel {
                        motif_id: Set(m.id),
                        liker_id: Set(p.user_id),
                    }
                    .insert(txn)
                    .await?;
                }

                for m in rnd_of_n(&motifs, 5) {
                    let comment = comments::ActiveModel {
                        id: Default::default(),
                        motif_id: Set(m.id),
                        parent_id: Default::default(),
                        offset: Default::default(),
                        content: Set(lipsum_words(rnd_in(1..11))),
                        author_id: Set(p.user_id),
                        created_at: Default::default(),
                    }
                    .insert(txn)
                    .await?;
                    comments.push(comment);
                }
            }

            // Random sub comments
            for p in &profiles {
                let mut sub_comments: Vec<comments::Model> = Vec::new();
                for c in rnd_of_n(&comments, 3) {
                    let sub_comment = comments::ActiveModel {
                        id: Default::default(),
                        motif_id: Set(c.motif_id),
                        parent_id: Set(Some(c.id)),
                        offset: Default::default(),
                        content: Set(lipsum_words(rnd_in(1..11))),
                        author_id: Set(p.user_id),
                        created_at: Default::default(),
                    }
                    .insert(txn)
                    .await?;
                    sub_comments.push(sub_comment);
                }
                comments.append(&mut sub_comments);
            }

            // Random comment likes
            for p in &profiles {
                for c in rnd_of_n(&comments, 3) {
                    let _ = comment_likes::ActiveModel {
                        comment_id: Set(c.id),
                        liker_id: Set(p.user_id),
                    }
                    .insert(txn)
                    .await?;
                }
            }

            Ok(())
        })
    })
    .await
    .map_err(|err| err.into())
}

fn rnd_in(range: Range<usize>) -> usize {
    rand::thread_rng().gen_range(range)
}

fn rnd_of_n<T>(values: &Vec<T>, at_most: usize) -> Vec<&T> {
    rnd_in_n(0..values.len(), at_most)
        .into_iter()
        .map(|idx| &values[idx])
        .collect()
}

fn rnd_in_n(range: Range<usize>, at_most: usize) -> Vec<usize> {
    let rng = Uniform::from(range);
    let mut values: Vec<usize> = rand::thread_rng().sample_iter(&rng).take(at_most).collect();
    values.sort();
    values.dedup();
    values
}
