package com.taskmanager.realtime;

import com.taskmanager.security.JwtService;
import com.taskmanager.workspace.repository.WorkspaceMemberRepository;
import io.jsonwebtoken.Claims;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame with the JWT carried in the {@code Authorization} header and
 * authorizes SUBSCRIBE to {@code /topic/workspace/{id}} by verifying the user is a member of that
 * workspace. The connected user's id is stored as the session principal.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String WS_TOPIC_PREFIX = "/topic/workspace/";

    private final JwtService jwtService;
    private final WorkspaceMemberRepository memberRepository;

    public StompAuthChannelInterceptor(JwtService jwtService, WorkspaceMemberRepository memberRepository) {
        this.jwtService = jwtService;
        this.memberRepository = memberRepository;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = authenticate(accessor);
            accessor.setUser(new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, List.of()));
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private Long authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token on STOMP CONNECT");
        }
        try {
            Claims claims = jwtService.parse(header.substring(7));
            return jwtService.extractUserId(claims);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid token on STOMP CONNECT");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (dest == null || !dest.startsWith(WS_TOPIC_PREFIX) || accessor.getUser() == null) {
            return;
        }
        long workspaceId;
        try {
            workspaceId = Long.parseLong(dest.substring(WS_TOPIC_PREFIX.length()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Bad workspace topic: " + dest);
        }
        Long userId = Long.valueOf(accessor.getUser().getName());
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new IllegalArgumentException("Not a member of workspace " + workspaceId);
        }
    }
}
