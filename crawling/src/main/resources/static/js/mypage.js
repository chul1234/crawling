document.addEventListener('DOMContentLoaded', () => {
    // 공통 기능: 다크 모드 및 로그아웃
    const themeToggleBtn = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;

    const savedTheme = localStorage.getItem('theme') || 'light';
    htmlElement.setAttribute('data-theme', savedTheme);
    updateToggleButtonText(savedTheme);

    themeToggleBtn.addEventListener('click', () => {
        const newTheme = htmlElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        htmlElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateToggleButtonText(newTheme);
    });

    function updateToggleButtonText(theme) {
        themeToggleBtn.innerHTML = theme === 'dark' ? '☀️ 라이트 모드' : '🌙 다크 모드';
    }

    document.getElementById('logout-btn').addEventListener('click', () => {
        if(confirm('로그아웃 하시겠습니까?')) {
            fetch('/api/logout', { method: 'POST' }).then(() => window.location.href = '/login.html');
        }
    });

    // --- 마이페이지 전용 로직 ---

    // 1-2. 프로필 정보 렌더링 함수
    function loadUserInfo() {
        fetch('/api/users/me')
            .then(res => {
                if (!res.ok) {
                    if (res.status === 401 || res.status === 403) {
                        alert('로그인이 필요합니다.');
                        window.location.href = '/login.html';
                    } else {
                        console.error('서버 오류: ', res.status);
                        alert('사용자 정보를 불러오는데 실패했습니다.');
                    }
                    throw new Error('Fetch failed');
                }
                return res.json();
            })
            .then(data => {
                // 화면 요약 정보 업데이트
                document.getElementById('info-username').textContent = data.username;
                document.getElementById('info-name').textContent = data.name || '-';
                document.getElementById('info-email').textContent = data.email || '-';
                const date = new Date(data.createdAt);
                document.getElementById('info-created').textContent = date.toLocaleDateString();

                // 모달 폼 초기값 세팅
                document.getElementById('name').value = data.name || '';
                document.getElementById('email').value = data.email || '';
            })
            .catch(console.error);
    }
    loadUserInfo();

    // 1-3. 모달 제어 로직
    const editModal = document.getElementById('edit-modal');
    const openModalBtn = document.getElementById('open-edit-modal-btn');
    const closeModalBtn = document.getElementById('close-modal-btn');

    openModalBtn.addEventListener('click', () => {
        editModal.style.display = 'flex';
    });

    closeModalBtn.addEventListener('click', () => {
        editModal.style.display = 'none';
        document.getElementById('currentPassword').value = '';
        document.getElementById('newPassword').value = '';
    });

    window.addEventListener('click', (e) => {
        if (e.target === editModal) {
            editModal.style.display = 'none';
            document.getElementById('currentPassword').value = '';
            document.getElementById('newPassword').value = '';
        }
    });

    // 2. 정보 수정 제출
    document.getElementById('profile-update-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        try {
            const response = await fetch('/api/users/me', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (response.ok) {
                alert('정보가 성공적으로 수정되었습니다.');
                editModal.style.display = 'none';
                document.getElementById('currentPassword').value = '';
                document.getElementById('newPassword').value = '';
                loadUserInfo(); // 요약 화면 갱신
            } else {
                const errorMsg = await response.text();
                alert('수정 실패: ' + errorMsg);
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    });

    // 3. 찜한 상품 목록 불러오기
    function loadBookmarks() {
        fetch('/api/bookmarks/my')
            .then(res => res.json())
            .then(products => {
                const grid = document.getElementById('bookmark-grid');
                if (!products || products.length === 0) {
                    grid.innerHTML = '<div class="empty-msg">아직 찜한 상품이 없습니다.<br>메인 화면에서 마음에 드는 핫딜에 ❤️를 눌러보세요!</div>';
                    return;
                }

                grid.innerHTML = products.map(product => {
                    const discountRatio = product.discountRate || 0;
                    return `
                        <article class="product-card" style="margin-bottom:0;">
                            <div class="image-wrapper" style="height:150px;">
                                <img src="${product.imageUrl || 'https://via.placeholder.com/600x400?text=No+Image'}" alt="${product.name}">
                            </div>
                            <div class="product-info" style="padding:15px;">
                                <span class="category-text">${product.category || '기타'}</span>
                                <h3 class="product-name" style="font-size:1rem;">${product.name}</h3>
                                <div class="price-container">
                                    <div class="price-row">
                                        <span class="discount-rate">${discountRatio}%</span>
                                        <span class="current-price">${product.price ? product.price.toLocaleString() : 0}<span class="unit">원</span></span>
                                    </div>
                                    ${product.originalPrice ? `<span class="original-price">${product.originalPrice.toLocaleString()}원</span>` : ''}
                                </div>
                                <button class="login-submit-btn" style="margin-top:10px; padding:8px; font-size:0.9rem;" onclick="removeBookmark(${product.id})">❌ 찜 해제</button>
                            </div>
                        </article>
                    `;
                }).join('');
            })
            .catch(console.error);
    }

    loadBookmarks();

    // 찜 해제 함수 (전역 노출 필요)
    window.removeBookmark = function(productId) {
        if(confirm('찜 목록에서 삭제하시겠습니까?')) {
            fetch(`/api/bookmarks/${productId}`, { method: 'POST' })
                .then(res => res.json())
                .then(data => {
                    if (data.status === 'removed') {
                        loadBookmarks(); // 목록 새로고침
                    }
                });
        }
    };
});
