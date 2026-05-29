package com.example.crawling.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    // TODO: ProductService 주입 및 페이징, 카테고리 필터링 API 구현

    @GetMapping
    public String getProducts() {
        return "상품 목록 API 준비 완료";
    }
}
