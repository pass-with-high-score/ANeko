# GitHub Actions Workflows for App Publishing

This directory contains GitHub Actions workflow configurations for automating the build and deployment process of the
Secure Camera app to both Google Play and GitHub Releases.

## Workflow: Publish Releases

The `publish-release.yml` workflow automatically builds and publishes the app when a new tag with
the format `v*` (e.g., `v1.0.0`) is pushed to the repository. It contains two jobs:

1. Build and publish to Play Store
2. Build and publish GitHub release

### Play Store Job Steps

1. Checkout the code
2. Set up JDK 17
3. Set up Ruby and install Fastlane
4. Decode the Android keystore from a base64-encoded secret
5. Build the release AAB with proper signing
6. Decode the Google Play service account key
7. Deploy to Google Play using Fastlane

### GitHub Release Job Steps

1. Checkout the code
2. Set up JDK 17
3. Decode a separate Android keystore from a base64-encoded secret
4. Build a signed release APK
5. Create a GitHub release
6. Attach the APK to the release

### Required Secrets

#### For Play Store Publishing

The following secrets must be configured in your GitHub repository settings:

1. **ENCODED_KEYSTORE**: Base64-encoded Android keystore file
   ```bash
   # Generate using:
   base64 -w 0 keystore.jks > keystore_base64.txt
   ```

2. **KEYSTORE_PASSWORD**: Password for the keystore

3. **KEY_ALIAS**: Alias of the key in the keystore

4. **KEY_PASSWORD**: Password for the key

5. **PLAY_STORE_CONFIG_JSON**: Google Play service account JSON key file content
    - This is used by Fastlane to authenticate with Google Play
    - You need to create a service account in the Google Play Console with the appropriate permissions

#### For GitHub Release Publishing

The following secrets must be configured in your GitHub repository settings:

1. **GITHUB_RELEASE_ENCODED_KEYSTORE**: Base64-encoded Android keystore file (separate from Play Store keystore)
   ```bash
   # Generate using:
   base64 -w 0 github_release_keystore.jks > github_release_keystore_base64.txt
   ```

2. **GITHUB_RELEASE_KEYSTORE_PASSWORD**: Password for the GitHub release keystore

3. **GITHUB_RELEASE_KEY_ALIAS**: Alias of the key in the GitHub release keystore

4. **GITHUB_RELEASE_KEY_PASSWORD**: Password for the key in the GitHub release keystore

### How to Use

1. Set up all the required secrets in your GitHub repository settings:
    - For Play Store publishing: ENCODED_KEYSTORE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD, PLAY_STORE_CONFIG_JSON
    - For GitHub release publishing: GITHUB_RELEASE_ENCODED_KEYSTORE, GITHUB_RELEASE_KEYSTORE_PASSWORD,
      GITHUB_RELEASE_KEY_ALIAS, GITHUB_RELEASE_KEY_PASSWORD
2. When you're ready to release a new version:
    - Update the version information in `gradle/libs.versions.toml`
    - Commit and push the changes
    - Create and push a new tag with the format `v1.0.0` (matching your version)
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
3. The workflow will automatically trigger and:
    - Deploy the app to Google Play
    - Create a GitHub release with the signed APK attached

### Troubleshooting

If the workflow fails, check the following:

#### For Play Store Publishing

1. Ensure all Play Store secrets are correctly configured
2. Verify that the keystore is valid and contains the correct key
3. Make sure the Google Play service account has the necessary permissions
4. Check that the app's version code has been incremented since the last release

#### For GitHub Release Publishing

1. Ensure all GitHub release secrets are correctly configured
2. Verify that the GitHub release keystore is valid and contains the correct key
3. Check that you have the necessary permissions to create releases in the repository
4. Verify that the APK is being built correctly