package com.ncortez.TPV_TotalGlobal.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio JWT para crear y validar tokens de autenticación.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String username, String role) {
        // Guardamos el rol como claim para poder autorizar por perfil en servidor.
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return buildToken(claims, username);
    }

    public boolean isTokenValid(String token, String username) {
        // Token válido = pertenece al mismo usuario y no está expirado.
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    private String buildToken(Map<String, Object> extraClaims, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpirationMs);

        // Subject = username canónico. Expiración configurable por properties.
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        // Si firma o formato no son correctos, JJWT lanza excepción.
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(toBase64(jwtSecret));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Normaliza la clave para permitir configuración en texto plano y convertirla a Base64.
     */
    private String toBase64(String rawSecret) {
        if (rawSecret == null) {
            return "";
        }
        return java.util.Base64.getEncoder().encodeToString(rawSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
