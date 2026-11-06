package com.donation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@donation.com}")
    private String fromEmail;

    @Value("${app.password-reset.token-expiry-minutes}")
    private int tokenExpiryMinutes;

    public void sendPasswordResetEmail(String toEmail, String userName, String rawToken) {
        String resetLink = baseUrl + "/reset-password?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Reset your Goodwell password");
        message.setText(
            "Hello " + userName + ",\n\n" +
            "Use the link below to reset your password. It expires in " + tokenExpiryMinutes + " minutes:\n" +
            resetLink + "\n\n" +
            "If you did not request this, you can ignore this email."
        );
        mailSender.send(message);
    }
}
