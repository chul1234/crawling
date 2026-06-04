package com.example.crawling.controller;

import com.example.crawling.entity.PriceHistory;
import com.example.crawling.entity.Product;
import com.example.crawling.repository.PriceHistoryRepository;
import com.example.crawling.repository.ProductRepository;
import com.example.crawling.service.CoupangPartnersService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ContributeController {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CoupangPartnersService coupangPartnersService;

    @PostMapping("/contribute")
    public ResponseEntity<?> contributeProduct(@RequestBody ContributeRequest request) {
        // Find existing or create new
        Product existingProduct = productRepository.findByProductUrl(request.getProductUrl()).orElse(null);
        
        if (existingProduct == null) {
            Product p = new Product();
            p.setName(request.getName());
            p.setPrice(request.getPrice());
            p.setOriginalPrice(request.getOriginalPrice() != null ? request.getOriginalPrice() : request.getPrice());
            p.setDiscountRate(request.getDiscountRate() != null ? request.getDiscountRate() : 0);
            p.setImageUrl(request.getImageUrl());
            p.setProductUrl(request.getProductUrl());
            p.setAffiliateUrl(coupangPartnersService.generateDeeplink(request.getProductUrl()));
            p.setCategory(request.getCategory() != null ? request.getCategory() : "기타");
            p.setReviewCount(0); // newly contributed
            p.setIsSoldOut(false);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            Product saved = productRepository.save(p);
            
            PriceHistory history = new PriceHistory();
            history.setProductId(saved.getId());
            history.setPrice(request.getPrice());
            priceHistoryRepository.save(history);
        } else {
            boolean priceChanged = (existingProduct.getPrice() != null && !existingProduct.getPrice().equals(request.getPrice()));
            
            existingProduct.setName(request.getName());
            existingProduct.setPrice(request.getPrice());
            if (request.getOriginalPrice() != null) existingProduct.setOriginalPrice(request.getOriginalPrice());
            if (request.getDiscountRate() != null) existingProduct.setDiscountRate(request.getDiscountRate());
            existingProduct.setImageUrl(request.getImageUrl());
            existingProduct.setAffiliateUrl(coupangPartnersService.generateDeeplink(request.getProductUrl()));
            existingProduct.setUpdatedAt(LocalDateTime.now());
            productRepository.save(existingProduct);
            
            if (priceChanged) {
                PriceHistory history = new PriceHistory();
                history.setProductId(existingProduct.getId());
                history.setPrice(request.getPrice());
                priceHistoryRepository.save(history);
            }
        }
        
        return ResponseEntity.ok().body(Map.of("message", "제보가 성공적으로 등록되었습니다."));
    }

    @Getter
    @Setter
    public static class ContributeRequest {
        private String name;
        private Integer price;
        private Integer originalPrice;
        private Integer discountRate;
        private String imageUrl;
        private String productUrl;
        private String category;
    }
}
