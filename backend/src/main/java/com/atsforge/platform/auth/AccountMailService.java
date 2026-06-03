package com.atsforge.platform.auth;

import com.atsforge.platform.config.AppProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AccountMailService {
    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public AccountMailService(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendVerification(String email, String token) {
        send(email, "Verify your ATSForge account", "Verify your email: " + frontendLink("/verify-email?token=", token));
    }

    public void sendReset(String email, String token) {
        send(email, "Reset your ATSForge password", "Reset your password: " + frontendLink("/reset-password?token=", token));
    }

    private String frontendLink(String path, String token) {
        return properties.publicUrl() + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void send(String email, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setFrom("no-reply@atsforge.app");
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}

