fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android internal

```sh
[bundle exec] fastlane android internal
```

Submit a new Build to Google Play Internal

### android alpha

```sh
[bundle exec] fastlane android alpha
```

Submit a new Alpha Build to Google Play Close

### android beta

```sh
[bundle exec] fastlane android beta
```

Submit a new Beta Build to Google Play Open

### android release

```sh
[bundle exec] fastlane android release
```

Deploy a new version to the Google Play

### android notify_telegram

```sh
[bundle exec] fastlane android notify_telegram
```



----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
