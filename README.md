# PWRTelegram<br><a href="https://github.com/andrew-ld/PWRTelegram/releases"><img src="https://img.shields.io/github/release/andrew-ld/pwrtelegram.svg"/></a> <img src="https://img.shields.io/github/downloads/andrew-ld/pwrtelegram/total"></img>

[Telegram](https://telegram.org) is a messaging app with a focus on speed and security. It’s superfast, simple and free.
This is an unofficial, FOSS-friendly fork of the original [Telegram App for Android](https://github.com/DrKLO/Telegram).

## Current Maintainers

- [thermatk](https://github.com/thermatk)
- [Bubu](https://github.com/Bubu)
- [andrew-ld](https://github.com/andrew-ld)

## Contributors

- [slp](https://github.com/slp)
- [Sudokamikaze](https://github.com/Sudokamikaze)
- [l2dy](https://github.com/l2dy)
- [maximgrafin](https://github.com/maximgrafin)
- [vn971](https://github.com/vn971)
- [theel0ja](https://github.com/theel0ja)
- [AnXh3L0](https://github.com/AnXh3L0)

## Changes:

*Replacement of non-FOSS, untrustworthy or suspicious binaries or source code:*
- Do location sharing with OpenStreetMap(osmdroid) instead of Google Maps
- Use Twemoji emoji set instead of Apple's emoji
- Google Play Services GCM replaced with Telegram's push service
- Has to show a notification on Oreo+, ask Google
- **SECURITY:** Old BoringSSL prebuilts are replaced with the newest upstream source code built at compile time
- **SECURITY:** Old FFmpeg prebuilts are replaced with the newest upstream source code built at compile time
- **SECURITY:** Bundled libWebP is updated
- **SECURITY:** Upstream sqlite3 / libyuv / opus

*Removal of non-FOSS, untrustworthy or suspicious binaries or source code and their functionality:*
- Google Vision face detection and barcode scanning (Passport)
- Google Wallet and Android Pay integration
- HockeyApp crash reporting and self-updates
- Google SMS retrieval. You have to type the code manually

*PWRTelegram patches:*
 - login as bot using importBotAuthorization
 - completely removed all unsupported background calls for bot
 - read groups / channels history using getChannelDifference
 - download all old private messages using getDifference with pts = 0
 - search public chats using resolveUsername

*Other:*
- Allow to set a proxy before login
- Added the ability to parse locations from intents containing a `geo:<lat>,<lon>,<zoom>` string
- Force static map previews from Telegram

## API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

## Building
**NOTE: Building on Windows is, unfortunately, not supported.
Consider using a Linux VM or dual booting.**

1. You need the Android NDK (r19c), Go(Golang) and [Ninja](https://ninja-build.org/) to build the apk.

2. Don't forget to include the submodules when you clone:
      - `git clone --recursive https://github.com/andrew-ld/PWRTelegram.git`

3. Build native dependencies:
      - Go to the `TMessagesProj/jni` folder and execute the following (define the paths to your NDK and Ninja):

      ```
      export NDK=[PATH_TO_NDK]
      export NINJA_PATH=[PATH_TO_NINJA]
      ./build_ffmpeg_clang.sh
      ./patch_boringssl.sh
      ./build_boringssl.sh
      ./build_sqlite3.sh
      ```

4. If you want to publish a modified version of Telegram:
      - You should get **your own API key** here: https://core.telegram.org/api/obtaining_api_id and create a file called `API_KEYS` in the source root directory.
        The contents should look like this:
        ```
        APP_ID = 12345
        APP_HASH = aaaaaaaabbbbbbccccccfffffff001122
        ```
      - Do not use the name Telegram and the standard logo (white paper plane in a blue circle) for your app — or make sure your users understand that it is unofficial
      - Take good care of your users' data and privacy
      - **Please remember to publish your code too in order to comply with the licenses**

The project can be built with Android Studio or from the command line with gradle:

`./gradlew assembleAfatRelease`
