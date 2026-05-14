package com.inventalert.notificationService.security;

import com.inventalert.notificationService.security.model.JwtUser;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Only CONNECT frames carry the Authorization header; subsequent SEND/SUBSCRIBE frames
        // inherit the principal set here, so we validate once per session rather than per message
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Missing or invalid Authorization header");
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.isTokenValid(token)) {
                throw new MessagingException("Invalid or expired token");
            }
            JwtUser principal = new JwtUser(
                    jwtUtil.extractUserId(token),
                    jwtUtil.extractCompanyId(token),
                    jwtUtil.extractRole(token),
                    jwtUtil.extractWarehouseId(token)
            );
            // Attaching the authentication to the accessor binds it to the STOMP session
            // so @MessageMapping methods receive a populated SecurityContext automatically
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()));
        }

        return message;
    }
}
