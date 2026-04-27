package com.learn.bankingapi.config;

import com.learn.bankingapi.enums.UserRole;
import com.learn.bankingapi.security.CustomAuthEntryPoint;
import com.learn.bankingapi.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public final CustomAuthEntryPoint authEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomAuthEntryPoint authEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
//                .exceptionHandling(ex -> ex
//                    .authenticationEntryPoint(authenticationEntryPoint()))
                .sessionManagement(sessoin -> sessoin
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        .requestMatchers("/api/users/**").hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
                        .requestMatchers("/api/accounts**").hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(authEntryPoint)
                )
                .build();

    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
