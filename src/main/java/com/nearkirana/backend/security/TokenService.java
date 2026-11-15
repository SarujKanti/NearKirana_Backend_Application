package com.nearkirana.backend.security;

import com.nearkirana.backend.config.JwtProperties;
import com.nearkirana.backend.dto.auth.AuthTokens;
import com.nearkirana.backend.entity.RefreshToken;
import com.nearkirana.backend.entity.User;
import com.nearkirana.backend.exception.ApiException;
import com.nearkirana.backend.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues, rotates and revokes tokens.
 *
 * Access token  = signed JWT (stateless: the server does not store it).
 * Refresh token = 256-bit random string; only its SHA-256 hash is stored.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String TOKEN_TYPE = "Bearer";
    public static final String ROLE_CLAIM = "role";
    private static final int REFRESH_TOKEN_BYTES = 32; // 256 bits

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(JwtEncoder jwtEncoder,
                        JwtProperties jwtProperties,
                        RefreshTokenRepository refreshTokenRepository,
                        Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    /** After a successful OTP login: a brand-new session (new family). */
    @Transactional
    public AuthTokens issueTokens(User user) {
        return issue(user, UUID.randomUUID(), clock.instant()).tokens();
    }

    /**
     * POST /auth/refresh-token. The used refresh token is revoked and replaced
     * ("rotation"). If a token that was ALREADY rotated is used again, someone
     * copied it, so the whole session is revoked.
     *
     * noRollbackFor: the theft-revocation must be saved even though we return 401.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthTokens refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(sha256Hex(rawRefreshToken))
                .orElseThrow(TokenService::invalidRefreshToken);

        if (current.isRevoked()) {
            int revoked = refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            log.warn("Reuse of revoked refresh token {} (user {}): revoked {} token(s) of family {}",
                    current.getId(), current.getUser().getId(), revoked, current.getFamilyId());
            throw invalidRefreshToken();
        }
        if (current.isExpired(now)) {
            throw invalidRefreshToken();
        }
        User user = current.getUser();
        if (!user.isActive()) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new ApiException(HttpStatus.FORBIDDEN, "This account has been disabled. Please contact support.");
        }

        Issued next = issue(user, current.getFamilyId(), now);
        current.revoke(now, next.refreshTokenId());
        return next.tokens();
    }

    /** POST /auth/logout: ends this device's session. Unknown tokens are ignored (no information leak). */
    @Transactional
    public void logout(String rawRefreshToken) {
        Instant now = clock.instant();
        Optional<RefreshToken> token = refreshTokenRepository.findByTokenHashForUpdate(sha256Hex(rawRefreshToken));
        token.ifPresent(t -> {
            refreshTokenRepository.revokeFamily(t.getFamilyId(), now);
            log.info("User {} logged out (family {})", t.getUser().getId(), t.getFamilyId());
        });
    }

    // ------------------------------------------------------------------

    private record Issued(AuthTokens tokens, UUID refreshTokenId) {
    }

    private Issued issue(User user, UUID familyId, Instant now) {
        String accessToken = createAccessToken(user, now);

        String rawRefreshToken = newRandomToken();
        UUID refreshTokenId = UUID.randomUUID();
        refreshTokenRepository.save(new RefreshToken(
                refreshTokenId, user, familyId, sha256Hex(rawRefreshToken),
                now.plus(jwtProperties.refreshTokenTtl()), now));

        AuthTokens tokens = new AuthTokens(accessToken, rawRefreshToken, TOKEN_TYPE,
                jwtProperties.accessTokenTtl().toSeconds());
        return new Issued(tokens, refreshTokenId);
    }

    /** Signed JWT with user id (sub), role and expiry. No phone number: tokens can be decoded by anyone. */
    String createAccessToken(User user, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(String.valueOf(user.getId()))
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String newRandomToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Plain SHA-256 is fine here (unlike OTPs): a 256-bit random token cannot be guessed,
     * so the hash only protects us if the database is ever leaked.
     */
    static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token. Please log in again.");
    }
}
