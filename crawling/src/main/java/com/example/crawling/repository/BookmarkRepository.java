package com.example.crawling.repository;

import com.example.crawling.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUsername(String username);
    Optional<Bookmark> findByUsernameAndProductId(String username, Long productId);
    boolean existsByUsernameAndProductId(String username, Long productId);
    
    // 외래키(FK)가 없으므로 유저나 상품 삭제 시 수동 정리를 위한 메서드
    void deleteByUsername(String username);
    void deleteByProductId(Long productId);
}
