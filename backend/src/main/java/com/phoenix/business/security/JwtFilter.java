package com.phoenix.business.security;

import com.phoenix.business.domain.User;
import com.phoenix.business.repository.*;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final RevokedTokenRepository revoked;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        User user = null;
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                if (!revoked.existsById(claims.getId())) {
                    user = users.findByUsernameIgnoreCase(claims.getSubject()).filter(User::isActive).orElse(null);
                }
            } catch (Exception ignored) { /* Invalid/expired tokens remain unauthenticated. */ }
        }
        if (user != null) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    user.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
            // Read current state on every request, including tokens issued before a password reset.
            if (user.isForcePasswordChange() && !allowedWhileChanging(req)) {
                res.setStatus(403);
                res.setContentType("application/json");
                res.getWriter().write("{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"Change your temporary password before continuing.\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private boolean allowedWhileChanging(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String method = req.getMethod();
        if (method.equals("OPTIONS")) return true;
        if (method.equals("GET")) return path.equals("/api/auth/me") || path.equals("/actuator/health")
                || path.equals("/api/public/branding") || path.startsWith("/api/public/branding-assets/");
        return method.equals("POST") && (path.equals("/api/auth/change-password")
                || path.equals("/api/auth/logout") || path.equals("/api/auth/login"));
    }
}
