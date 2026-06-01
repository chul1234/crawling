package com.example.crawling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 로컬 테스트 및 API 통신을 위해 CSRF 비활성화 (실무 API 환경 구성)
            .authorizeHttpRequests(auth -> auth
                // CSS, JS, 이미지 등 정적 리소스와 로그인/회원가입 페이지는 누구나 접근 가능
                .requestMatchers("/css/**", "/js/**", "/images/**", "/login.html", "/api/login", "/signup.html", "/api/signup").permitAll()
                // 그 외 모든 요청은 로그인(인증) 필요 (index.html 포함)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")         // 커스텀 로그인 페이지 지정
                .loginProcessingUrl("/api/login") // 폼의 action 경로
                .defaultSuccessUrl("/index.html", true) // 성공 시 이동할 기본 페이지
                .failureUrl("/login.html?error=true")   // 실패 시 이동할 페이지
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout")
                .logoutSuccessUrl("/login.html")
                .permitAll()
            );

        return http.build();
    }

    // BCrypt 비밀번호 인코더 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
