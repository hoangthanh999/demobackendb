package com.badminton.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        log.debug("🔑 JWT Secret length: {} bytes", keyBytes.length);
        log.debug("🔑 JWT Secret (first 10 chars): {}...",
                jwtSecret.substring(0, Math.min(10, jwtSecret.length())));

        // ✅ QUAN TRỌNG: Secret phải >= 512 bits (64 bytes) cho HS512
        if (keyBytes.length < 64) {
            log.warn("⚠️ JWT Secret too short! Current: {} bytes, Required: >= 64 bytes",
                    keyBytes.length);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateTokenFromEmail(userPrincipal.getUsername());
    }

    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            log.info("🔍 Validating token: {}...", authToken.substring(0, Math.min(30, authToken.length())));

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(authToken);

            log.info("✅ Token validation SUCCESS");
            return true;
        } catch (SecurityException ex) {
            log.error("❌ Invalid JWT signature", ex);
        } catch (MalformedJwtException ex) {
            log.error("❌ Invalid JWT token", ex);
        } catch (ExpiredJwtException ex) {
            log.error("❌ Expired JWT token - exp: {}, now: {}",
                    ex.getClaims().getExpiration(), new Date());
        } catch (UnsupportedJwtException ex) {
            log.error("❌ Unsupported JWT token", ex);
        } catch (IllegalArgumentException ex) {
            log.error("❌ JWT claims string is empty", ex);
        }
        return false;
    }

}
