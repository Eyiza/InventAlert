package com.inventalert.notificationService.service.impl;

import com.inventalert.notificationService.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${notification.mail.from:noreply@inventalert.com}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendNotificationEmail(String to, String subject, String body) {
        log.info("Attempting to send email to={}, subject='{}', from={}", to, subject, fromAddress);
        MailException lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                log.debug("Email attempt {}/3 to {}", attempt, to);
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(fromAddress);
                mail.setTo(to);
                mail.setSubject(subject);
                mail.setText(body);
                mailSender.send(mail);
                log.info("Email sent successfully to {} on attempt {}", to, attempt);
                return;
            } catch (MailException e) {
                log.warn("Email attempt {}/3 failed for {}: {}", attempt, to, e.getMessage());
                lastException = e;
            }
        }
        log.error("Email delivery failed after 3 attempts for {}: {}", to, lastException.getMessage(), lastException);
        throw lastException;
    }
}
