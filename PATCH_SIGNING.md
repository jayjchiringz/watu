# Remote patch signing

The app rejects every downloaded DEX/JAR patch unless its bytes verify against
the configured RSA public key. The control server signs the exact patch bytes;
HTTPS is also required for control and artifact URLs.

Generate a dedicated RSA key pair outside both repositories. Keep the private
key only in the server's secret environment/configuration, and never commit it:

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out patch-signing-private.pem
openssl pkey -in patch-signing-private.pem -pubout -outform DER -out patch-public.der
[Convert]::ToBase64String([IO.File]::ReadAllBytes('patch-public.der'))
```

Set the resulting Base64 public key as the Gradle property
`guardianPatchPublicKey` (for example, in the developer's user-level
`%USERPROFILE%\.gradle\gradle.properties`). Set the matching PEM private key
in the server environment as `GUARDIAN_PATCH_SIGNING_PRIVATE_KEY`. The app
fails closed when either side is not configured: patches will not execute or
be advertised for download.

Provision the same key pair in build and server environments before enabling
remote patches. Rotate it by shipping a new app build with the replacement
public key before switching the server to the new private key.
