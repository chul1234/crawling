package com.example.crawling.repository;

import com.example.crawling.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 카테고리로 페이징 검색
    Page<Product> findByCategory(String category, Pageable pageable);

    // 검색어(이름)로 페이징 검색
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    // 카테고리 + 검색어로 페이징 검색
    Page<Product> findByCategoryAndNameContainingIgnoreCase(String category, String keyword, Pageable pageable);

    // 크롤링 시 상품 중복 확인을 위한 메서드
    java.util.Optional<Product> findByProductUrl(String productUrl);

    // 할인율 1위 상품 조회 (품절 제외)
    java.util.Optional<Product> findFirstByIsSoldOutFalseOrderByDiscountRateDesc();

    // AI 요약용 상위 10개 상품 추출 (품절 제외)
    java.util.List<Product> findTop10ByIsSoldOutFalseOrderByDiscountRateDesc();

    @Modifying
    @Transactional
    @Query("DELETE FROM Product p WHERE p.source = :source")
    void deleteBySource(@Param("source") String source);

    // 1주일 경과 상품 청소를 위한 쿼리
    java.util.List<Product> findByUpdatedAtBefore(java.time.LocalDateTime dateTime);
}
