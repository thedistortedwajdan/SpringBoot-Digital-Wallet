package com.app.wallet.security;

import com.app.wallet.exception.AccountDisabledException;
import com.app.wallet.exception.InvalidTokenException;
import com.app.wallet.model.Role;
import com.app.wallet.model.User;
import com.app.wallet.repository.UserRepository;
import com.app.wallet.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String SCHEME = "Bearer";
    private static final String BEARER_PREFIX = SCHEME + " ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SecurityExceptionHandler securityExceptionHandler;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserRepository userRepository,
                                   SecurityExceptionHandler securityExceptionHandler) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    /**
     * Login and registration must keep working even when a client sends a stale token.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || path.equals("/api/users/register");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // no credentials, a different scheme, or already authenticated: nothing for us to do
        if (authHeader == null
                || !authHeader.regionMatches(true, 0, SCHEME, 0, SCHEME.length())
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (!authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                    || authHeader.substring(BEARER_PREFIX.length()).isBlank()) {
                throw new InvalidTokenException("Malformed Authorization header");
            }

            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            Long userId = jwtService.getUserIdFromToken(token);

            User user = userRepository.findUserById(userId)
                    .orElseThrow(() -> new InvalidTokenException("Invalid token"));

            if (!user.isActive()) {
                throw new AccountDisabledException();
            }

            List<GrantedAuthority> authorities = Role.from(user.getRole())
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.authority()))
                    .stream()
                    .toList();

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));

        } catch (InvalidTokenException | AccountDisabledException e) {
            log.debug("rejecting request [{}]: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
            securityExceptionHandler.fail(request, response, e);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
