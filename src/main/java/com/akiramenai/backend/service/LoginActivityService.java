package com.akiramenai.backend.service;

import com.akiramenai.backend.model.LoginActivity;
import com.akiramenai.backend.repo.LoginActivityRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
public class LoginActivityService {
  private final LoginActivityRepo loginActivityRepo;

  public LoginActivityService(LoginActivityRepo loginActivityRepo) {
    this.loginActivityRepo = loginActivityRepo;
  }

  public void addLoginActivity(UUID userId, LocalDate date) {
    Optional<LoginActivity> loginActivity = loginActivityRepo.findLoginActivitiesByAssociatedUserIdAndYear(
        userId,
        date.getYear()
    );
    if (loginActivity.isEmpty()) {
      // No login activity in this year, so create one
      ArrayList<Integer> initialActivityArray = new ArrayList<>(Collections.nCopies(367, 0));
      if (!date.isLeapYear()) {
        initialActivityArray.set(60, -1);
      }

      LoginActivity newLoginActivity = LoginActivity
          .builder()
          .associatedUserId(userId)
          .year(date.getYear())
          .activity(initialActivityArray)
          .build();

      loginActivity = Optional.of(newLoginActivity);
    }

    loginActivity.get().getActivity().set(date.getDayOfYear(), 1);
    loginActivityRepo.save(loginActivity.get());
  }

  public Optional<List<Integer>> getLoginActivityForMonth(UUID userId, LocalDate requestedMonth) {
    Optional<LoginActivity> loginActivity = loginActivityRepo.findLoginActivitiesByAssociatedUserIdAndYear(
        userId,
        requestedMonth.getYear()
    );
    if (loginActivity.isEmpty()) {
      return Optional.empty();
    }


    LocalDate requestedMonthBeginning = requestedMonth.withDayOfMonth(1);
    YearMonth reqMonth = YearMonth.from(requestedMonthBeginning);
    int startIdx = requestedMonthBeginning.getDayOfYear();
    int endIdx = requestedMonthBeginning.plusDays(reqMonth.lengthOfMonth()).getDayOfYear();
    if (!requestedMonth.isLeapYear()) {
      if (requestedMonth.getMonthValue() == 2) {
        endIdx += 1;
      }
      if (requestedMonth.getMonthValue() > 2) {
        startIdx += 1;
      }
    }
    List<Integer> monthActivity = loginActivity.get().getActivity().subList(startIdx, endIdx);

    return Optional.of(monthActivity);
  }

  public Optional<List<Integer>> getLoginActivityForYear(UUID userId, LocalDate requestedYear) {
    Optional<LoginActivity> loginActivity = loginActivityRepo.findLoginActivitiesByAssociatedUserIdAndYear(
        userId,
        requestedYear.getYear()
    );
    if (loginActivity.isEmpty()) {
      return Optional.empty();
    }

    ArrayList<Integer> activityInYear = loginActivity.get().getActivity();
    return Optional.of(activityInYear.subList(1, activityInYear.size()));
  }
}
