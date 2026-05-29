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

    // 버튼 텍스트 업데이트 함수
    function updateToggleButtonText(theme) {
        if (theme === 'dark') {
            themeToggleBtn.innerHTML = '☀️ 라이트 모드';
        } else {
            themeToggleBtn.innerHTML = '🌙 다크 모드';
        }
    }

    // TODO: 백엔드 API(/api/products) 호출 및 데이터 렌더링 로직 추가
});
