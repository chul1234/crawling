document.addEventListener('DOMContentLoaded', () => {
    // 테마 토글 관련 로직 (공통)
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

    document.getElementById('logout-btn').addEventListener('click', () => {
        if(confirm('로그아웃 하시겠습니까?')) {
            fetch('/api/logout', { method: 'POST' }).then(() => window.location.href = '/login.html');
        }
    });

    function updateToggleButtonText(theme) {
        themeToggleBtn.innerHTML = theme === 'dark' ? '☀️ 라이트 모드' : '🌙 다크 모드';
    }

    const userList = document.getElementById('users-tbody');

    // 1. 관리자 유저 정보 불러오기
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
                    userList.innerHTML = '<tr><td colspan="6" style="text-align: center;">사용자가 없습니다.</td></tr>';
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
                        </tr>
                    `;
                }).join('');
            })
            .catch(err => {
                console.error(err);
                userList.innerHTML = '<tr><td colspan="6" style="text-align: center; color: red;">오류가 발생했습니다.</td></tr>';
            });
    }

    loadUsers();

    // 글로벌 권한 변경 함수
    window.changeRole = async function(userId, isAdminStr) {
        if (!confirm('해당 유저의 권한을 변경하시겠습니까?')) {
            loadUsers(); // 원래 상태로 되돌리기
            return;
        }

        try {
            const res = await fetch(`/api/admin/users/${userId}/role`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ isAdmin: isAdminStr === 'true' })
            });

            if (res.ok) {
                alert('권한이 변경되었습니다.');
                loadUsers();
            } else {
                alert('권한 변경 실패: ' + await res.text());
                loadUsers();
            }
        } catch (e) {
            alert('서버 오류');
            loadUsers();
        }
    };

    // 2. 새 유저 생성 모달 제어 로직
    const createUserModal = document.getElementById('create-user-modal');
    const openCreateBtn = document.getElementById('open-create-user-modal');
    const closeCreateBtn = document.getElementById('close-create-user-modal');

    openCreateBtn.addEventListener('click', () => createUserModal.style.display = 'flex');
    closeCreateBtn.addEventListener('click', () => createUserModal.style.display = 'none');
    window.addEventListener('click', (e) => {
        if (e.target === createUserModal) createUserModal.style.display = 'none';
    });

    // 3. 새 유저 폼 제출 로직
    document.getElementById('create-user-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        try {
            const res = await fetch('/api/admin/users', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (res.ok) {
                alert('유저가 성공적으로 생성되었습니다.');
                createUserModal.style.display = 'none';
                e.target.reset(); // 폼 초기화
                loadUsers(); // 목록 새로고침
            } else {
                alert('유저 생성 실패: ' + await res.text());
            }
        } catch (e) {
            alert('서버 오류 발생');
        }
    });
});
