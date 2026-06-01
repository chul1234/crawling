package com.example.crawling.controller;

import com.example.crawling.entity.Bookmark;
import com.example.crawling.entity.Product;
import com.example.crawling.repository.BookmarkRepository;
import com.example.crawling.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkRepository bookmarkRepository;
    private final ProductRepository productRepository;

    // 상품 찜하기 토글 (있으면 삭제, 없으면 추가)
    @PostMapping("/{productId}")
    public ResponseEntity<?> toggleBookmark(@PathVariable Long productId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        String username = authentication.getName();
        Optional<Bookmark> existing = bookmarkRepository.findByUsernameAndProductId(username, productId);

        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return ResponseEntity.ok(Map.of("status", "removed"));
        } else {
            Bookmark bookmark = new Bookmark();
            bookmark.setUsername(username);
            bookmark.setProductId(productId);
            bookmark.setCreatedAt(LocalDateTime.now());
            bookmarkRepository.save(bookmark);
            return ResponseEntity.ok(Map.of("status", "added"));
        }
    }

    // 내 관심상품(찜) 목록 조회
    @GetMapping("/my")
    public ResponseEntity<?> getMyBookmarks(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        String username = authentication.getName();
        List<Long> productIds = bookmarkRepository.findByUsername(username).stream()
                .map(Bookmark::getProductId)
                .collect(Collectors.toList());

        List<Product> bookmarkedProducts = productRepository.findAllById(productIds);
        return ResponseEntity.ok(bookmarkedProducts);
    }
}
