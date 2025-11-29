package com.gym.service.gymmanagementservice.config;

import com.gym.service.gymmanagementservice.filters.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApplicationConfig applicationConfig;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // Cho phép tất cả origins (có thể giới hạn trong production)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/auth/verify**", "/api/public/**", "/api/vnpay/ipn", "/api/vnpay/return").permitAll()
                        .requestMatchers("/api/members/qr/gym/**").permitAll()
                        .requestMatchers("/api/members/search").permitAll() // Cho phép tìm kiếm hội viên từ POS
                        .requestMatchers("/api/check-in/mobile", "/api/check-in/mobile/subscription", "/api/check-in/events").permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(applicationConfig.authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Cấu hình bảo mật cho Giao diện Web (dùng Session, Form Login)
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**") // Tắt CSRF cho API
                )
                .authorizeHttpRequests(auth -> auth
                        // Các trang yêu cầu quyền cụ thể
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        .requestMatchers("/pt/**").hasRole("PT")

                        .requestMatchers("/members/**").hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers("/check-in/**").hasAnyRole("ADMIN", "STAFF")

                        .requestMatchers("/pos/**").hasAnyRole("ADMIN", "STAFF")

                        // Các trang và tài nguyên công khai
                        .requestMatchers(
                                "/login",
                                "/signup",
                                "/error",
                                "/scan",
                                "/payment/vnpay/**",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/swagger.html",// Tài nguyên của webjar
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/default", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("gym-remember-key")
                        .rememberMeCookieName("GYM_REMEMBER_ME")
                        .tokenValiditySeconds(60 * 60 * 24 * 30)
                        .alwaysRemember(true)
                        .userDetailsService(userDetailsService)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/login?error=forbidden")
                );

        return http.build();
    }
}
