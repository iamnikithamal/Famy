# Famy Release Keystore

This directory should contain the release keystore for signing the APK.

## Generate Keystore

Run the following command to generate the keystore:

```bash
keytool -genkeypair -v \
  -keystore famy-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias famy \
  -dname "CN=Famy App, OU=Development, O=Famy, L=Unknown, ST=Unknown, C=US" \
  -storepass famy_store_password_123 \
  -keypass famy_key_password_123
```

## Configuration

The keystore is configured in `app/build.gradle.kts`:

- **Keystore File**: `famy-release.jks`
- **Store Password**: `famy_store_password_123`
- **Key Alias**: `famy`
- **Key Password**: `famy_key_password_123`

## GitHub Actions

For CI/CD, the keystore should be base64 encoded and stored as a GitHub secret:

```bash
base64 -i famy-release.jks -o famy-release.jks.base64
```

Then add the following secrets to your GitHub repository:
- `KEYSTORE_BASE64`: The base64 encoded keystore file
- `KEYSTORE_PASSWORD`: `famy_store_password_123`
- `KEY_ALIAS`: `famy`
- `KEY_PASSWORD`: `famy_key_password_123`

## Security Note

For production apps, use strong passwords and keep the keystore file secure.
Do not commit actual production keystores to version control.
