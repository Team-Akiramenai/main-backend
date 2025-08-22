package com.akiramenai.backend.service;

import com.akiramenai.backend.model.JwtErrorTypes;
import com.akiramenai.backend.model.JwtErrorTypes;
import com.akiramenai.backend.model.ResultOrError;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;

@Service
public class JWTService {
  public enum TokenType {
    AccessToken,
    RefreshToken,
  }

  @Value("${application.security.jwt.secret-key}")
  private String secretKey;

  @Value("${application.security.jwt.access-token-validity-duration}")
  private String accessTokenValidityDuration;

  @Value("${application.security.jwt.refresh-token-validity-duration}")
  private String refreshTokenValidityDuration;

  public static Optional<String> extractTokenFromAuthHeader(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return Optional.empty();
    }

    String token = authHeader.substring(7);

    return Optional.of(token);
  }

  public String generateToken(String username, String userId, String userType, TokenType tokenType) {
    long expirationTime = 0L;
    if (tokenType == TokenType.AccessToken) {
      expirationTime = Long.parseLong(accessTokenValidityDuration);
    } else {
      expirationTime = Long.parseLong(refreshTokenValidityDuration);
    }

    return Jwts.builder()
        .claims()
        .subject(username)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expirationTime))
        .add("accountType", userType)
        .add("userId", userId)
        .and()
        .signWith(getKey())
        .compact();
  }

  private SecretKey getKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public ResultOrError<String, JwtErrorTypes> extractUserName(String token) {
    ResultOrError<Claims, JwtErrorTypes> resultOrError = extractClaim(token);
    if (resultOrError.errorMessage() != null) {
      return ResultOrError
          .<String, JwtErrorTypes>builder()
          .result(null)
          .errorMessage(resultOrError.errorMessage())
          .errorType(resultOrError.errorType())
          .build();
    }

    String extractedUsername = usernameResolver(resultOrError.result(), Claims::getSubject);

    return ResultOrError
        .<String, JwtErrorTypes>builder()
        .result(extractedUsername)
        .errorMessage(null)
        .errorType(null)
        .build();
  }

  public ResultOrError<String, JwtErrorTypes> extractUserId(String token) {
    ResultOrError<Claims, JwtErrorTypes> resultOrError = extractClaim(token);
    if (resultOrError.errorMessage() != null) {
      return ResultOrError
          .<String, JwtErrorTypes>builder()
          .result(null)
          .errorMessage(resultOrError.errorMessage())
          .errorType(resultOrError.errorType())
          .build();
    }

    String extractedUserId = usernameResolver(resultOrError.result(), (claims -> claims.get("userId", String.class)));

    return ResultOrError
        .<String, JwtErrorTypes>builder()
        .result(extractedUserId)
        .errorMessage(null)
        .errorType(null)
        .build();
  }

  public ResultOrError<Claims, JwtErrorTypes> extractClaim(String token) {
    return extractAllClaims(token);
  }

  private <T> T usernameResolver(Claims claims, Function<Claims, T> claimResolver) {
    return claimResolver.apply(claims);
  }

  private ResultOrError<Claims, JwtErrorTypes> extractAllClaims(String token) {
    Claims claims = null;
    String errMessage = null;
    JwtErrorTypes errorType = null;

    try {
      claims = Jwts
          .parser()
          .verifyWith(getKey())
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (ExpiredJwtException e) {
      claims = e.getClaims();
      errMessage = "JWT token has expired.";
      errorType = JwtErrorTypes.JwtExpiredException;
    } catch (SignatureException e) {
      errMessage = "Invalid JWT signature. Rejecting token as it might have been tampered with.";
      errorType = JwtErrorTypes.JwtSignatureException;
    } catch (MalformedJwtException e) {
      errMessage = "Invalid JWT token. Token was not properly constructed.";
      errorType = JwtErrorTypes.JwtMalformedException;
    } catch (UnsupportedJwtException e) {
      errMessage = "Unsupported JWT token. Format does not match.";
      errorType = JwtErrorTypes.JwtUnsupportedException;
    } catch (IllegalArgumentException e) {
      errMessage = "JWT token invalid.";
      errorType = JwtErrorTypes.JwtIllegalArgumentException;
    }

    return ResultOrError
        .<Claims, JwtErrorTypes>builder()
        .result(claims)
        .errorMessage(errMessage)
        .errorType(errorType)
        .build();
  }

  public boolean validateToken(String token, UserDetails userDetails) {
    final ResultOrError<String, JwtErrorTypes> usernameOrError = extractUserName(token);

    final String userName = usernameOrError.result();
    return (usernameOrError.errorMessage() == null) && (userName.equals(userDetails.getUsername()));
  }

  public Optional<String> isTokenExpired(String token) {
    final ResultOrError<String, JwtErrorTypes> usernameOrError = extractUserName(token);
    if (usernameOrError.errorMessage() != null) {
      return Optional.of(usernameOrError.errorMessage());
    }

    return Optional.empty();
  }

  public Optional<String> generateTokenUsingRefreshToken(String refreshToken, String username, String userId, String userType) {
    if (isTokenExpired(refreshToken).isPresent()) {
      return Optional.empty();
    }

    return Optional.of(generateToken(username, userId, userType, TokenType.AccessToken));
  }
}
