package com.example.crawling.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 물리적 외래키 없이 논리적 매핑 (유저 아이디)
    @Column(nullable = false, length = 50)
    private String username;

    // 물리적 외래키 없이 논리적 매핑 (상품 ID)
    @Column(nullable = false)
    private Long productId;

    private LocalDateTime createdAt;
}
