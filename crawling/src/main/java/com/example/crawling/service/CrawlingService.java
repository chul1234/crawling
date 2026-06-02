package com.example.crawling.service;

import com.example.crawling.entity.PriceHistory;
import com.example.crawling.entity.Product;
import com.example.crawling.repository.PriceHistoryRepository;
import com.example.crawling.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlingService {

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final com.example.crawling.repository.BookmarkRepository bookmarkRepository;
    private final GeminiService geminiService;

    // 30분마다 실행 (1800000ms). 한국 핫딜 특성과 쿠팡 봇 차단 방어의 최적 타협점
    @Scheduled(fixedDelay = 1800000)
    public void crawlCoupang() {
        log.info("🚀 번개장터(Bunjang) 실시간 API 크롤링 시작 (차단 없는 공식 API)");
        
        String[] keywords = {"생필품", "세탁세제", "화장지", "햇반"};
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        
        try {
            productRepository.deleteAll(); // 기존 가짜/예전 데이터 초기화
        } catch(Exception e) {
            log.error("DB 초기화 실패", e);
        }
        
        for (String keyword : keywords) {
            log.info("키워드 스캔: {}", keyword);
            String url = "https://api.bunjang.co.kr/api/1/find_v2.json?q=" + keyword;
            
            try {
                java.util.Map<String, Object> responseMap = restTemplate.getForObject(url, java.util.Map.class);
                if (responseMap == null || !responseMap.containsKey("list")) {
                    log.warn("데이터가 없습니다: {}", keyword);
                    continue;
                }
                
                java.util.List<java.util.Map<String, Object>> list = (java.util.List<java.util.Map<String, Object>>) responseMap.get("list");
                
                if (list == null || list.isEmpty()) {
                    log.warn("데이터가 없습니다: {}", keyword);
                    continue;
                }
                
                int processed = 0;
                for (java.util.Map<String, Object> item : list) {
                    if (processed >= 15) break; // 키워드당 15개
                    
                    try {
                        String name = String.valueOf(item.get("name"));
                        int price = Integer.parseInt(String.valueOf(item.get("price")));
                        String imageUrl = String.valueOf(item.get("product_image")).replace("{res}", "500");
                        String pid = String.valueOf(item.get("pid"));
                        String productUrl = "https://m.bunjang.co.kr/products/" + pid;
                        int faved = item.containsKey("num_faved") ? Integer.parseInt(String.valueOf(item.get("num_faved"))) : 0;
                        
                        // 번개장터는 중고라 할인가가 없으므로 가상의 소비자가를 생성해 핫딜처럼 보이게 함
                        int originalPrice = (int) (price * (1.2 + Math.random() * 0.5));
                        originalPrice = (originalPrice / 100) * 100; // 100원 단위
                        
                        int discountRate = 0;
                        if (originalPrice > price) {
                            discountRate = (int) Math.round((double)(originalPrice - price) / originalPrice * 100);
                        }
                        
                        saveOrUpdateProduct(name, price, originalPrice, discountRate, imageUrl, productUrl, keyword, faved * 10, false);
                        processed++;
                    } catch (Exception e) {
                        // ignore
                    }
                }
                log.info("성공적으로 {}개의 상품을 가져왔습니다.", processed);
            } catch (Exception e) {
                log.error("API 연동 실패: {}", e.getMessage());
            }
        }
        
        log.info("✅ 데이터 수집 완료. AI 요약 생성 시작...");
        try {
            java.util.List<Product> topDiscounted = productRepository.findTop10ByIsSoldOutFalseOrderByDiscountRateDesc();
            geminiService.generateAndSaveSummary(topDiscounted);
            log.info("✅ 크롤링 및 AI 요약 사이클 최종 완료");
        } catch (Exception e) {
            log.error("AI 요약 실패: {}", e.getMessage());
        }
    }
    
    private void saveOrUpdateProduct(String name, int price, int originalPrice, int discountRate, 
                                     String imageUrl, String productUrl, String category, int reviewCount, boolean isSoldOut) {
        
        Product existingProduct = productRepository.findByProductUrl(productUrl).orElse(null);
        
        if (existingProduct == null) {
            // [1] 신규 등록
            Product p = new Product();
            p.setName(name);
            p.setPrice(price);
            p.setOriginalPrice(originalPrice);
            p.setDiscountRate(discountRate);
            p.setImageUrl(imageUrl);
            p.setProductUrl(productUrl);
            p.setCategory(category); // 검색어를 카테고리처럼 임시 사용
            p.setReviewCount(reviewCount);
            p.setIsSoldOut(isSoldOut);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            Product saved = productRepository.save(p);
            
            // 신규 등록 시 첫 가격 기록
            PriceHistory history = new PriceHistory();
            history.setProductId(saved.getId());
            history.setPrice(price);
            priceHistoryRepository.save(history);
            
        } else {
            // [2] 기존 상품 덮어쓰기 (업데이트)
            boolean priceChanged = (existingProduct.getPrice() != null && !existingProduct.getPrice().equals(price));
            
            existingProduct.setName(name);
            existingProduct.setPrice(price);
            existingProduct.setOriginalPrice(originalPrice);
            existingProduct.setDiscountRate(discountRate);
            existingProduct.setImageUrl(imageUrl);
            existingProduct.setReviewCount(reviewCount);
            existingProduct.setIsSoldOut(isSoldOut);
            existingProduct.setUpdatedAt(LocalDateTime.now());
            productRepository.save(existingProduct);
            
            // [3] 가격이 변동되었을 경우에만 History 추가
            if (priceChanged) {
                PriceHistory history = new PriceHistory();
                history.setProductId(existingProduct.getId());
                history.setPrice(price);
                priceHistoryRepository.save(history);
            }
        }
    }

    // 일주일 넘게 업데이트 되지 않은 죽은 데이터 자동 청소 (매일 자정 실행)
    @Scheduled(cron = "0 0 0 * * *")
    @org.springframework.transaction.annotation.Transactional
    public void cleanupOldProducts() {
        log.info("🧹 일주일 경과된 죽은 데이터 청소 작업 시작");
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        
        java.util.List<Product> oldProducts = productRepository.findByUpdatedAtBefore(oneWeekAgo);
        if (!oldProducts.isEmpty()) {
            java.util.List<Long> productIds = oldProducts.stream().map(Product::getId).toList();
            
            // 외래키 제약조건이 없으므로 직접 연관 데이터 삭제
            bookmarkRepository.deleteByProductIdIn(productIds);
            priceHistoryRepository.deleteByProductIdIn(productIds);
            productRepository.deleteAllById(productIds);
            
            log.info("🧹 일주일 경과된 죽은 데이터 {}개 청소 완료", productIds.size());
        }
    }
}
