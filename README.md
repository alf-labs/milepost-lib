# milepost-lib
Milepost Docent, an automated docent for heritage railways


## Gradle Usage

This library is not exported to Maven Central
(I'm not familiar enough with publishing Maven artifacts and CI/Travis testing... contributions welcome).

To use it, get the source via a Git Submodule as well as its dependencies:

```shell
$ git submodule add "https://github.com/alf-labs/milepost-lib.git" MilepostLib
$ git submodule add "https://github.com/ralfoide/libutils.git" LibUtils
$ git submodule add "https://github.com/ralfoide/android-nmea-parser" AndroidNmeaParser
```

`settings.gradle.kts`:
```
include(":MilepostLib")
```

`build.gradle.kts`:
```
dependencies {
    implementation(project(":MilepostLib"))
}
```

If you prefer, you can also build AAR libraries directly:

```shell
$ cd MilepostLib
$ ./_gradlew.sh assemble test
$ ls build/outputs/aar
```

~~
