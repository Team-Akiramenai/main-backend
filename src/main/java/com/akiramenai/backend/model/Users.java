package com.akiramenai.backend.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
public class Users {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Nullable
  private UUID learnerForeignKey;

  @Nullable
  private UUID instructorForeignKey;

  @NotBlank(message = "Username cannot be blank or whitespace only")
  @Size(min = 1, max = 100, message = "Username must be between 5 and 100 characters")
  @Column(nullable = false, unique = true)
  private String username;

  @NotBlank(message = "E-mail cannot be blank or whitespace only")
  @Size(min = 5, max = 100, message = "E-mail must be between 5 and 100 characters")
  @Column(nullable = false, unique = true)
  private String email;

  @NotBlank(message = "Password cannot be blank or whitespace only")
  @Size(min = 8, max = 256, message = "Username must be between 5 and 100 characters")
  @Column(nullable = false)
  private String password;

  private String pfpFileName;

  @NotNull(message = "Account type cannot be null")
  @Enumerated(EnumType.STRING)
  private UserType userType;

  @NotNull
  private long totalStorageInBytes;

  @NotNull
  private long usedStorageInBytes;

  public void setUserType(String accType) {
    if (accType.equalsIgnoreCase("Learner")) {
      this.userType = UserType.Learner;
    } else {
      this.userType = UserType.Instructor;
    }
  }

  public String getUserType() {
    if (this.userType.equals(UserType.Learner)) {
      return "Learner";
    }

    return "Instructor";
  }

}
