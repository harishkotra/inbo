<!-- Modified for Inbo from Clear SMS. See LICENSE and NOTICE. -->

# Sign an Inbo release

The Android workflow builds a release APK for tags that match `v*`. With all signing secrets configured, it verifies the APK and attaches it to a draft GitHub release. The draft can be reviewed before publishing. Without the signing secrets, the workflow uploads an unsigned CI artifact.

For a first release, generate a private signing key and keep a secure copy. Reuse the original release key for APK updates.

```sh
keytool -genkeypair -v -keystore inbo-release.jks -alias inbo \
  -keyalg RSA -keysize 4096 -validity 10000
```

In GitHub, add these under **Settings → Secrets and variables → Actions**.

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | Base64 encoding of the keystore file |
| `SIGNING_KEYSTORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias, such as `inbo` |
| `SIGNING_KEY_PASSWORD` | Private key password |

You can set them through the GitHub CLI from an authenticated checkout.

```sh
base64 < inbo-release.jks | gh secret set SIGNING_KEYSTORE_BASE64
gh secret set SIGNING_KEYSTORE_PASSWORD
gh secret set SIGNING_KEY_ALIAS --body inbo
gh secret set SIGNING_KEY_PASSWORD
```

For a local signed build, supply `SIGNING_KEYSTORE_FILE` with the keystore path and the same password and alias variable names. Load these values from private storage before invoking Gradle.

```sh
./gradlew assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/Inbo.apk
```

Keep the keystore and signing credentials out of Git. Review the APK and release notes before publishing any draft.
