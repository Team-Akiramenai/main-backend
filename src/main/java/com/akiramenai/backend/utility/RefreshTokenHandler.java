package com.akiramenai.backend.utility;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class RefreshTokenHandler {
  public static Optional<String> getFromCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }

    Optional<Cookie> refreshCookie = Arrays.stream(cookies).filter(cookie -> cookie.getName().equals("REFRESH_TOKEN")).findFirst();
    if (refreshCookie.isEmpty() || refreshCookie.get().getValue() == null || refreshCookie.get().getValue().isBlank()) {
      return Optional.empty();
    }

    return Optional.of(refreshCookie.get().getValue());
  }


  public static Cookie cookieBuilder(
      String cookieValue,
      int validityDuration,
      boolean httpOnly
  ) {
    Cookie refreshTokenCookie = new Cookie("REFRESH_TOKEN", cookieValue);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setMaxAge(validityDuration);
    refreshTokenCookie.setPath("/");
    if (httpOnly) {
      refreshTokenCookie.setSecure(true);
      log.warn("Refresh token cookie will ONLY be sent over HTTPS. This can changed in the `application.yaml` file.");
    } else {
      refreshTokenCookie.setSecure(false);
      log.warn("Refresh token cookie will be sent over both HTTP and HTTPS. This can changed in the `application.yaml` file.");
    }

    return refreshTokenCookie;
  }

  public static void removeCookie(
      HttpServletResponse response,
      boolean httpOnly
  ) {
    Cookie cookieToRemove = cookieBuilder(null, 0, httpOnly);
    response.addCookie(cookieToRemove);
  }
}
