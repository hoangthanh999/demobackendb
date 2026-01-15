// backend/src/main/java/com/badminton/security/JwtAuthenticationFilter.java

package com.badminton.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ← THÊM
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j // ← THÊM
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // ✅ Bỏ qua filter cho các endpoint public
        boolean shouldSkip = path.startsWith("/auth/")
                || (path.equals("/courts") && method.equals("GET"))
                || path.startsWith("/courts/search")
                || (path.matches("/courts/\\d+") && method.equals("GET"))
                || path.equals("/payments/momo/webhook")
                || path.startsWith("/payments/mock/")
                || path.equals("/error")
                || method.equalsIgnoreCase("OPTIONS");

        if (!shouldSkip) {
            log.debug("🔒 JWT Filter will process: {} {}", method, path);
        }

        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();
        String method = request.getMethod();

        try {
            String jwt = getJwtFromRequest(request);

            log.info("🔍 JWT Filter - Path: {} {}, JWT present: {}", method, path, jwt != null);

            if (StringUtils.hasText(jwt)) {
                log.info("🔑 JWT Token: {}...", jwt.substring(0, Math.min(50, jwt.length())));

                if (jwtUtil.validateToken(jwt)) {
                    String email = jwtUtil.getEmailFromToken(jwt);
                    log.info("📧 Email from token: {}", email);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    log.info("👤 User loaded: {}", userDetails.getUsername());
                    log.info("🔐 Authorities: {}", userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("✅ Authentication set successfully for: {}", email);
                    log.info("✅ Authorities in SecurityContext: {}",
                            SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                } else {
                    log.warn("❌ JWT validation failed");
                }
            } else {
                log.warn("⚠️ No JWT token found in request to: {} {}", method, path);
            }
        } catch (Exception ex) {
            log.error("❌ Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        log.debug("📋 Authorization header: {}",
                bearerToken != null ? bearerToken.substring(0, Math.min(20, bearerToken.length())) + "..." : "NULL");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
