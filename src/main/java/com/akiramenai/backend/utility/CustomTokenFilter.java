package com.akiramenai.backend.utility;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CustomTokenFilter extends OncePerRequestFilter {
  private final AuthenticationManager authenticationManager;

  public CustomTokenFilter(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);

      CustomAuthToken authToken = new CustomAuthToken(null, null, token);
      try {
        Authentication authenticatedToken = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authenticatedToken);
      } catch (AuthenticationException e) {
        // Handle authentication failure (e.g., send 401 Unauthorized)
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    } else {
      logger.error("Shit is cursed..");
    }

    filterChain.doFilter(request, response);
  }
}
