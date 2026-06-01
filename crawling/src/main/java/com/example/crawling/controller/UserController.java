package com.example.crawling.controller;

import com.example.crawling.entity.Role;
import com.example.crawling.entity.User;
import com.example.crawling.repository.RoleRepository;
import com.example.crawling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        String name = request.get("name");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty() ||
            email == null || email.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("모든 필드(아이디, 비밀번호, 이메일, 이름)를 입력해주세요.");
        }

        // 비밀번호 8자 이상 및 특수문자 포함 여부 검사
        if (password.length() < 8 || !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return ResponseEntity.badRequest().body("비밀번호는 특수문자를 포함하여 8자 이상이어야 합니다.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password)); // BCrypt 암호화

        // Role 테이블을 그대로 사용하면서 user_roles에 텍스트가 들어가도록 복구
        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName("ROLE_USER");
            return roleRepository.save(newRole);
        });
        
        user.getRoles().add(userRole);
        userRepository.save(user);

        return ResponseEntity.ok("회원가입 성공");
    }
}
