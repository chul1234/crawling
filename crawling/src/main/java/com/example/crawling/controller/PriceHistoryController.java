package com.example.crawling.controller;

import com.example.crawling.entity.PriceHistory;
import com.example.crawling.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/price-history")
@RequiredArgsConstructor
public class PriceHistoryController {

    private final PriceHistoryRepository priceHistoryRepository;

    @GetMapping("/{productId}")
    public ResponseEntity<List<PriceHistory>> getPriceHistory(@PathVariable Long productId) {
        List<PriceHistory> history = priceHistoryRepository.findByProductIdOrderByCreatedAtDesc(productId);
        return ResponseEntity.ok(history);
    }
}
