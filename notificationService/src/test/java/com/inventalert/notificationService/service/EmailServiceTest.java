package com.inventalert.notificationService.service;

import com.inventalert.notificationService.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender, "noreply@inventalert.com");
    }

    @Test
    void SendEmail_ValidEmail_CheckIfMailSentTest() {
        emailService.sendNotificationEmail(
                "adebayo@konga.ng", "RESTOCK_ALERT", "Low stock on Indomie noodles");

        verify(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void SendEmail_CheckIfFieldsSetCorrectlyTest() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendNotificationEmail(
                "chukwuemeka@fidelity.ng",
                "TRANSFER_SUGGESTION",
                "Transfer 50 units of garri from Apapa to Ikeja warehouse");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getFrom()).isEqualTo("noreply@inventalert.com");
        assertThat(sent.getTo()).containsExactly("chukwuemeka@fidelity.ng");
        assertThat(sent.getSubject()).isEqualTo("TRANSFER_SUGGESTION");
        assertThat(sent.getText()).isEqualTo("Transfer 50 units of garri from Apapa to Ikeja warehouse");
    }
}
