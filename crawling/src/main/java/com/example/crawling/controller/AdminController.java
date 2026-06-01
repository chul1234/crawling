package com.example.crawling.controller;

import com.example.crawling.entity.User;
import com.example.crawling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.crawling.entity.Role;
import com.example.crawling.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserDto(
                        u.getId(), 
                        u.getUsername(), 
                        u.getEmail(), 
                        u.getName(), 
                        u.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()),
                        u.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        String name = request.get("name");
        boolean isAdmin = Boolean.parseBoolean(request.get("isAdmin"));

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("아이디와 비밀번호는 필수입니다.");
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email == null ? "" : email);
        user.setName(name == null ? "" : name);
        user.setPassword(passwordEncoder.encode(password));

        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_USER");
            return roleRepository.save(r);
        });
        user.getRoles().add(userRole);

        if (isAdmin) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ROLE_ADMIN");
                return roleRepository.save(r);
            });
            user.getRoles().add(adminRole);
        }

        userRepository.save(user);
        return ResponseEntity.ok("회원이 성공적으로 생성되었습니다.");
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("유저를 찾을 수 없습니다.");
        }

        boolean makeAdmin = request.getOrDefault("isAdmin", false);
        Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_ADMIN");
            return roleRepository.save(r);
        });

        if (makeAdmin) {
            if (user.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
                user.getRoles().add(adminRole);
            }
        } else {
            user.getRoles().removeIf(r -> r.getName().equals("ROLE_ADMIN"));
        }

        userRepository.save(user);
        return ResponseEntity.ok("권한이 성공적으로 변경되었습니다.");
    }

    public record UserDto(Long id, String username, String email, String name, List<String> roles, java.time.LocalDateTime createdAt) {}
}
