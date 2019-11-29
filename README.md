# PWRTelegram - a client for bot<br><a href="https://github.com/alissonlauffer/PWRTelegram/releases"><img src="https://img.shields.io/github/release/alissonlauffer/pwrtelegram.svg"/></a> <img src="https://img.shields.io/github/downloads/alissonlauffer/pwrtelegram/total"></img> <img src="https://travis-ci.com/alissonlauffer/PWRTelegram.svg?branch=master-foss"></img>
### <img src="https://en.bitcoin.it/w/images/en/c/cb/BC_Logotype.png" alt="Bitcoin" height="25px" /> `18FFke142Ppvt3xPgQA1MJjkkKznAhTuYy`


## Current Maintainers

- [thermatk](https://github.com/thermatk)
- [andrew-ld](https://github.com/andrew-ld)
- [alissonlauffer](https://github.com/alissonlauffer)

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
      ./patch_ffmpeg.sh
      ./patch_boringssl.sh
      ./build_boringssl.sh
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

