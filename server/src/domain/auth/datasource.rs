use std::error::Error;

use chrono::{FixedOffset, Utc};
use sea_orm::ActiveValue::Set;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, Condition, DatabaseConnection, DbErr, EntityTrait,
    IntoActiveModel, NotSet, QueryFilter, TransactionTrait,
};
use uuid::Uuid;

use entity::service_credentials::{
    Entity as ServiceCredentialsEntity, Model as ServiceCredentialsModel,
};
use entity::{profiles, sea_orm_active_enums, service_credentials, users};

use crate::domain::auth::dto::{ServiceCredentialsDto, ServiceLoginDto};
use crate::domain::auth::typedef::ServiceToken::Full;
use crate::domain::auth::typedef::{FullServiceToken, ServiceToken};
use crate::domain::common::typedef::Service;
use anyhow::{Context, Result};
use serde::ser;

impl From<ServiceCredentialsModel> for FullServiceToken {
    fn from(model: ServiceCredentialsModel) -> Self {
        Self {
            service: Service::from(model.service),
            service_id: model.service_id,
            access_token: model.access_token,
            access_token_expires: model.access_token_expires.map(|dt| dt.with_timezone(&Utc)),
            refresh_token: model.refresh_token,
            refresh_token_expires: model.refresh_token_expires.map(|dt| dt.with_timezone(&Utc)),
        }
    }
}

pub fn build_username(display_name: &String) -> String {
    let mut username: String = display_name
        .chars()
        .filter_map(|c| if c.is_alphabetic() { Some(c) } else { None })
        .collect();
    username += &rand::random::<u16>().to_string();
    username
}

pub async fn get_credentials_by_user_and_service(
    db: &DatabaseConnection,
    user_id: Uuid,
    service: Service,
) -> Result<FullServiceToken> {
    let service: sea_orm_active_enums::Service = service.into();
    let model = ServiceCredentialsEntity::find()
        .filter(
            Condition::all()
                .add(service_credentials::Column::UserId.eq(user_id))
                .add(service_credentials::Column::Service.eq(service)),
        )
        .one(db)
        .await?;
    let model: ServiceCredentialsModel = model.context("Credentials not found")?;
    Ok(model.into())
}

pub async fn update_from_credentials(
    db: &DatabaseConnection,
    dto: &ServiceCredentialsDto,
) -> Result<FullServiceToken> {
    let service: sea_orm_active_enums::Service = dto.service.into();
    let existing = ServiceCredentialsEntity::find()
        .filter(
            Condition::all()
                .add(service_credentials::Column::Service.eq(service))
                .add(service_credentials::Column::ServiceId.eq(dto.service_id.clone())),
        )
        .one(db)
        .await?
        .context("No credentials to update")?;
    let mut existing = existing.into_active_model();
    existing.access_token = Set(dto.access_token.clone());
    existing.access_token_expires = Set(dto
        .access_token_expires
        .map(|dt| dt.with_timezone(&FixedOffset::east(0))));
    existing.refresh_token = Set(dto.refresh_token.clone());
    existing.refresh_token_expires = Set(dto
        .refresh_token_expires
        .map(|dt| dt.with_timezone(&FixedOffset::east(0))));
    let existing = existing.update(db).await?;

    Ok(existing.into())
}

pub async fn upsert_from_service_login(
    db: &DatabaseConnection,
    dto: ServiceLoginDto,
) -> Result<FullServiceToken, impl Error> {
    let service: sea_orm_active_enums::Service = dto.credentials.service.into();
    db.transaction::<_, FullServiceToken, DbErr>(|txn| {
        Box::pin(async move {
            let existing = ServiceCredentialsEntity::find()
                .filter(
                    Condition::all()
                        .add(service_credentials::Column::Service.eq(service))
                        .add(
                            service_credentials::Column::ServiceId
                                .eq(dto.credentials.service_id.clone()),
                        ),
                )
                .one(txn)
                .await?;

            if let Some(existing) = existing {
                let mut credentials = existing.into_active_model();
                credentials.access_token = Set(dto.credentials.access_token.clone());
                credentials.access_token_expires = Set(dto
                    .credentials
                    .access_token_expires
                    .map(|dt| dt.with_timezone(&FixedOffset::east(0))));
                credentials.refresh_token = Set(dto.credentials.refresh_token.clone());
                credentials.refresh_token_expires = Set(dto
                    .credentials
                    .refresh_token_expires
                    .map(|dt| dt.with_timezone(&FixedOffset::east(0))));
                let credentials = credentials.update(txn).await?;

                Ok(credentials.into())
            } else {
                let user = users::ActiveModel {
                    id: NotSet,
                    email: Set(dto.account.email.clone()),
                    created_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
                    updated_at: Set(Utc::now().with_timezone(&FixedOffset::east(0))),
                };
                let user = user.insert(txn).await?;

                let username = build_username(&dto.account.display_name);
                let profile = profiles::ActiveModel {
                    user_id: Set(user.id),
                    username: Set(username),
                    biography: Set(None),
                    photo_url: Set(dto.account.photo_url.clone()),
                    display_name: Set(dto.account.display_name.clone()),
                };
                profile.insert(txn).await?;

                let credentials = service_credentials::ActiveModel {
                    user_id: Set(user.id),
                    service: Set(dto.credentials.service.into()),
                    service_id: Set(dto.credentials.service_id.clone()),
                    access_token: Set(dto.credentials.access_token.clone()),
                    access_token_expires: Set(dto
                        .credentials
                        .access_token_expires
                        .map(|dt| dt.with_timezone(&FixedOffset::east(0)))),
                    refresh_token: Set(dto.credentials.refresh_token.clone()),
                    refresh_token_expires: Set(dto
                        .credentials
                        .refresh_token_expires
                        .map(|dt| dt.with_timezone(&FixedOffset::east(0)))),
                };

                let credentials = credentials.insert(txn).await?;

                Ok(credentials.into())
            }
        })
    })
    .await
}
