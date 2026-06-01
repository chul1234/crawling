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

    // 현재 사용자 권한 확인 후 관리자 탭 노출
    fetch('/api/users/me')
        .then(res => res.json())
        .then(data => {
            if (data && data.roles && data.roles.includes('ROLE_ADMIN')) {
                document.getElementById('admin-tab').style.display = 'inline-block';
            }
        })
        .catch(err => console.log('사용자 정보 로드 실패 (아마 비로그인 상태이거나 오류)', err));

    // API 호출 및 데이터 렌더링 관련 상태 변수
    let currentCategory = 'all';
    let currentPage = 0;
    const pageSize = 12;

    const productGrid = document.getElementById('product-grid');
    const paginationDiv = document.getElementById('pagination');
    const categoryButtons = document.querySelectorAll('#category-filter button');

    // 상품 데이터 불러오기 함수
    async function fetchProducts() {
        try {
            const response = await fetch(`/api/products?category=${encodeURIComponent(currentCategory)}&page=${currentPage}&size=${pageSize}`);
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
            
            // 할인율이 50 이상이면 초특가 뱃지
            let badge = '';
            let highlightClass = '';
            if (!p.isSoldOut) {
                if (p.discountRate >= 50) {
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
                </div>
                <div class="product-info">
                    <span class="category-text">${p.category || '기타'}</span>
                    <h3 class="product-name"><a href="${p.productUrl || '#'}" target="_blank" style="text-decoration:none; color:inherit;">${p.name}</a></h3>
                    <div class="price-container">
                        <div class="price-row">
                            ${discountText ? `<span class="${discountClass}">${discountText}</span>` : ''}
                            <span class="current-price">${p.price ? p.price.toLocaleString() : 0}<span class="unit">원</span></span>
                        </div>
                        ${p.originalPrice ? `<span class="original-price">${p.originalPrice.toLocaleString()}원</span>` : ''}
                    </div>
                    <div class="card-footer">
                        <span class="review-count">⭐ 리뷰 ${p.reviewCount ? p.reviewCount.toLocaleString() : 0}</span>
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
            currentPage = 0; // 카테고리 변경 시 1페이지로 초기화
            fetchProducts();
        });
    });

    // 초기 데이터 로드
    fetchProducts();
});
