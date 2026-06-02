package com.example.crawling.controller;

import com.example.crawling.entity.AiSummary;
import com.example.crawling.repository.AiSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiSummaryController {

    private final AiSummaryRepository aiSummaryRepository;

    @GetMapping("/summary")
    public ResponseEntity<?> getLatestSummary() {
        return aiSummaryRepository.findFirstByOrderByCreatedAtDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
