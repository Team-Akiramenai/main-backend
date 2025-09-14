package com.akiramenai.backend.model;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class Users {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Nullable
  private UUID learnerForeignKey;

  @Nullable
  private UUID instructorForeignKey;

  @NotBlank(message = "Username cannot be blank or whitespace only")
  @Size(min = 1, max = 100)
  @Column(nullable = false, unique = true)
  private String username;

  @NotBlank(message = "E-mail cannot be blank or whitespace only")
  @Size(min = 5, max = 100)
  @Column(nullable = false, unique = true)
  private String email;

  @NotBlank(message = "Password cannot be blank or whitespace only")
  @Size(min = 8, max = 256)
  @Column(nullable = false)
  private String password;

  private String pfpFileName;

  @NotNull(message = "UserType needs to be provided")
  @Enumerated(EnumType.STRING)
  private UserType userType;

  @NotNull
  private long totalStorageInBytes;

  @NotNull
  private long usedStorageInBytes;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDate lastLoginDate;

  @NotNull
  @ColumnDefault("0")
  private int loginStreak;

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
