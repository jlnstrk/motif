pub fn topic_comment_created(motif_id: i32) -> String {
    format!("COMMENT_CREATED.{}", motif_id)
}

pub fn topic_comment_deleted(motif_id: i32) -> String {
    format!("COMMENT_DELETED.{}", motif_id)
}
