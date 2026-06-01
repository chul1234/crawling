document.addEventListener('DOMContentLoaded', () => {
    // 테마 토글 관련 로직 (index.js와 동일)
    const themeToggleBtn = document.getElementById('theme-toggle');
    const htmlElement = document.documentElement;

    const savedTheme = localStorage.getItem('theme') || 'light';
    htmlElement.setAttribute('data-theme', savedTheme);
    updateToggleButtonText(savedTheme);

    themeToggleBtn.addEventListener('click', () => {
        const currentTheme = htmlElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        htmlElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);
        updateToggleButtonText(newTheme);
    });

    function updateToggleButtonText(theme) {
        if (theme === 'dark') {
            themeToggleBtn.innerHTML = '☀️ 라이트 모드';
        } else {
            themeToggleBtn.innerHTML = '🌙 다크 모드';
        }
    }

    const userList = document.getElementById('user-list');

    // 관리자 유저 정보 불러오기
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

            userList.innerHTML = users.map(user => {
                const isRoleAdmin = user.roles && user.roles.includes('ROLE_ADMIN');
                const badgeHtml = isRoleAdmin 
                    ? '<span class="badge-admin">관리자</span>' 
                    : '<span class="badge-user">일반 유저</span>';
                
                // 가입일 포맷팅
                const date = new Date(user.createdAt);
                const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;

                return `
                    <tr>
                        <td>${user.id}</td>
                        <td>${user.username}</td>
                        <td>${user.name}</td>
                        <td>${user.email}</td>
                        <td>${badgeHtml}</td>
                        <td>${dateStr}</td>
                    </tr>
                `;
            }).join('');
        })
        .catch(err => {
            console.error(err);
            userList.innerHTML = '<tr><td colspan="6" style="text-align: center; color: red;">오류가 발생했습니다.</td></tr>';
        });
});
