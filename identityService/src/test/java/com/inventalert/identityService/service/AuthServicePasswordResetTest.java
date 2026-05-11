package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.ForgotPasswordRequest;
import com.inventalert.identityService.dto.request.ResetPasswordRequest;
import com.inventalert.identityService.exception.InvalidResetTokenException;
import com.inventalert.identityService.kafka.CompanyEventProducer;
import com.inventalert.identityService.kafka.PasswordResetEventProducer;
import com.inventalert.identityService.model.PasswordResetToken;
import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.PasswordResetTokenRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.repository.WarehouseAssignmentRepository;
import com.inventalert.identityService.security.service.JwtUtil;
import com.inventalert.identityService.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordResetTest {

    @Mock CompanyRepository companyRepository;
    @Mock UserRepository userRepository;
    @Mock WarehouseAssignmentRepository assignmentRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock CompanyEventProducer eventProducer;
    @Mock PasswordResetEventProducer passwordResetEventProducer;

    @InjectMocks AuthServiceImpl authService;

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Test
    void forgotPassword_knownEmail_savesTokenAndPublishesEvent() {
        User user = buildUser("u-1", "emeka@firstbank.ng");
        when(userRepository.findByEmail("emeka@firstbank.ng")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndUsedFalse("u-1")).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(new ForgotPasswordRequest("emeka@firstbank.ng"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo("u-1");
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(saved.isUsed()).isFalse();

        verify(passwordResetEventProducer).publishPasswordResetRequested(
                eq("u-1"), eq("emeka@firstbank.ng"), eq(saved.getToken()), any(LocalDateTime.class));
    }

    @Test
    void forgotPassword_unknownEmail_doesNotSaveOrPublish() {
        when(userRepository.findByEmail("nobody@x.ng")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("nobody@x.ng"));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetEventProducer, never()).publishPasswordResetRequested(any(), any(), any(), any());
    }

    @Test
    void forgotPassword_existingPendingToken_invalidatesOldOneBeforeIssuingNew() {
        User user = buildUser("u-1", "chidi@uba.ng");
        PasswordResetToken old = buildToken("u-1", "old-token", LocalDateTime.now().plusMinutes(30), false);

        when(userRepository.findByEmail("chidi@uba.ng")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndUsedFalse("u-1")).thenReturn(Optional.of(old));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword(new ForgotPasswordRequest("chidi@uba.ng"));

        // Old token must be marked used
        assertThat(old.isUsed()).isTrue();
        // Two saves: one for the old token (mark used), one for the new token
        verify(passwordResetTokenRepository, times(2)).save(any());
        verify(passwordResetEventProducer).publishPasswordResetRequested(any(), any(), any(), any());
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Test
    void resetPassword_validToken_updatesPasswordAndMarksTokenUsed() {
        User user = buildUser("u-1", "ngozi@access.ng");
        PasswordResetToken token = buildToken("u-1", "valid-token", LocalDateTime.now().plusMinutes(30), false);

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newSecure99")).thenReturn("new-hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.resetPassword(new ResetPasswordRequest("valid-token", "newSecure99"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
    }

    @Test
    void resetPassword_tokenNotFound_throwsInvalidResetTokenException() {
        when(passwordResetTokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("ghost-token", "newSecure99")))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_throwsInvalidResetTokenException() {
        PasswordResetToken expired = buildToken("u-1", "expired-token", LocalDateTime.now().minusMinutes(5), false);
        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("expired-token", "newSecure99")))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_alreadyUsedToken_throwsInvalidResetTokenException() {
        PasswordResetToken used = buildToken("u-1", "used-token", LocalDateTime.now().plusMinutes(30), true);
        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("used-token", "newSecure99")))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String id, String email) {
        return User.builder()
                .id(id)
                .companyId("co-1")
                .email(email)
                .passwordHash("old-hashed")
                .role(Role.ADMIN)
                .build();
    }

    private PasswordResetToken buildToken(String userId, String token, LocalDateTime expiresAt, boolean used) {
        PasswordResetToken t = new PasswordResetToken();
        t.setId("tok-id");
        t.setUserId(userId);
        t.setToken(token);
        t.setExpiresAt(expiresAt);
        t.setUsed(used);
        return t;
    }
}
