package com.example.oa.module.notification.websocket;

import com.example.oa.common.constant.SecurityConstants;
import com.example.oa.security.jwt.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenUtil jwtTokenUtil;
    private final WebSocketTicketService ticketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        var query = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        Long ticketUserId = ticketService.consume(query.getFirst("ticket"));
        if (ticketUserId != null) {
            attributes.put("userId", ticketUserId);
            return true;
        }
        String token = query.getFirst("token");
        if (StringUtils.hasText(token) && token.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            token = token.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        if (!StringUtils.hasText(token) || !jwtTokenUtil.validateToken(token)) {
            return false;
        }
        attributes.put("userId", jwtTokenUtil.userId(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
