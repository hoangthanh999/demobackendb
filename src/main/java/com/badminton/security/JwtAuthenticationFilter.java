package com.badminton.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

                // ✅ PayOS endpoints - QUAN TRỌNG!
                || path.equals("/payments/payos/webhook") // ← THÊM DÒNG NÀY
                || path.equals("/payments/payos/callback") // ← THÊM DÒNG NÀY
                || path.startsWith("/payments/payos/status/") // ← THÊM DÒNG NÀY

                // ✅ MoMo webhook
                || path.equals("/payments/momo/webhook")

                // ✅ VNPay callback
                || path.equals("/payments/vnpay/callback")

                // ✅ Mock payment
                || path.startsWith("/payments/mock/")
                || path.startsWith("/shop/payments/mock/")

                || path.equals("/error")
                || method.equalsIgnoreCase("OPTIONS");

        if (shouldSkip) {
            log.info("⏭️  SKIPPING JWT Filter for: {} {}", method, path);
        } else {
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

        log.info("🔵 JWT Filter RUNNING for: {} {}", method, path);

        try {
            String jwt = getJwtFromRequest(request);
            log.info("🔑 JWT present: {}", jwt != null);

            if (StringUtils.hasText(jwt)) {
                log.info("📝 JWT: {}...", jwt.substring(0, Math.min(50, jwt.length())));

                if (jwtUtil.validateToken(jwt)) {
                    log.info("✅ JWT valid");

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

                    log.info("✅ Authentication set in SecurityContext");
                    log.info("✅ Current authorities: {}",
                            SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                } else {
                    log.error("❌ JWT validation FAILED");
                }
            } else {
                log.warn("⚠️  NO JWT found in request (path: {})", path);
            }
        } catch (Exception ex) {
            log.error("❌ JWT Filter exception", ex);
        }

        log.info("🔵 Continuing filter chain...");
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        log.debug("📋 Authorization header: {}",
                bearerToken != null ? bearerToken.substring(0, Math.min(30, bearerToken.length())) + "..." : "NULL");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("✅ Extracted JWT: {}...", token.substring(0, Math.min(30, token.length())));
            return token;
        }

        return null;
    }
}