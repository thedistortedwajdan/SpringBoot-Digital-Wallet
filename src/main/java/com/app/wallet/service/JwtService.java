package com.app.wallet.service;

import com.app.wallet.config.JwtProperties;
import com.app.wallet.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {


    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(
                jwtProperties.getSecret());

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {

        Date now = new Date();

        Date expiry =
                new Date(now.getTime()
                        + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(Long userId) {

        Date now = new Date();

        Date expiry =
                new Date(now.getTime()
                        + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }
}
