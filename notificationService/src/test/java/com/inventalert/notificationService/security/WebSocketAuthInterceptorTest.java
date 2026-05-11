package com.inventalert.notificationService.security;

import com.inventalert.notificationService.security.model.JwtUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock JwtUtil jwtUtil;
    @Mock MessageChannel channel;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtUtil);
    }

    @Test
    void PreSend_ValidConnectFrame_CheckIfUserSetTest() {
        when(jwtUtil.isTokenValid("valid.token")).thenReturn(true);
        when(jwtUtil.extractUserId("valid.token")).thenReturn("adebayo-001");
        when(jwtUtil.extractCompanyId("valid.token")).thenReturn("konga-001");
        when(jwtUtil.extractRole("valid.token")).thenReturn("STAFF");
        when(jwtUtil.extractWarehouseId("valid.token")).thenReturn(null);

        Message<?> result = interceptor.preSend(connectMessage("Bearer valid.token"), channel);

        assertThat(result).isNotNull();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) accessor.getUser();
        assertThat(auth).isNotNull();
        JwtUser principal = (JwtUser) auth.getPrincipal();
        assertThat(principal.getUserId()).isEqualTo("adebayo-001");
        assertThat(principal.getCompanyId()).isEqualTo("konga-001");
    }

    @Test
    void PreSend_MissingAuthHeader_CheckIfExceptionThrownTest() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channel))
                .isInstanceOf(MessagingException.class);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void PreSend_NoBearerPrefix_CheckIfExceptionThrownTest() {
        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Basic dXNlcjpwYXNz"), channel))
                .isInstanceOf(MessagingException.class);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void PreSend_InvalidToken_CheckIfExceptionThrownTest() {
        when(jwtUtil.isTokenValid("bad.token")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer bad.token"), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void PreSend_NonConnectFrame_CheckIfPassedThroughTest() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isNotNull();
        verifyNoInteractions(jwtUtil);
    }

    private Message<?> connectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
