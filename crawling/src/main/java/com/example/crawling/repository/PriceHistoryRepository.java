package com.example.crawling.repository;

import com.example.crawling.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdOrderByCreatedAtDesc(Long productId);
}
