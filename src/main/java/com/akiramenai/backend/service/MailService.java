package com.akiramenai.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {
  private final JavaMailSender mailSender;

  public MailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void sendMail(String to, String subject, String body) {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

      helper.setFrom("mahi6703890@gmail.com");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body);

      mailSender.send(mimeMessage);

      log.info("Mail sent successfully");
    } catch (MessagingException e) {
      log.error("Failed to send mail");
      System.exit(1);
    }
  }
}
