package com.example.crawling.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // 외래키 물리적 제약조건 없이 논리적으로만 매핑 (행동 강령 준수)
    // 숫자가 아닌 직관적인 텍스트(username, role_name)로 DB user_roles 테이블이 구성되도록 수정
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "username", referencedColumnName = "username", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT)),
        inverseJoinColumns = @JoinColumn(name = "role_name", referencedColumnName = "name", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    )
    private Set<Role> roles = new HashSet<>();
}
