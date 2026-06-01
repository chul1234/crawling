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

    @GetMapping("/users/me")
    public ResponseEntity<?> getCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인되지 않았습니다.");
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).body("유저를 찾을 수 없습니다.");
        }

        java.util.List<String> roles = authentication.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList());
                
        java.util.Map<String, Object> responseMap = new java.util.HashMap<>();
        responseMap.put("username", user.getUsername());
        responseMap.put("name", user.getName());
        responseMap.put("email", user.getEmail());
        responseMap.put("createdAt", user.getCreatedAt());
        responseMap.put("roles", roles);

        return ResponseEntity.ok(responseMap);
    }

    @PutMapping("/users/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody Map<String, String> request, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인되지 않았습니다.");
        }
        
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("유저를 찾을 수 없습니다.");
        }

        String name = request.get("name");
        String email = request.get("email");
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (name != null && !name.trim().isEmpty()) user.setName(name);
        if (email != null && !email.trim().isEmpty()) user.setEmail(email);

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.badRequest().body("현재 비밀번호가 일치하지 않습니다.");
            }
            if (newPassword.length() < 8 || !newPassword.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
                return ResponseEntity.badRequest().body("새 비밀번호는 특수문자를 포함하여 8자 이상이어야 합니다.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        return ResponseEntity.ok("정보가 성공적으로 수정되었습니다.");
    }
}
