# ANeko Automation Guide

This document explains how to control ANeko using external intents from automation apps.

## Supported Intents

ANeko supports the following broadcast intents for automation:

- **Start ANeko**: `org.nqmgaming.aneko.intent.action.START`
- **Stop ANeko**: `org.nqmgaming.aneko.intent.action.STOP`

## Prerequisites

Before using automation intents, ensure that:
1. ANeko is installed on your device
2. ANeko has been granted overlay permission
3. Your automation app has permission to send broadcasts

## Usage Examples

### Tasker

**To start ANeko:**
1. Create a new Task
2. Add Action → System → Send Intent
3. Configure the intent:
   - Action: `org.nqmgaming.aneko.intent.action.START`
   - Target: `Broadcast Receiver`
4. Save the task

**To stop ANeko:**
1. Create a new Task
2. Add Action → System → Send Intent
3. Configure the intent:
   - Action: `org.nqmgaming.aneko.intent.action.STOP`
   - Target: `Broadcast Receiver`
4. Save the task

### MacroDroid

**To start ANeko:**
1. Create a new Macro
2. Add Action → Connectivity → Send Intent
3. Configure the intent:
   - Action: `org.nqmgaming.aneko.intent.action.START`
   - Target: `Broadcast`
4. Save the macro

**To stop ANeko:**
1. Create a new Macro
2. Add Action → Connectivity → Send Intent
3. Configure the intent:
   - Action: `org.nqmgaming.aneko.intent.action.STOP`
   - Target: `Broadcast`
4. Save the macro

### ADB (Android Debug Bridge)

For testing or development purposes, you can use ADB commands:

**Start ANeko:**
```bash
adb shell am broadcast -a org.nqmgaming.aneko.intent.action.START
```

**Stop ANeko:**
```bash
adb shell am broadcast -a org.nqmgaming.aneko.intent.action.STOP
```

### Automation Ideas

Here are some creative ways to use ANeko automation:

1. **Work Hours**: Start ANeko when you arrive at work, stop when you leave
2. **Battery Level**: Stop ANeko when battery is low, start when charging
3. **Time-based**: Start ANeko during specific hours of the day
4. **App-based**: Start ANeko when opening certain apps, stop with others
5. **Location-based**: Start ANeko at home, stop when away

## Troubleshooting

### ANeko doesn't start

- Verify that overlay permission is granted in Android Settings
- Check that ANeko is installed and updated to a version that supports intents
- Ensure your automation app has permission to send broadcasts

### ANeko doesn't respond to stop command

- Try force-stopping ANeko from Android Settings
- Restart your device if the issue persists

### Automation app can't send the intent

- Make sure you've entered the exact action string (case-sensitive)
- Verify that the target is set to "Broadcast Receiver" or "Broadcast"
- Check your automation app's logs for error messages

## Security Note

The broadcast receiver is intentionally exported without a custom permission to ensure compatibility with automation apps. This design decision allows Tasker and similar apps to control ANeko without requiring additional setup. Since ANeko only controls a visual overlay with no access to sensitive data, the security risk is minimal.

## Feedback

If you encounter any issues or have suggestions for improving the automation feature, please open an issue on our GitHub repository:
https://github.com/pass-with-high-score/ANeko/issues
