package com.example.crawling.config;

import com.example.crawling.entity.Product;
import com.example.crawling.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DummyDataInit implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // 상품 데이터가 하나도 없을 때만 임시 더미 데이터 생성
        if (productRepository.count() == 0) {
            List<Product> dummies = Arrays.asList(
                createProduct("프리미엄 천연 펄프 3겹 롤화장지 30롤", "생필품", 15900, 28900, 45, false, 1204, "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?q=80&w=600&auto=format&fit=crop"),
                createProduct("2024년형 울트라 스마트 워치 티타늄", "가전디지털", 99000, 309000, 68, false, 4592, "https://images.unsplash.com/photo-1546868871-7041f2a55e12?q=80&w=600&auto=format&fit=crop"),
                createProduct("햅쌀 프리미엄 유기농 백미 10kg", "식품", 32500, 35000, 0, true, 856, "https://images.unsplash.com/photo-1596755094514-f87e34085b2c?q=80&w=600&auto=format&fit=crop"),
                createProduct("레몬 탄산수 제로 칼로리 500ml x 20입", "식품", 12900, 18500, 30, false, 3211, "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?q=80&w=600&auto=format&fit=crop"),
                createProduct("고속 충전기 65W GaN 멀티 포트", "가전디지털", 24900, 49900, 50, false, 210, "https://images.unsplash.com/photo-1583394838336-acd977736f90?q=80&w=600&auto=format&fit=crop"),
                createProduct("친환경 뽑아쓰는 키친타올 100매 x 6팩", "생필품", 8900, 12900, 31, false, 542, "https://images.unsplash.com/photo-1584824486509-112e4181f1b6?q=80&w=600&auto=format&fit=crop")
            );
            productRepository.saveAll(dummies);
        }
    }

    private Product createProduct(String name, String category, int price, int originalPrice, int discountRate, boolean isSoldOut, int reviewCount, String imageUrl) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        p.setOriginalPrice(originalPrice);
        p.setDiscountRate(discountRate);
        p.setIsSoldOut(isSoldOut);
        p.setReviewCount(reviewCount);
        p.setImageUrl(imageUrl);
        p.setProductUrl("https://www.coupang.com");
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return p;
    }
}
