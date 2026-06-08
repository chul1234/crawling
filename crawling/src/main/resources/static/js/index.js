document.addEventListener('DOMContentLoaded', () => {
    console.log("메인 페이지 로드 완료 - 데이터 Fetch 준비");
    
    // 테마 토글 관련 로직
    const themeToggleBtn = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;

    // 1. 로드 시 이전에 저장된 테마 불러오기 (기본값: 라이트 모드)
    const savedTheme = localStorage.getItem('theme') || 'light';
    htmlElement.setAttribute('data-theme', savedTheme);
    updateToggleButtonText(savedTheme);

    // 2. 버튼 클릭 이벤트
    themeToggleBtn.addEventListener('click', () => {
        const currentTheme = htmlElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        
        // 속성 변경 및 저장
        htmlElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateToggleButtonText(newTheme);
    });

    // 로그아웃 버튼 클릭 이벤트
    document.getElementById('logout-btn').addEventListener('click', () => {
        if(confirm('로그아웃 하시겠습니까?')) {
            // 스프링 시큐리티의 기본 /api/logout 은 POST를 권장함 (CSRF가 비활성화되어 있으므로 GET도 가능하지만 POST로 명시)
            fetch('/api/logout', { method: 'POST' })
                .then(() => {
                    window.location.href = '/login.html';
                });
        }
    });

    // 버튼 텍스트 업데이트 함수
    function updateToggleButtonText(theme) {
        if (theme === 'dark') {
            themeToggleBtn.innerHTML = '☀️ 라이트 모드';
        } else {
            themeToggleBtn.innerHTML = '🌙 다크 모드';
        }
    }

    // 현재 사용자 권한 확인 후 관리자/마이페이지 탭 노출
    fetch('/api/users/me')
        .then(res => res.json())
        .then(data => {
            // 로그인 상태이면 마이페이지 탭 노출
            if (data && data.username) {
                document.getElementById('mypage-tab').style.display = 'inline-block';
            }
            // 관리자면 관리자 탭 추가 노출
            if (data && data.roles && data.roles.includes('ROLE_ADMIN')) {
                document.getElementById('admin-tab').style.display = 'inline-block';
            }
        })
        .catch(err => console.log('사용자 정보 로드 실패 (비로그인 상태)', err));

    // API 호출 및 데이터 렌더링 관련 상태 변수
    let currentCategory = 'all';
    let currentPage = 0;
    let currentKeyword = '';
    window.myBookmarkedProductIds = new Set();
    const pageSize = 12;

    const productGrid = document.getElementById('product-grid');
    const paginationDiv = document.getElementById('pagination');
    const categoryButtons = document.querySelectorAll('#category-filter button');
    const searchInput = document.getElementById('search-input');
    const searchBtn = document.getElementById('search-btn');

    // 검색 이벤트
    searchBtn.addEventListener('click', () => {
        currentKeyword = searchInput.value.trim();
        currentPage = 0;
        fetchProducts();
    });

    searchInput.addEventListener('keyup', (e) => {
        if (e.key === 'Enter') {
            searchBtn.click();
        }
    });

    // 상품 데이터 불러오기 함수
    async function fetchProducts() {
        try {
            let url = `/api/products?category=${encodeURIComponent(currentCategory)}&page=${currentPage}&size=${pageSize}`;
            if (currentKeyword) {
                url += `&keyword=${encodeURIComponent(currentKeyword)}`;
            }
            const response = await fetch(url);
            if (!response.ok) {
                if (response.status === 401 || response.status === 403) {
                    window.location.href = '/login.html'; // 로그인 안 되어 있으면 이동
                }
                throw new Error('데이터를 불러오는데 실패했습니다.');
            }
            const data = await response.json();
            renderProducts(data.content);
            renderPagination(data.totalPages);
        } catch (error) {
            console.error('Error fetching products:', error);
            productGrid.innerHTML = '<p class="error-msg">상품을 불러오는 중 오류가 발생했습니다.</p>';
        }
    }

    // 상품 카드 렌더링
    function renderProducts(products) {
        if (!products || products.length === 0) {
            productGrid.innerHTML = '<p class="empty-msg">해당 카테고리에 상품이 없습니다.</p>';
            return;
        }

        productGrid.innerHTML = products.map(p => {
            const discountClass = p.isSoldOut ? 'sold-out-text' : 'discount-rate';
            const discountText = p.isSoldOut ? '품절' : (p.discountRate ? `${p.discountRate}%` : '');
            const cardClass = p.isSoldOut ? 'product-card sold-out' : 'product-card';
            const overlay = p.isSoldOut ? `<div class="sold-out-overlay"><span>일시품절</span></div>` : '';
            
            // 할인율이 50 이상이면 초특가 뱃지, 유저 제보 상품이면 프리미엄 뱃지
            let badge = '';
            let highlightClass = '';
            if (!p.isSoldOut) {
                if (p.source === 'USER') {
                    badge = `<span class="badge badge-userpick">✨ 유저 픽</span>`;
                    highlightClass = 'highlight-userpick';
                } else if (p.discountRate >= 50) {
                    badge = `<span class="badge badge-hot">🔥 초특가</span>`;
                    highlightClass = 'highlight-card';
                } else {
                    badge = `<span class="badge badge-rocket">🚀 로켓배송</span>`;
                }
            }

            return `
            <article class="${cardClass} ${highlightClass}">
                <div class="image-wrapper">
                    <img src="${p.imageUrl || 'https://via.placeholder.com/600x600?text=No+Image'}" alt="${p.name}">
                    ${badge}
                    ${overlay}
                    <button class="bookmark-btn" onclick="toggleBookmark(${p.id}, this)" style="position: absolute; bottom: 10px; right: 10px; background: rgba(255,255,255,0.8); border: none; border-radius: 50%; width: 36px; height: 36px; cursor: pointer; font-size: 1.2rem; box-shadow: 0 2px 5px rgba(0,0,0,0.2);">${window.myBookmarkedProductIds && window.myBookmarkedProductIds.has(p.id) ? '❤️' : '🤍'}</button>
                </div>
                <div class="product-info">
                    <span class="category-text">${p.category || '기타'}</span>
                    <h3 class="product-name"><a href="${p.affiliateUrl || p.productUrl || '#'}" target="_blank" style="text-decoration:none; color:inherit;">${p.name}</a></h3>
                    <div class="price-container">
                        <div class="price-row">
                            ${discountText ? `<span class="${discountClass}">${discountText}</span>` : ''}
                            <span class="current-price">${p.price ? p.price.toLocaleString() : 0}<span class="unit">원</span></span>
                        </div>
                        ${p.originalPrice ? `<span class="original-price">${p.originalPrice.toLocaleString()}원</span>` : ''}
                    </div>
                    <div class="card-footer" style="display:flex; justify-content:space-between; align-items:center;">
                        <span class="review-count">⭐ 리뷰 ${p.reviewCount ? p.reviewCount.toLocaleString() : 0}</span>
                        <a href="/price-history.html?id=${p.id}" class="history-link-btn" style="font-size:0.9rem; color:#007bff; text-decoration:none; font-weight:600;">📈 가격추이</a>
                    </div>
                </div>
            </article>
            `;
        }).join('');
    }

    // 페이지네이션 렌더링
    function renderPagination(totalPages) {
        if (totalPages <= 1) {
            paginationDiv.innerHTML = '';
            return;
        }

        let buttonsHtml = '';
        for (let i = 0; i < totalPages; i++) {
            const activeClass = i === currentPage ? 'active' : '';
            buttonsHtml += `<button class="page-btn ${activeClass}" data-page="${i}">${i + 1}</button>`;
        }
        paginationDiv.innerHTML = buttonsHtml;

        // 페이징 버튼 클릭 이벤트 등록
        document.querySelectorAll('.page-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                currentPage = parseInt(e.target.getAttribute('data-page'));
                fetchProducts();
                window.scrollTo({ top: 0, behavior: 'smooth' }); // 화면 위로
            });
        });
    }

    // 카테고리 버튼 클릭 이벤트
    categoryButtons.forEach(btn => {
        btn.addEventListener('click', (e) => {
            categoryButtons.forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            
            currentCategory = e.target.getAttribute('data-category');
            currentPage = 0;
            currentKeyword = ''; // 카테고리 이동 시 검색어 초기화
            searchInput.value = '';
            fetchProducts();
        });
    });

    // 다이내믹 히어로 배너 데이터 로드 (할인율 1위 상품)
    async function loadTopDiscountBanner() {
        try {
            const res = await fetch('/api/products/top-discount');
            if (res.ok && res.status === 200) {
                const product = await res.json();
                if (product && product.id) {
                    const slide1 = document.querySelector('.slide-1 .slide-content');
                    if (slide1) {
                        slide1.innerHTML = `
                            <h2>🔥 오늘의 초특가: ${product.discountRate}% 할인</h2>
                            <p>${product.name}</p>
                            <a href="${product.affiliateUrl || product.productUrl}" target="_blank" style="display:inline-block; margin-top:10px; padding:8px 15px; background:white; color:#ff4757; text-decoration:none; border-radius:20px; font-weight:bold;">바로가기</a>
                        `;
                        // 배경 이미지도 상품 이미지로 변경 (선택사항, CSS에 맞춰 어둡게 처리 필요할 수 있음)
                        document.querySelector('.slide-1').style.backgroundImage = `linear-gradient(rgba(0,0,0,0.6), rgba(0,0,0,0.6)), url('${product.imageUrl}')`;
                        document.querySelector('.slide-1').style.backgroundSize = 'cover';
                        document.querySelector('.slide-1').style.backgroundPosition = 'center';
                    }
                }
            }
        } catch (e) {
            console.log('초특가 배너 로드 실패', e);
        }
    }

    // AI 요약 브리핑 로드
    async function loadAiSummary() {
        try {
            const res = await fetch('/api/ai/summary');
            if (res.ok && res.status === 200) {
                const data = await res.json();
                if (data && data.content) {
                    const box = document.getElementById('ai-summary-box');
                    const contentDiv = document.getElementById('ai-summary-content');
                    if (box && contentDiv) {
                        // AI가 작성한 HTML(혹은 마크다운)을 innerHTML로 삽입
                        contentDiv.innerHTML = data.content.replace(/\n/g, '<br>');
                        box.style.display = 'block';
                    }
                }
            }
        } catch (e) {
            console.log('AI 요약 로드 실패', e);
        }
    }

    // 실시간 랭킹 데이터 로드
    async function loadRanking() {
        try {
            const res = await fetch('/api/bookmarks/ranking');
            if (res.ok && res.status === 200) {
                const products = await res.json();
                if (products && products.length > 0) {
                    const box = document.getElementById('ranking-bar-box');
                    const listDiv = document.getElementById('ranking-list');
                    if (box && listDiv) {
                        listDiv.innerHTML = products.map((p, index) => {
                            let rankColor = index === 0 ? '#F59E0B' : (index === 1 ? '#94A3B8' : (index === 2 ? '#B45309' : '#64748B'));
                            return `
                                <a href="${p.affiliateUrl || p.productUrl || '#'}" target="_blank" class="ranking-item" style="display:flex; align-items:center; gap:10px; background:#fff; padding:8px 12px; border-radius:8px; border:1px solid #E2E8F0; text-decoration:none; color:inherit; min-width: 250px; flex: 0 0 auto; box-shadow: 0 2px 5px rgba(0,0,0,0.02);">
                                    <span style="font-weight:900; font-size:1.2rem; min-width:24px; text-align:center; color:${rankColor};">${index + 1}</span>
                                    <img src="${p.imageUrl}" style="width:40px; height:40px; object-fit:cover; border-radius:6px;">
                                    <div style="display:flex; flex-direction:column; overflow:hidden;">
                                        <span style="font-size:0.85rem; font-weight:600; white-space:nowrap; text-overflow:ellipsis; overflow:hidden;">${p.name}</span>
                                        <span style="font-size:0.9rem; font-weight:800; color:#EF4444;">${p.price.toLocaleString()}원</span>
                                    </div>
                                </a>
                            `;
                        }).join('');
                        box.style.display = 'block';
                    }
                }
            }
        } catch (e) {
            console.log('랭킹 데이터 로드 실패', e);
        }
    }

    // 초기 데이터 로드 전 찜 목록 선행 로드
    async function initData() {
        loadTopDiscountBanner(); // 배너 비동기 로드
        loadAiSummary(); // AI 브리핑 비동기 로드
        loadRanking(); // 랭킹 비동기 로드
        try {
            const userRes = await fetch('/api/users/me');
            if (userRes.ok) {
                const userData = await userRes.json();
                if (userData && userData.username) {
                    const bmRes = await fetch('/api/bookmarks/my');
                    if (bmRes.ok) {
                        const bmData = await bmRes.json();
                        if (Array.isArray(bmData)) {
                            bmData.forEach(p => window.myBookmarkedProductIds.add(p.id));
                        }
                    }
                }
            }
        } catch (e) {
            console.log('찜 목록 로드 실패', e);
        }
        fetchProducts();
    }
    initData();
});

// 찜하기(하트) 토글 글로벌 함수
window.toggleBookmark = function(productId, btnElement) {
    fetch(`/api/bookmarks/${productId}`, { method: 'POST' })
        .then(res => {
            if (res.status === 401) {
                alert('로그인이 필요합니다.');
                window.location.href = '/login.html';
                throw new Error('Not logged in');
            }
            return res.json();
        })
        .then(data => {
            if (data.status === 'added') {
                btnElement.innerHTML = '❤️';
                if (window.myBookmarkedProductIds) window.myBookmarkedProductIds.add(productId);
                alert('찜 목록에 추가되었습니다!');
            } else if (data.status === 'removed') {
                btnElement.innerHTML = '🤍';
                if (window.myBookmarkedProductIds) window.myBookmarkedProductIds.delete(productId);
            }
        })
        .catch(console.error);
};
