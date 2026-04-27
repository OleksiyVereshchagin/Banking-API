package com.learn.bankingapi.security;


import com.learn.bankingapi.details.CustomUserDetails;
import com.learn.bankingapi.entity.User;
import com.learn.bankingapi.repository.UserRepository;
import com.learn.bankingapi.service.JwtService;
import com.learn.bankingapi.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // 1. Пропускаємо auth endpoints
        if (path.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        // 2. Якщо немає токена → просто далі (security сам вирішить)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = authHeader.substring(7);

            String userId = jwtService.extractUserId(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userRepository.findUserById(Long.valueOf(userId)).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

                UserDetails userDetails = new CustomUserDetails(user);

                if (jwtService.isTokenValid(jwt, userId)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("""
            {
              "status": 401,
              "message": "Invalid or malformed token"
            }
        """);
        }
    }

//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        String path = request.getServletPath();
//
//        if (path.startsWith("/api/auth")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        try {
//            String authHeader = request.getHeader("Authorization");
//
//            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//                filterChain.doFilter(request, response);
//                return;
//            }
//
//            String jwt = authHeader.substring(7);
//            String userId = jwtService.extractUserId(jwt);
//
//            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                User user = userRepository.findUserById(Long.valueOf(userId))
//                        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
//
//                UserDetails userDetails = new CustomUserDetails(user);
//
//                if (jwtService.isTokenValid(jwt, userId)) {
//                    UsernamePasswordAuthenticationToken authenticationToken =
//                            new UsernamePasswordAuthenticationToken(
//                                    userDetails,
//                                    null,
//                                    userDetails.getAuthorities()
//                            );
//
//                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//                }
//            }
//            filterChain.doFilter(request, response);
//            } catch (Exception e){
//            response.setStatus(401);
//            return;
//        }
//    }
}
