use entity::sea_orm_active_enums::Service as DbService;

use crate::domain::common::typedef::Service;

impl From<DbService> for Service {
    fn from(db_type: DbService) -> Self {
        match db_type {
            DbService::AppleMusic => Service::AppleMusic,
            DbService::Spotify => Service::Spotify,
        }
    }
}

impl From<Service> for DbService {
    fn from(db_type: Service) -> Self {
        match db_type {
            Service::AppleMusic => DbService::AppleMusic,
            Service::Spotify => DbService::Spotify,
        }
    }
}
