package com.akiramenai.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.UUID;

@Entity
@Table(name = "login_activity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "associated_user_id", insertable = true, updatable = false, nullable = false)
  private UUID associatedUserId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "associated_user_id", insertable = false, updatable = false)
  private Users associatedUser;

  @NotNull
  private int year;

  // We use the following notation in the array:
  // 0 -> Didn't log in that day
  // 1 -> Logged in that day
  // -1 -> To indicate that day is the unavailable leap year day (for non-leap years)
  private ArrayList<Integer> activity;
}
