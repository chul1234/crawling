package com.example.crawling.repository;

import com.example.crawling.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 카테고리별 페이징 검색 (카테고리가 'all'일 경우 findAll 사용)
    Page<Product> findByCategory(String category, Pageable pageable);
}
