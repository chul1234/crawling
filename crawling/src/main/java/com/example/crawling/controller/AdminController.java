package com.example.crawling.controller;

import com.example.crawling.entity.User;
import com.example.crawling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

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

    // 간단한 내부 DTO 레코드 (비밀번호 노출 방지)
    public record UserDto(Long id, String username, String email, String name, List<String> roles, java.time.LocalDateTime createdAt) {}
}
