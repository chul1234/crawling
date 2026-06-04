package com.example.crawling.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer price;
    private Integer originalPrice;
    private Integer discountRate;
    private String imageUrl;
    private String productUrl;
    private String affiliateUrl;
    
    @Column(columnDefinition = "varchar(50) default 'CRAWLER'")
    private String source;
    
    private String category;
    
    @Column(columnDefinition = "int default 0")
    private Integer reviewCount;
    
    @Column(columnDefinition = "tinyint(1) default 0")
    private Boolean isSoldOut;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TODO: JPA Auditing(@CreatedDate, @LastModifiedDate) 추가 예정
}
