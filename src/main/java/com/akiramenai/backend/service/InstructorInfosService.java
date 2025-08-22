package com.akiramenai.backend.service;

import com.akiramenai.backend.model.InstructorInfos;
import com.akiramenai.backend.model.Users;
import com.akiramenai.backend.repo.InstructorInfosRepo;
import com.akiramenai.backend.repo.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class InstructorInfosService {
  private final InstructorInfosRepo instructorInfosRepo;
  private final UserRepo userRepo;

  public InstructorInfosService(InstructorInfosRepo instructorInfosRepo, UserRepo userRepo) {
    this.instructorInfosRepo = instructorInfosRepo;
    this.userRepo = userRepo;
  }

  public Optional<String> courseSold(String authorId, double price) {
    Optional<Users> author = userRepo.findById(UUID.fromString(authorId));
    if (author.isEmpty()) {
      return Optional.of("Failed to retrieve author infos.");
    }

    Optional<InstructorInfos> targetInstructorInfos = instructorInfosRepo.getInstructorInfosById(author.get().getInstructorForeignKey());
    if (targetInstructorInfos.isEmpty()) {
      return Optional.of("Failed to retrieve instructor infos for author.");
    }

    InstructorInfos instructorInfos = targetInstructorInfos.get();
    instructorInfos.setTotalCoursesSold(instructorInfos.getTotalCoursesSold() + 1);
    instructorInfos.setAccountBalance(instructorInfos.getAccountBalance() + price);

    try {
      instructorInfosRepo.save(instructorInfos);

      return Optional.empty();
    } catch (Exception e) {
      log.error("Failed to update instructor infos: ", e);

      return Optional.of("Failed to update instructor infos.");
    }
  }
}
