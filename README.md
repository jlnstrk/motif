# motif

Motif is a mobile app that allows users to share their music track discoveries with friends across streaming provider boundaries.

Users must be enrolled in a premium subscription of either Apple Music or Spotify in order to listen to shared songs.
They can share full songs with an optional initial seek position.
Motif maps tracks between Apple Music and Spotify via the unique ISRC (International Standard Recording Code).

Motif is built using Kotlin Multiplatform and runs on Android and iOS.
For music playback, it binds to the Apple Music and Spotify apps using the respective platform SDKs.
The server-side is written in Rust and communicated to in GraphQL.


## Project structure

```
motif
├── android                    Android app (Kotlin)
├── client                     Shared mobile networking (Kotlin): Apollo GraphQL, REST
├── ios                        iOS app (Swift)
├── player                     Common playback (Kotlin)
│   ├── player-applemusic      Common Apple Music playback (Kotlin): MusicKit (Android), MediaPlayer (iOS)
│   └── player-spotify         Common Spotify playback (Kotlin): Spotify iOS+Android SDKs
├── server                     Backend server (Rust): GraphQL, PostreSQL, Redis
└── shared                     Shared mobile logic (Kotlin): ViewModels, Repositories, DataSources, DI etc.
```

License
=======

    Copyright 2022 Julian Ostarek

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.