package com.example.crawling.service;

import com.example.crawling.entity.PriceHistory;
import com.example.crawling.entity.Product;
import com.example.crawling.repository.PriceHistoryRepository;
import com.example.crawling.repository.ProductRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
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

    // 3시간마다 실행 (10800000ms). 한도 고려하여 너무 빈번한 실행 금지
    @Scheduled(fixedDelay = 10800000)
    public void crawlCoupang() {
        log.info("🚀 쿠팡 크롤링 시작 (Playwright 가동)");
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            
            Page page = context.newPage();
            
            // 타겟 검색어 배열 (생필품 위주)
            String[] keywords = {"생필품", "세탁세제", "화장지", "햇반"};
            
            for (String keyword : keywords) {
                log.info("크롤링 키워드 스캔: {}", keyword);
                String url = "https://www.coupang.com/np/search?component=&q=" + keyword + "&channel=user";
                
                try {
                    page.navigate(url);
                    // 쿠팡은 동적 로딩이 있으므로 잠시 대기
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    
                    Locator products = page.locator("ul#productList > li.search-product");
                    int count = products.count();
                    log.info("찾은 상품 개수: {}", count);
                    
                    // 1회 수집량 한도 정책에 따라 페이지당 약 30~50개만 제한적으로 처리
                    for (int i = 0; i < Math.min(count, 30); i++) {
                        Locator item = products.nth(i);
                        try {
                            // 상품 정보 파싱
                            String name = item.locator("div.name").innerText();
                            String priceText = item.locator("strong.price-value").innerText().replaceAll("[^0-9]", "");
                            int price = Integer.parseInt(priceText);
                            
                            // 기본/원래 가격 (없을 수도 있음)
                            int originalPrice = price;
                            if (item.locator("del.base-price").count() > 0) {
                                String origPriceText = item.locator("del.base-price").innerText().replaceAll("[^0-9]", "");
                                if (!origPriceText.isEmpty()) {
                                    originalPrice = Integer.parseInt(origPriceText);
                                }
                            }
                            
                            // 할인율 계산
                            int discountRate = 0;
                            if (originalPrice > price) {
                                discountRate = (int) Math.round((double)(originalPrice - price) / originalPrice * 100);
                            }
                            
                            // 링크 및 이미지 (경로가 //로 시작하는 경우 보정)
                            String productUrl = "https://www.coupang.com" + item.locator("a.search-product-link").getAttribute("href");
                            String imageUrl = item.locator("img.search-product-wrap-img").getAttribute("src");
                            if (imageUrl != null && imageUrl.startsWith("//")) {
                                imageUrl = "https:" + imageUrl;
                            }
                            
                            // 리뷰 수 
                            int reviewCount = 0;
                            if (item.locator("span.rating-total-count").count() > 0) {
                                String reviewText = item.locator("span.rating-total-count").innerText().replaceAll("[^0-9]", "");
                                if (!reviewText.isEmpty()) {
                                    reviewCount = Integer.parseInt(reviewText);
                                }
                            }
                            
                            // 품절 여부 확인 (out-of-stock 클래스 유무)
                            boolean isSoldOut = item.locator("div.out-of-stock").count() > 0;
                            
                            saveOrUpdateProduct(name, price, originalPrice, discountRate, imageUrl, productUrl, keyword, reviewCount, isSoldOut);
                            
                        } catch (Exception e) {
                            log.warn("상품 파싱 중 일부 오류 발생 (스킵): {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("키워드 [{}] 크롤링 실패 (차단 의심 또는 네트워크 오류): {}", keyword, e.getMessage());
                }
            }
            context.close();
            browser.close();
            log.info("✅ 쿠팡 크롤링 사이클 완료");
        } catch (Exception e) {
            log.error("Playwright 실행 중 치명적 오류 발생", e);
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
}
