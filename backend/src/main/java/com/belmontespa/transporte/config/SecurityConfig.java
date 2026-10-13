package com.belmontespa.transporte.config;

import com.belmontespa.transporte.service.TransporteUserDetailsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationProvider authenticationProvider) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/viajes/*/asignar").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/viajes/*/estado").hasRole("CONDUCTOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/viajes/*/cancelar").hasRole("SOLICITANTE")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pagos/*/estado").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/v1/administradores/**", "/api/v1/moviles/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/v1/conductores/**", "/api/v1/habilitaciones-movil-conductor/**")
                            .hasAnyRole("ADMINISTRADOR", "CONDUCTOR")
                        .requestMatchers("/api/v1/solicitantes/**", "/api/v1/empresas/**")
                            .hasAnyRole("ADMINISTRADOR", "SOLICITANTE")
                        .requestMatchers("/api/v1/pasajeros/**", "/api/v1/pasajeros-viaje/**")
                            .hasAnyRole("ADMINISTRADOR", "SOLICITANTE")
                        .requestMatchers("/api/v1/pagos/**")
                            .hasAnyRole("ADMINISTRADOR", "SOLICITANTE")
                        .requestMatchers("/api/v1/viajes/**")
                            .hasAnyRole("ADMINISTRADOR", "CONDUCTOR", "SOLICITANTE")
                        .requestMatchers("/api/v1/usuarios/**")
                            .hasAnyRole("ADMINISTRADOR", "CONDUCTOR", "SOLICITANTE")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthenticationProvider authenticationProvider(TransporteUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new org.springframework.security.authentication.ProviderManager(authenticationProvider);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** El filtro se ejecuta dentro de Spring Security, no como filtro servlet duplicado. */
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterServletRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
