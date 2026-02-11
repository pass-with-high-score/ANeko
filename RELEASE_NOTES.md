# Release v1.5.2 Preparation

## Analysis Summary

### Latest Issues (as of Feb 11, 2026)
1. **Issue #66** (Open): "Reliance on malicious services" - Feature request about Telegram icon
2. **Issue #64** (Open): "Not restarting on phone reboot" - Bug report
3. **Issue #61** (Open): "[Bug] can't switch between languages" - Bug report, assigned to @nqmgaming and @Copilot
4. **Issue #65** (Closed): "Not restarting after SeedVault backup" - Related to #64

### Commits After v1.5.1
After the v1.5.1 release (Jan 23, 2026), there is **only 1 commit**:

- **01ded3f** (Jan 27, 2026): "fix: enhance arabic language"
  - Minor fix to improve Arabic language strings
  - Changed 3 additions and 14 deletions in `app/src/main/res/values-ar/strings.xml`

### Decision
Since there's only one small enhancement commit after v1.5.1 and the open issues are not critical blockers for a store release, it's appropriate to prepare a new patch version (v1.5.2) for the store.

## Changes Made

### Version Update
- Updated `versionCode` from `21` to `22`
- Updated `versionName` from `"1.5.1"` to `"1.5.2"`
- File: `gradle/libs.versions.toml`

### Changelog
- Created changelog for version 22 (v1.5.2)
- File: `fastlane/metadata/android/en-US/changelogs/22.txt`
- Contents:
  - Enhanced Arabic language support
  - Minor bug fixes and improvements

## Release Instructions

### Automated Release (Recommended)
The repository has automated CI/CD set up. To trigger a release:

1. **Create and push a tag**:
   ```bash
   git tag v1.5.2
   git push origin v1.5.2
   ```

2. **GitHub Actions will automatically**:
   - Build the signed APK and AAB
   - Create a GitHub Release with the APK attached
   - Deploy to Google Play Store
   - Use the changelog from `fastlane/metadata/android/en-US/changelogs/22.txt`

3. **Monitor the workflow**:
   - Go to: https://github.com/pass-with-high-score/ANeko/actions
   - Watch the "Publish Releases" workflow
   - Ensure both jobs complete successfully:
     - "Build and publish GitHub release"
     - "Build and publish to Play Store"

### Manual Release (If needed)
If automated release fails or you need to build locally:

1. **Build the APK**:
   ```bash
   ./gradlew clean assembleRelease
   ```

2. **Test the build**:
   - Install on a test device
   - Verify Arabic language strings display correctly
   - Test basic app functionality

3. **Create GitHub Release**:
   - Tag: `v1.5.2`
   - Title: `Release v1.5.2`
   - Body:
     ```
     - Enhanced Arabic language support
     - Minor bug fixes and improvements
     ```
   - Upload the APK from: `app/build/outputs/apk/release/app-release.apk`

4. **Push to Google Play Store** (Manual):
   - Option A: Use Fastlane:
     ```bash
     fastlane android release_all
     ```
   - Option B: Manual upload through Play Console
   - The changelog will be automatically picked up from `fastlane/metadata/android/en-US/changelogs/22.txt`

5. **Optional - Update F-Droid**:
   - F-Droid should automatically pick up the new version from the repository

## Notes

- The open issues (#66, #64, #61) don't block this release:
  - #66 is a feature request/discussion about platform choices
  - #64 and #61 are bugs that should be addressed in a future release
- The commit after v1.5.1 is a minor enhancement suitable for a patch release
- This release maintains the same feature set as v1.5.1 with improved localization
