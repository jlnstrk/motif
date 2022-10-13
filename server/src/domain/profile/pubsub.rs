use uuid::Uuid;

pub fn topic_profile_updated(profile_id: Uuid) -> String {
    format!("PROFILE_UPDATED.{}", profile_id.as_hyphenated())
}

pub fn topic_profile_followed(profile_id: Uuid) -> String {
    format!("PROFILE_FOLLOWED.{}", profile_id.as_hyphenated())
}
