use uuid::Uuid;

pub fn topic_motif_created(author_id: Uuid) -> String {
    format!("MOTIF_CREATED.{}", author_id)
}

pub fn topic_motif_deleted(author_id: Uuid) -> String {
    format!("MOTIF_DELETED.{}", author_id)
}

pub fn topic_motif_listened(motif_id: i32) -> String {
    format!("MOTIF_LISTENED.{}", motif_id)
}
