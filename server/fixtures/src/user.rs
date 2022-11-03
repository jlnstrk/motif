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

pub struct UserFixture {
    pub email: &'static str,
    pub display_name: &'static str,
    pub username: &'static str,
    pub photo_url: &'static str,
    pub biography: Option<&'static str>,
}

pub const fixtures: &'static [UserFixture] = &[
    UserFixture {
        email: "jules.mathieu@example.com",
        display_name: "Jules Mathieu",
        username: "happybear736",
        photo_url: "https://randomuser.me/api/portraits/men/41.jpg",
        biography: None,
    },
    UserFixture {
        email: "elias.hansen@example.com",
        display_name: "Elias Hansen",
        username: "purplefish701",
        photo_url: "https://randomuser.me/api/portraits/men/92.jpg",
        biography: None,
    },
    UserFixture {
        email: "blagovist.paskevich@example.com",
        display_name: "Blagovist Paskevich",
        username: "brownwolf750",
        photo_url: "https://randomuser.me/api/portraits/men/34.jpg",
        biography: None,
    },
    UserFixture {
        email: "deniz.koyluoglu@example.com",
        display_name: "Deniz Köylüoğlu",
        username: "happydog113",
        photo_url: "https://randomuser.me/api/portraits/women/18.jpg",
        biography: None,
    },
    UserFixture {
        email: "dafina.tentije@example.com",
        display_name: "Dafina Ten Tije",
        username: "whitemeercat382",
        photo_url: "https://randomuser.me/api/portraits/women/14.jpg",
        biography: None,
    },
    UserFixture {
        email: "belen.carmona@example.com",
        display_name: "Belén Carmona",
        username: "organicgorilla750",
        photo_url: "https://randomuser.me/api/portraits/women/80.jpg",
        biography: None,
    },
    UserFixture {
        email: "mariano.munoz@example.com",
        display_name: "Mariano Muñoz",
        username: "lazyleopard838",
        photo_url: "https://randomuser.me/api/portraits/men/29.jpg",
        biography: None,
    },
    UserFixture {
        email: "gabrielle.ouellet@example.com",
        display_name: "Gabrielle Ouellet",
        username: "whitebutterfly627",
        photo_url: "https://randomuser.me/api/portraits/women/28.jpg",
        biography: None,
    },
    UserFixture {
        email: "emile.levesque@example.com",
        display_name: "Emile Lévesque",
        username: "organictiger668",
        photo_url: "https://randomuser.me/api/portraits/men/40.jpg",
        biography: None,
    },
    UserFixture {
        email: "sr.kmyrn@example.com",
        display_name: "سارا كامياران",
        username: "redrabbit217",
        photo_url: "https://randomuser.me/api/portraits/women/63.jpg",
        biography: None,
    },
    UserFixture {
        email: "allison.rodriguez@example.com",
        display_name: "Allison Rodriguez",
        username: "heavykoala497",
        photo_url: "https://randomuser.me/api/portraits/women/9.jpg",
        biography: None,
    },
    UserFixture {
        email: "jackie.daniels@example.com",
        display_name: "Jackie Daniels",
        username: "yellowdog958",
        photo_url: "https://randomuser.me/api/portraits/women/4.jpg",
        biography: None,
    },
    UserFixture {
        email: "mohamed.carmona@example.com",
        display_name: "Mohamed Carmona",
        username: "heavymeercat417",
        photo_url: "https://randomuser.me/api/portraits/men/76.jpg",
        biography: None,
    },
    UserFixture {
        email: "orislava.grinchishin@example.com",
        display_name: "Orislava Grinchishin",
        username: "bluekoala231",
        photo_url: "https://randomuser.me/api/portraits/women/62.jpg",
        biography: None,
    },
    UserFixture {
        email: "alexis.brown@example.com",
        display_name: "Alexis Brown",
        username: "redostrich128",
        photo_url: "https://randomuser.me/api/portraits/women/12.jpg",
        biography: None,
    },
    UserFixture {
        email: "domingo.navarro@example.com",
        display_name: "Domingo Navarro",
        username: "sadkoala444",
        photo_url: "https://randomuser.me/api/portraits/men/71.jpg",
        biography: None,
    },
    UserFixture {
        email: "johan.poulsen@example.com",
        display_name: "Johan Poulsen",
        username: "brownrabbit131",
        photo_url: "https://randomuser.me/api/portraits/men/39.jpg",
        biography: None,
    },
    UserFixture {
        email: "ulku.pocan@example.com",
        display_name: "Ülkü Poçan",
        username: "goldensnake973",
        photo_url: "https://randomuser.me/api/portraits/women/32.jpg",
        biography: None,
    },
    UserFixture {
        email: "joel.pereira@example.com",
        display_name: "Joel Pereira",
        username: "smalltiger432",
        photo_url: "https://randomuser.me/api/portraits/men/76.jpg",
        biography: None,
    },
    UserFixture {
        email: "nicolas.ouellet@example.com",
        display_name: "Nicolas Ouellet",
        username: "whitemeercat232",
        photo_url: "https://randomuser.me/api/portraits/men/0.jpg",
        biography: None,
    },
];
