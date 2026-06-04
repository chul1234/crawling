document.addEventListener('DOMContentLoaded', () => {
    // 테마 및 로그아웃 버튼 (공통)
    const themeToggle = document.getElementById('theme-toggle');
    if (localStorage.getItem('theme') === 'dark') {
        document.body.classList.add('dark-mode');
        if (themeToggle) themeToggle.textContent = '☀️ 라이트 모드';
    }

    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            document.body.classList.toggle('dark-mode');
            const isDark = document.body.classList.contains('dark-mode');
            localStorage.setItem('theme', isDark ? 'dark' : 'light');
            themeToggle.textContent = isDark ? '☀️ 라이트 모드' : '🌙 다크 모드';
        });
    }

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            await fetch('/api/logout', { method: 'POST' });
            window.location.href = '/login.html';
        });
    }

    // URL 파라미터 파싱 및 폼 자동 채우기
    const urlParams = new URLSearchParams(window.location.search);
    
    const nameInput = document.getElementById('name');
    const priceInput = document.getElementById('price');
    const originalPriceInput = document.getElementById('originalPrice');
    const imageUrlInput = document.getElementById('imageUrl');
    const productUrlInput = document.getElementById('productUrl');
    
    if (urlParams.has('name')) nameInput.value = urlParams.get('name');
    if (urlParams.has('price')) priceInput.value = urlParams.get('price');
    if (urlParams.has('originalPrice')) originalPriceInput.value = urlParams.get('originalPrice');
    if (urlParams.has('imageUrl')) imageUrlInput.value = urlParams.get('imageUrl');
    if (urlParams.has('productUrl')) productUrlInput.value = urlParams.get('productUrl');

    // 이미지 미리보기 업데이트
    const updateImagePreview = () => {
        const previewContainer = document.getElementById('image-preview-container');
        const previewImg = document.getElementById('image-preview-img');
        if (imageUrlInput.value) {
            previewImg.src = imageUrlInput.value;
            previewContainer.style.display = 'flex';
        } else {
            previewContainer.style.display = 'none';
        }
    };

    updateImagePreview();
    imageUrlInput.addEventListener('input', updateImagePreview);

    // 폼 제출
    const form = document.getElementById('contribute-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = document.getElementById('submit-btn');
        submitBtn.disabled = true;
        submitBtn.textContent = '등록 중...';

        const payload = {
            name: nameInput.value,
            price: parseInt(priceInput.value),
            originalPrice: originalPriceInput.value ? parseInt(originalPriceInput.value) : null,
            imageUrl: imageUrlInput.value,
            productUrl: productUrlInput.value,
            category: document.getElementById('category').value
        };
        
        // 할인율 계산
        if (payload.originalPrice && payload.originalPrice > payload.price) {
            payload.discountRate = Math.round(((payload.originalPrice - payload.price) / payload.originalPrice) * 100);
        } else {
            payload.discountRate = 0;
        }

        try {
            const res = await fetch('/api/products/contribute', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (res.ok) {
                alert('성공적으로 등록되었습니다!');
                setTimeout(() => {
                    window.location.href = '/index.html';
                }, 1500);
            } else if (res.status === 401 || res.status === 403) {
                alert('로그인이 필요합니다.');
                setTimeout(() => { window.location.href = '/login.html'; }, 1500);
            } else {
                alert('등록에 실패했습니다.');
                submitBtn.disabled = false;
                submitBtn.textContent = '최종 등록하기';
            }
        } catch (err) {
            alert('서버 통신 오류');
            submitBtn.disabled = false;
            submitBtn.textContent = '최종 등록하기';
        }
    });
});
