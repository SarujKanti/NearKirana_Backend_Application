package com.nearkirana.backend.security;

import com.nearkirana.backend.config.JwtConfig;
import com.nearkirana.backend.config.JwtProperties;
import com.nearkirana.backend.dto.auth.AuthTokens;
import com.nearkirana.backend.entity.RefreshToken;
import com.nearkirana.backend.entity.Role;
import com.nearkirana.backend.entity.User;
import com.nearkirana.backend.exception.ApiException;
import com.nearkirana.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final JwtProperties props = new JwtProperties("nearkirana",
            Duration.ofMinutes(15), Duration.ofDays(30), "test-jwt-secret-that-is-at-least-32-chars");
    private final JwtConfig jwtConfig = new JwtConfig();
    private final SecretKey key = jwtConfig.jwtSecretKey(props);
    private final JwtDecoder decoder = jwtConfig.jwtDecoder(key, props);

    private RefreshTokenRepository repo;
    private TokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        repo = mock(RefreshTokenRepository.class);
        service = new TokenService(jwtConfig.jwtEncoder(key), props, repo, Clock.systemUTC());
        user = new User("+919911111111", Role.CUSTOMER);
        ReflectionTestUtils.setField(user, "id", 4L);
    }

    @Test
    void accessTokenHasUserIdRoleIssuerAndExpiry() {
        AuthTokens tokens = service.issueTokens(user);
        Jwt jwt = decoder.decode(tokens.accessToken()); // also verifies the signature

        assertEquals("4", jwt.getSubject());
        assertEquals("CUSTOMER", jwt.getClaimAsString("role"));
        assertEquals("nearkirana", jwt.getClaimAsString("iss"));
        assertEquals(900, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds());
        assertEquals(null, jwt.getClaimAsString("mobile_number")); // no personal data in the token
        assertEquals("Bearer", tokens.tokenType());
        assertEquals(900, tokens.expiresIn());
    }

    @Test
    void tamperedAccessTokenIsRejected() {
        String token = service.issueTokens(user).accessToken();
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtProperties otherProps = new JwtProperties("nearkirana",
                Duration.ofMinutes(15), Duration.ofDays(30), "a-completely-different-secret-of-32-chars");
        TokenService attacker = new TokenService(
                jwtConfig.jwtEncoder(jwtConfig.jwtSecretKey(otherProps)), otherProps, repo, Clock.systemUTC());
        String forged = attacker.issueTokens(user).accessToken();
        assertThrows(JwtException.class, () -> decoder.decode(forged));
    }

    @Test
    void refreshTokenIsStoredOnlyAsHash() {
        AuthTokens tokens = service.issueTokens(user);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(saved.capture());
        assertNotEquals(tokens.refreshToken(), saved.getValue().getTokenHash());
        assertEquals(TokenService.sha256Hex(tokens.refreshToken()), saved.getValue().getTokenHash());
        assertTrue(tokens.refreshToken().length() >= 43); // 256 bits, base64url
    }

    @Test
    void refreshRotatesTokenInSameFamily() {
        RefreshToken current = stored("old-raw", Instant.now().plus(Duration.ofDays(1)));
        when(repo.findByTokenHashForUpdate(TokenService.sha256Hex("old-raw"))).thenReturn(Optional.of(current));

        AuthTokens next = service.refresh("old-raw");

        assertTrue(current.isRevoked());
        assertNotEquals("old-raw", next.refreshToken());
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(saved.capture());
        assertEquals(current.getFamilyId(), saved.getValue().getFamilyId());
        assertEquals(saved.getValue().getId(), current.getReplacedBy());
    }

    @Test
    void reusingRevokedTokenRevokesWholeFamily() {
        RefreshToken current = stored("stolen", Instant.now().plus(Duration.ofDays(1)));
        current.revoke(Instant.now().minusSeconds(60), UUID.randomUUID()); // already rotated once
        when(repo.findByTokenHashForUpdate(TokenService.sha256Hex("stolen"))).thenReturn(Optional.of(current));

        ApiException ex = assertThrows(ApiException.class, () -> service.refresh("stolen"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(repo).revokeFamily(any(), any());
        verify(repo, never()).save(any());
    }

    @Test
    void expiredRefreshTokenIsRejected() {
        RefreshToken current = stored("old", Instant.now().minusSeconds(1));
        when(repo.findByTokenHashForUpdate(TokenService.sha256Hex("old"))).thenReturn(Optional.of(current));

        assertThrows(ApiException.class, () -> service.refresh("old"));
        verify(repo, never()).save(any());
    }

    @Test
    void unknownRefreshTokenIsRejected() {
        when(repo.findByTokenHashForUpdate(any())).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> service.refresh("made-up"));
    }

    @Test
    void logoutRevokesFamilyAndIgnoresUnknownTokens() {
        RefreshToken current = stored("raw", Instant.now().plus(Duration.ofDays(1)));
        when(repo.findByTokenHashForUpdate(TokenService.sha256Hex("raw"))).thenReturn(Optional.of(current));

        service.logout("raw");
        verify(repo).revokeFamily(eq(current.getFamilyId()), any(Instant.class));

        service.logout("unknown"); // no exception
    }

    private RefreshToken stored(String raw, Instant expiresAt) {
        return new RefreshToken(UUID.randomUUID(), user, UUID.randomUUID(),
                TokenService.sha256Hex(raw), expiresAt, Instant.now().minus(Duration.ofDays(1)));
    }
}
