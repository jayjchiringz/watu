package com.system.guardian;

import android.content.Context;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

public final class PatchSignatureVerifier {

    private PatchSignatureVerifier() {
    }

    public static boolean verify(Context context, File artifact, String encodedSignature)
            throws IOException, GeneralSecurityException {
        String encodedPublicKey = BuildConfig.GUARDIAN_PATCH_PUBLIC_KEY;
        if (encodedPublicKey == null || encodedPublicKey.trim().isEmpty()) {
            CrashLogger.log(context, "PatchSignature", "❌ Patch rejected: verification public key is not configured.");
            return false;
        }

        byte[] publicKeyBytes = Base64.decode(encodedPublicKey, Base64.DEFAULT);
        byte[] signatureBytes = Base64.decode(encodedSignature, Base64.DEFAULT);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(KeyFactory.getInstance("RSA").generatePublic(keySpec));

        try (FileInputStream input = new FileInputStream(artifact)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                verifier.update(buffer, 0, bytesRead);
            }
        }

        boolean verified = verifier.verify(signatureBytes);
        if (!verified) {
            CrashLogger.log(context, "PatchSignature", "❌ Patch rejected: publisher signature is invalid.");
        }
        return verified;
    }
}
