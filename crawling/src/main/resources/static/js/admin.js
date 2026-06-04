document.addEventListener('DOMContentLoaded', () => {
    // 테마 토글
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

    // 탭 전환 로직
    const tabBtns = document.querySelectorAll('.tab-btn');
    const panels = document.querySelectorAll('.dashboard-panel');
    
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(b => b.classList.remove('active'));
            panels.forEach(p => p.classList.remove('active'));
            
            btn.classList.add('active');
            document.getElementById(btn.dataset.target).classList.add('active');
        });
    });

    // 데이터 로딩 함수들
    loadStats();
    loadUsers();
    loadProducts();

    function loadStats() {
        fetch('/api/admin/stats')
            .then(res => res.json())
            .then(data => {
                document.getElementById('stat-users').innerText = data.users + '명';
                document.getElementById('stat-products').innerText = data.products + '개';
                document.getElementById('stat-bookmarks').innerText = data.bookmarks + '회';
            })
            .catch(console.error);
    }

    // ========== 회원 관리 ==========
    const userList = document.getElementById('users-tbody');
    function loadUsers() {
        fetch('/api/admin/users')
            .then(res => {
                if (!res.ok) {
                    if (res.status === 403 || res.status === 401) {
                        alert('관리자 권한이 없습니다.');
                        window.location.href = '/index.html';
                    }
                    throw new Error('데이터 로딩 실패');
                }
                return res.json();
            })
            .then(users => {
                if (users.length === 0) {
                    userList.innerHTML = '<tr><td colspan="7" style="text-align: center;">사용자가 없습니다.</td></tr>';
                    return;
                }

                userList.innerHTML = users.map((user, index) => {
                    const isRoleAdmin = user.roles && user.roles.includes('ROLE_ADMIN');
                    const badgeHtml = isRoleAdmin 
                        ? '<span class="role-badge role-admin">관리자</span>' 
                        : '<span class="role-badge role-user">일반 유저</span>';
                    
                    const selectHtml = `
                        <select class="role-select" onchange="changeRole(${user.id}, this.value)">
                            <option value="false" ${!isRoleAdmin ? 'selected' : ''}>일반 유저</option>
                            <option value="true" ${isRoleAdmin ? 'selected' : ''}>관리자</option>
                        </select>
                    `;

                    const date = new Date(user.createdAt);
                    const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;

                    return `
                        <tr>
                            <td>${index + 1}</td>
                            <td>${user.name || '-'} (${user.username})</td>
                            <td>${user.email || '-'}</td>
                            <td>${dateStr}</td>
                            <td>${badgeHtml}</td>
                            <td>${selectHtml}</td>
                            <td><button class="btn-danger" onclick="deleteUser(${user.id}, '${user.username}')">삭제</button></td>
                        </tr>
                    `;
                }).join('');
            }).catch(console.error);
    }

    window.changeRole = async function(userId, isAdminStr) {
        if (!confirm('권한을 변경하시겠습니까?')) return loadUsers();
        try {
            const res = await fetch(`/api/admin/users/${userId}/role`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ isAdmin: isAdminStr === 'true' })
            });
            if (res.ok) { alert('변경되었습니다.'); loadUsers(); } 
            else alert('실패: ' + await res.text());
        } catch (e) { alert('서버 오류'); }
    };

    window.deleteUser = async function(userId, username) {
        if (!confirm(`정말 '${username}' 유저를 삭제하시겠습니까?\n(삭제 시 관련된 찜 목록도 모두 사라집니다.)`)) return;
        try {
            const res = await fetch(`/api/admin/users/${userId}`, { method: 'DELETE' });
            if (res.ok) { alert('유저가 삭제되었습니다.'); loadUsers(); loadStats(); }
            else alert('삭제 실패: ' + await res.text());
        } catch (e) { alert('서버 오류'); }
    };

    // 유저 생성 모달
    const createUserModal = document.getElementById('create-user-modal');
    document.getElementById('open-create-user-modal').addEventListener('click', () => createUserModal.style.display = 'flex');
    document.getElementById('close-create-user-modal').addEventListener('click', () => createUserModal.style.display = 'none');
    window.addEventListener('click', (e) => { if (e.target === createUserModal) createUserModal.style.display = 'none'; });

    document.getElementById('create-user-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const data = Object.fromEntries(new FormData(e.target).entries());
        try {
            const res = await fetch('/api/admin/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            if (res.ok) {
                alert('생성되었습니다.'); createUserModal.style.display = 'none';
                e.target.reset(); loadUsers(); loadStats();
            } else alert('생성 실패: ' + await res.text());
        } catch (e) { alert('서버 오류'); }
    });

    // ========== 상품/크롤링 관리 ==========
    const productList = document.getElementById('products-tbody');
    const userProductList = document.getElementById('user-products-tbody');
    
    function loadProducts() {
        fetch('/api/admin/products')
            .then(res => res.json())
            .then(products => {
                console.log("Total products:", products.length);
                if (products.length > 0) {
                    console.log("First product source:", products[0].source);
                }
                const crawlerProducts = products.filter(p => !p.source || String(p.source).trim().toUpperCase() !== 'USER');
                const userProducts = products.filter(p => p.source && String(p.source).trim().toUpperCase() === 'USER');
                console.log("User products count:", userProducts.length);
                
                renderTable(productList, crawlerProducts, '수집된 상품이 없습니다.');
                renderTable(userProductList, userProducts, '유저가 제보한 상품이 없습니다.');
            }).catch(console.error);
    }
    
    function renderTable(tbody, products, emptyMsg) {
        if (products.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center;">${emptyMsg}</td></tr>`;
            return;
        }
        tbody.innerHTML = products.map(p => {
            const priceStr = p.price ? p.price.toLocaleString() + '원' : '가격 정보 없음';
            return `
                <tr>
                    <td><img src="${p.imageUrl}" class="product-img-thumb" alt="상품 이미지"></td>
                    <td style="max-width:300px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;" title="${p.name}">${p.name}</td>
                    <td>${p.category || '-'}</td>
                    <td><span style="color:#ff4757; font-weight:bold;">${p.discountRate || 0}%</span></td>
                    <td style="font-weight:bold;">${priceStr}</td>
                    <td><button class="btn-danger" onclick="deleteProduct(${p.id})">삭제</button></td>
                </tr>
            `;
        }).join('');
    }

    window.deleteProduct = async function(productId) {
        if (!confirm('해당 상품을 정말 삭제하시겠습니까? (유저들의 찜 목록에서도 사라집니다)')) return;
        try {
            const res = await fetch(`/api/admin/products/${productId}`, { method: 'DELETE' });
            if (res.ok) { alert('상품이 삭제되었습니다.'); loadProducts(); loadStats(); }
            else alert('삭제 실패: ' + await res.text());
        } catch (e) { alert('서버 오류'); }
    };

    // 크롤링 트리거
    document.getElementById('trigger-crawling-btn').addEventListener('click', async () => {
        if (!confirm('지금 즉시 크롤링 봇을 작동시켜 최신 상품을 가져오시겠습니까?\n(약간의 시간이 소요될 수 있습니다)')) return;
        try {
            alert('요청이 서버로 전송되었습니다. 백그라운드에서 크롤링이 진행됩니다!');
            const res = await fetch('/api/admin/crawling/trigger', { method: 'POST' });
            if (res.ok) {
                // 당장 반영되지 않더라도 새로고침해서 시각적 피드백 제공 (Playwright 완성 시 비동기로 처리됨)
                setTimeout(() => { loadProducts(); loadStats(); }, 2000);
            }
        } catch (e) { alert('서버 오류'); }
    });
});
