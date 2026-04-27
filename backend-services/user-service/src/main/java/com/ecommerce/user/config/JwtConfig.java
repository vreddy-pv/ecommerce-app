package com.ecommerce.user.config;

import com.ecommerce.user.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    // Both keys are base64-encoded PEM (without headers/footers), injected via env var.
    // Only user-service holds the private key. All other services get only the public key.
    @Value("${jwt.private-key}")
    private String base64PrivateKey;

    @Value("${jwt.public-key}")
    private String base64PublicKey;

    @Value("${jwt.access-token-expiry-ms:900000}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

    @Bean
    public JwtService jwtService() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");

        byte[] privateBytes = Base64.getDecoder().decode(stripPemHeaders(base64PrivateKey));
        PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));

        byte[] publicBytes = Base64.getDecoder().decode(stripPemHeaders(base64PublicKey));
        PublicKey publicKey = kf.generatePublic(new X509EncodedKeySpec(publicBytes));

        return new JwtService(privateKey, publicKey, accessTokenExpiryMs, refreshTokenExpiryMs);
    }

    private String stripPemHeaders(String pem) {
        return pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
    }
}
