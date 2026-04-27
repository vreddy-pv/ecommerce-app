package com.ecommerce.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class GatewayJwtConfig {

    @Value("${jwt.public-key}")
    private String base64PublicKey;

    @Bean
    public PublicKey jwtPublicKey() throws Exception {
        byte[] decoded = Base64.getDecoder().decode(
            base64PublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "")
        );
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    @Bean
    public GatewayJwtService gatewayJwtService(PublicKey jwtPublicKey) {
        return new GatewayJwtService(jwtPublicKey);
    }
}
