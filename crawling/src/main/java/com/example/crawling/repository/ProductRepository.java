package com.example.crawling.repository;

import com.example.crawling.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // TODO: 카테고리별 검색 및 페이징 처리를 위한 쿼리 메서드 작성
}
