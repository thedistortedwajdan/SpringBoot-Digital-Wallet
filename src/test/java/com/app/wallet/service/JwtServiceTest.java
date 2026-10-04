package com.app.wallet.service;

import com.app.wallet.config.JwtProperties;
import com.app.wallet.exception.InvalidTokenException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=";

    private JwtProperties properties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpiration(60_000);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatedToken_roundTripsUserId() {
        String token = jwtService.generateToken(42L);

        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(42L);
    }

    @Test
    void expiredToken_isRejected() {
        properties.setExpiration(-1000);
        String token = jwtService.generateToken(42L);

        assertThatThrownBy(() -> jwtService.getUserIdFromToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Token has expired");
    }

    @Test
    void malformedAndBlankTokens_areRejected() {
        assertThatThrownBy(() -> jwtService.getUserIdFromToken("abc"))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.getUserIdFromToken(""))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String otherSecret = java.util.Base64.getEncoder()
                .encodeToString("another-secret-another-secret-another-secret!!".getBytes());
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret(otherSecret);
        otherProps.setExpiration(60_000);

        String token = new JwtService(otherProps).generateToken(1L);

        assertThatThrownBy(() -> jwtService.getUserIdFromToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void nonNumericOrMissingSubject_isRejected() {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Date expiry = new Date(System.currentTimeMillis() + 60_000);

        String nonNumeric = Jwts.builder().subject("abc").expiration(expiry).signWith(key).compact();
        String noSubject = Jwts.builder().expiration(expiry).signWith(key).compact();

        assertThatThrownBy(() -> jwtService.getUserIdFromToken(nonNumeric))
                .isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.getUserIdFromToken(noSubject))
                .isInstanceOf(InvalidTokenException.class);
    }
}
