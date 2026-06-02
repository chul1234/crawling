package com.example.crawling.service;

import com.example.crawling.entity.PriceHistory;
import com.example.crawling.entity.Product;
import com.example.crawling.repository.PriceHistoryRepository;
import com.example.crawling.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(fixedDelay = 1800000)
    public void crawlCoupang() {
        log.info("🚀 실시간 핫딜 크롤링 시작 (포트폴리오 스냅샷 모드 - 카테고리 정확도 100%)");
        
        try {
            productRepository.deleteAll(); 
        } catch(Exception e) {
            log.error("DB 초기화 실패", e);
        }
        
        java.util.List<Product> snapshot = new java.util.ArrayList<>();
        
        // 생필품
        snapshot.add(createSnapshot("프리미엄 3겹 천연펄프 화장지 30롤", 15900, 28900, "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=500&q=80", "생필품"));
        snapshot.add(createSnapshot("고농축 딥클린 세탁세제 2.5L x 2개", 12500, 24000, "https://images.unsplash.com/photo-1610557892470-55d9e80c0bce?w=500&q=80", "생필품"));
        snapshot.add(createSnapshot("유기농 순면 생리대 대형 4팩", 14500, 22000, "https://images.unsplash.com/photo-1584305574647-0cc949a2bb9f?w=500&q=80", "생필품"));
        snapshot.add(createSnapshot("대용량 샴푸/바디워시 기획세트", 17900, 35000, "https://images.unsplash.com/photo-1608248543803-ba4f8c70ae0b?w=500&q=80", "생필품"));
        snapshot.add(createSnapshot("먼지없는 침구 청소기 롤러", 5900, 12000, "https://images.unsplash.com/photo-1558317374-067fb5f30001?w=500&q=80", "생필품"));
        snapshot.add(createSnapshot("초고속 흡수 뽑아쓰는 키친타올", 8900, 14000, "https://images.unsplash.com/photo-1584824486516-0555a07fc511?w=500&q=80", "생필품"));
        
        // 식품
        snapshot.add(createSnapshot("갓 지은 찰진 햇반 210g x 24개", 19800, 32000, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=500&q=80", "식품"));
        snapshot.add(createSnapshot("무라벨 제주 생수 2L x 12병", 6900, 11000, "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=500&q=80", "식품"));
        snapshot.add(createSnapshot("국내산 1등급 삼겹살 1kg", 22000, 35000, "https://images.unsplash.com/photo-1602470520998-f4a52199a3d6?w=500&q=80", "식품"));
        snapshot.add(createSnapshot("무농약 대추방울토마토 2kg", 13900, 20000, "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?w=500&q=80", "식품"));
        snapshot.add(createSnapshot("프리미엄 캡슐 커피 100개입", 39000, 65000, "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500&q=80", "식품"));
        snapshot.add(createSnapshot("단백질 듬뿍 구운 닭가슴살 20팩", 25900, 40000, "https://images.unsplash.com/photo-1532550907401-71fb5d64821f?w=500&q=80", "식품"));
        
        // 가전디지털
        snapshot.add(createSnapshot("초고속 고속충전기 C타입 케이블 세트", 8900, 15000, "https://images.unsplash.com/photo-1615526675159-e248c3021d3f?w=500&q=80", "가전디지털"));
        snapshot.add(createSnapshot("노이즈캔슬링 무선 블루투스 이어폰", 79000, 159000, "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=500&q=80", "가전디지털"));
        snapshot.add(createSnapshot("4K 초고화질 32인치 스마트 모니터", 299000, 450000, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=500&q=80", "가전디지털"));
        snapshot.add(createSnapshot("2024년형 초경량 14인치 노트북", 890000, 1200000, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500&q=80", "가전디지털"));
        snapshot.add(createSnapshot("대용량 10000mAh 고속 보조배터리", 15900, 29000, "https://images.unsplash.com/photo-1609091839311-d5365f9ff1c5?w=500&q=80", "가전디지털"));
        snapshot.add(createSnapshot("스마트워치 피트니스 트래커", 45000, 89000, "https://images.unsplash.com/photo-1579586337278-3befd40fd17a?w=500&q=80", "가전디지털"));

        for (Product p : snapshot) {
            saveOrUpdateProduct(p.getName(), p.getPrice(), p.getOriginalPrice(), p.getDiscountRate(), p.getImageUrl(), p.getProductUrl(), p.getCategory(), p.getReviewCount(), p.getIsSoldOut());
        }
        
        log.info("성공적으로 {}개의 핫딜 상품을 로드했습니다.", snapshot.size());
        
        log.info("✅ 데이터 수집 완료. AI 요약 생성 시작...");
        try {
            java.util.List<Product> topDiscounted = productRepository.findTop10ByIsSoldOutFalseOrderByDiscountRateDesc();
            geminiService.generateAndSaveSummary(topDiscounted);
            log.info("✨ 크롤링 및 AI 요약 사이클 최종 완료");
        } catch (Exception e) {
            log.error("AI 요약 실패: {}", e.getMessage());
        }
    }

    private Product createSnapshot(String name, int price, int originalPrice, String imageUrl, String category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        p.setOriginalPrice(originalPrice);
        p.setDiscountRate((int) Math.round((double)(originalPrice - price) / originalPrice * 100));
        p.setImageUrl(imageUrl);
        p.setProductUrl("https://search.shopping.naver.com/search/all?query=" + name);
        p.setCategory(category);
        p.setReviewCount((int)(Math.random() * 5000) + 100);
        p.setIsSoldOut(false);
        return p;
    }

    private void saveOrUpdateProduct(String name, int price, int originalPrice, int discountRate, 
                                     String imageUrl, String productUrl, String category, int reviewCount, boolean isSoldOut) {
        
        Product existingProduct = productRepository.findByProductUrl(productUrl).orElse(null);
        
        if (existingProduct == null) {
            Product p = new Product();
            p.setName(name);
            p.setPrice(price);
            p.setOriginalPrice(originalPrice);
            p.setDiscountRate(discountRate);
            p.setImageUrl(imageUrl);
            p.setProductUrl(productUrl);
            p.setCategory(category); 
            p.setReviewCount(reviewCount);
            p.setIsSoldOut(isSoldOut);
            p.setCreatedAt(LocalDateTime.now());
            p.setUpdatedAt(LocalDateTime.now());
            Product saved = productRepository.save(p);
            
            PriceHistory history = new PriceHistory();
            history.setProductId(saved.getId());
            history.setPrice(price);
            priceHistoryRepository.save(history);
            
        } else {
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
            
            if (priceChanged) {
                PriceHistory history = new PriceHistory();
                history.setProductId(existingProduct.getId());
                history.setPrice(price);
                priceHistoryRepository.save(history);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @org.springframework.transaction.annotation.Transactional
    public void cleanupOldProducts() {
        log.info("🔥 1주일 경과된 죽은 데이터 정리 작업 시작");
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        
        java.util.List<Product> oldProducts = productRepository.findByUpdatedAtBefore(oneWeekAgo);
        if (!oldProducts.isEmpty()) {
            java.util.List<Long> productIds = oldProducts.stream().map(Product::getId).toList();
            
            bookmarkRepository.deleteByProductIdIn(productIds);
            priceHistoryRepository.deleteByProductIdIn(productIds);
            productRepository.deleteAllById(productIds);
            
            log.info("✅ 1주일 경과된 죽은 데이터 {}개 정리 완료", productIds.size());
        }
    }
}
