package com.akiramenai.backend.filters;

import com.akiramenai.backend.model.Users;
import com.akiramenai.backend.repo.UserRepo;
import com.akiramenai.backend.service.LoginActivityService;
import com.akiramenai.backend.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class ActivityFilter extends OncePerRequestFilter {
  private final UserService userService;
  private final UserRepo userRepo;
  private final LoginActivityService loginActivityService;

  public ActivityFilter(UserService userService, UserRepo userRepo, LoginActivityService loginActivityService) {
    this.userService = userService;
    this.userRepo = userRepo;
    this.loginActivityService = loginActivityService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {
    var userIdStr = request.getAttribute("userId");
    if (userIdStr == null) {
      filterChain.doFilter(request, response);
      return;
    }

    UUID userID = UUID.fromString(userIdStr.toString());
    Optional<Users> targetUser = userService.findUserById(userID);
    if (targetUser.isEmpty()) {
      log.warn("Invalid user ID provided. User with ID `{}` not found.", userID);
      filterChain.doFilter(request, response);
      return;
    }

    LocalDate lastUserLoginDate = targetUser.get().getLastLoginDate();
    LocalDate dateToday = LocalDate.now();

    loginActivityService.addLoginActivity(userID, dateToday);

    if (lastUserLoginDate.equals(dateToday)) {
      filterChain.doFilter(request, response);
      return;
    }

    long dayDiff = ChronoUnit.DAYS.between(lastUserLoginDate, dateToday);
    if (dayDiff == 1) {
      targetUser.get().setLoginStreak(targetUser.get().getLoginStreak() + 1);
    }
    if (dayDiff > 1) {
      targetUser.get().setLoginStreak(1);
    }
    targetUser.get().setLastLoginDate(dateToday);
    userRepo.save(targetUser.get());

    filterChain.doFilter(request, response);
  }
}
