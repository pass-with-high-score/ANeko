<div align="center">      
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="128" height="128">
  <h1>ANeko Reborn</h1>
  <p><strong>ANeko Reborn</strong> is a modern version of the classic <strong>ANeko</strong> app.</p>
<p>It features a cute cat animation that follows your finger on the Android screen, inspired by apps like <strong>nekoDA, xneko, oneko</strong>, and more.</p>
<p>This version is built for modern Android devices with better performance and compatibility.</p>
  <br><br>
  <a href="https://github.com/pass-with-high-score/ANeko/releases">
    <img src="https://img.shields.io/github/v/release/pass-with-high-score/ANeko">
  </a>
  <a href="https://github.com/pass-with-high-score/ANeko/releases">
    <img src="https://img.shields.io/github/downloads/pass-with-high-score/ANeko/total">
  </a>
  <br><br>
  <h4>Download</h4>
  <a href="https://play.google.com/store/apps/details?id=org.nqmgaming.aneko">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="80">
  </a>
  <a href="https://github.com/pass-with-high-score/ANeko/releases">
    <img src="https://raw.githubusercontent.com/NeoApplications/Neo-Backup/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" height="80">
  </a>
  <a href="https://www.openapk.net/vi/aneko/org.nqmgaming.aneko/">
    <img src="https://www.openapk.net/images/openapk-badge.png" height="80">
  </a>
  <a href="https://f-droid.org/en/packages/org.nqmgaming.aneko/">
    <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="80">
  </a>
  </a>
</div> 

---  

## Features

* The cat follows your finger around the screen
* Updated user interface with Material You styling
* Compatible with Android 14 and newer
* Handles overlay permissions automatically
* Smoother animations and improved performance
* Supports custom cat skins
* Intent support for automation apps (Tasker, MacroDroid, etc.)

---

## Automation with Intents

ANeko can be controlled by external apps like **Tasker**, **MacroDroid**, or any automation tool that supports sending Android intents.

### Starting ANeko

Send a broadcast intent with the following action:
```
Action: org.nqmgaming.aneko.intent.action.START
```

**Example using ADB:**
```bash
adb shell am broadcast -a org.nqmgaming.aneko.intent.action.START
```

**Example in Tasker:**
1. Create a new task
2. Add action: System → Send Intent
3. Set Action: `org.nqmgaming.aneko.intent.action.START`
4. Set Target: `Broadcast Receiver`

### Stopping ANeko

Send a broadcast intent with the following action:
```
Action: org.nqmgaming.aneko.intent.action.STOP
```

**Example using ADB:**
```bash
adb shell am broadcast -a org.nqmgaming.aneko.intent.action.STOP
```

**Example in Tasker:**
1. Create a new task
2. Add action: System → Send Intent
3. Set Action: `org.nqmgaming.aneko.intent.action.STOP`
4. Set Target: `Broadcast Receiver`

**Note:** ANeko must have overlay permission granted for the start intent to work.

---  

## Community

Join our community on Telegram:  
[![Telegram](https://img.shields.io/badge/Telegram-Join%20Group-blue?logo=telegram)](https://t.me/aneko_community)

## Build Instructions

### Requirements

* [Android Studio](https://developer.android.com/studio)
* Java 11 or higher

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/pass-with-high-score/ANeko.git 
   cd ANeko 
   ```
2. Open the project in Android Studio

3. Sync Gradle and run the app on a device or emulator

---  

## Creating Custom Skins

* Go to [ANeko Builder](https://devtool.pwhs.app/aneko-builder)
* Upload your assets
* Then create your custom skin
---

## License

This project is licensed under the **GNU Lesser General Public License v2.1**.  
You are free to use, modify, and distribute it, but you must include proper credits and the
license.  
See the full [LICENSE](LICENSE) file for details.
  
---  

## Credits

* Original ANeko by [Tamanegi](https://github.com/lllllT)
* Fork maintained by [choiman1559](https://github.com/choiman1559/ANeko)
* Updated and improved by [Nguyen Quang Minh](https://github.com/nqmgaming)

---  

## Contributing

Pull requests and issue reports are welcome.
Help us improve ANeko Reborn!

### Help us translate ANeko

Want to see ANeko in your language?
Join our translation project here: [https://poeditor.com/join/project/zX7Nu44XDh](https://poeditor.com/join/project/zX7Nu44XDh)

---

## Star History
Thanks


[![Star History Chart](https://api.star-history.com/svg?repos=pass-with-high-score/ANeko&type=Date)](https://www.star-history.com/#pass-with-high-score/ANeko&Date)
