package com.huyao.config;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.huyao.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_GET = Set.of(
            "/api/characters",
            "/api/character-skins",
            "/api/character-skills",
            "/api/card-pools",
            "/api/home-character");

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/api/auth/")
                || ("GET".equalsIgnoreCase(request.getMethod()) && PUBLIC_GET.contains(path))) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        try {
            JwtUtil.JwtUser user = jwtUtil.parseToken(token);
            request.setAttribute("authUserId", user.userId());
            request.setAttribute("authUsername", user.username());
            response.setHeader("X-Auth-Token", jwtUtil.generateToken(user.userId(), user.username()));
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
