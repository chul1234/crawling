window.originalAlert = window.alert;

window.alert = function(message) {
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        document.body.appendChild(toastContainer);
    }
    
    const toast = document.createElement('div');
    toast.className = 'custom-toast';
    
    // 메시지 내용에 따라 성공(초록) 또는 실패(빨강) 스타일 적용
    if (message.includes('실패') || message.includes('오류') || message.includes('없습니다')) {
        toast.classList.add('error');
        toast.innerHTML = `<span>⚠️ ${message}</span>`;
    } else {
        toast.classList.add('success');
        toast.innerHTML = `<span>✅ ${message}</span>`;
    }
    
    toastContainer.appendChild(toast);
    
    // 애니메이션으로 부드럽게 등장
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            toast.classList.add('show');
        });
    });
    
    // 3초 뒤에 사라짐
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300); // CSS transition 시간 후 제거
    }, 3000);
};
