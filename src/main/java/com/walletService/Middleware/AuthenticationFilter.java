package com.walletService.Middleware;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:your-secret-key-change-this-in-production-min-256-bits}")
    private String jwtSecret;

    @Value("${jwt.enabled:true}")
    private boolean jwtEnabled;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            if (jwtEnabled) {
                String token = extractTokenFromRequest(request);

                if (token != null) {
                    Claims claims = validateAndParseToken(token);
                    setAuthentication(claims);
                }
            } else {
                String userIdHeader = request.getHeader(USER_ID_HEADER);

                if (userIdHeader != null && !userIdHeader.isEmpty()) {
                    Long userId = Long.parseLong(userIdHeader);
                    setSimpleAuthentication(userId);
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token expired");
            return;
        } catch (MalformedJwtException | UnsupportedJwtException | SignatureException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        } catch (NumberFormatException e) {
            log.warn("Invalid user ID in header: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID");
            return;
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }


    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }


    private Claims validateAndParseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    private void setAuthentication(Claims claims) {

        Long userId = null;

        if (claims.containsKey("userId")) {
            userId = claims.get("userId", Long.class);
        } else if (claims.containsKey("user_id")) {
            userId = claims.get("user_id", Long.class);
        } else if (claims.containsKey("id")) {
            userId = claims.get("id", Long.class);
        }

        if (userId == null) {
            throw new IllegalArgumentException("JWT token must contain userId claim");
        }

        String email = claims.getSubject();

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, email, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated user ID: {} with email: {}", userId, email);
    }


    private void setSimpleAuthentication(Long userId) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated user ID: {} (dev mode)", userId);
    }


    private void sendErrorResponse(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                        status == 401 ? "Unauthorized" : "Bad Request",
                        message)
        );
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/actuator") || path.startsWith("/h2-console");
    }
}
