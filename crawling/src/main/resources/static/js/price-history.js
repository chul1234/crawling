document.addEventListener('DOMContentLoaded', async () => {
    console.log("가격 변동 추이 페이지 로드 완료");
    
    const urlParams = new URLSearchParams(window.location.search);
    const productId = urlParams.get('id');
    
    if (!productId) {
        if (typeof showToast === 'function') {
            showToast("잘못된 접근입니다. 상품 정보가 없습니다.", "error");
        } else {
            alert("잘못된 접근입니다. 상품 정보가 없습니다.");
        }
        setTimeout(() => { window.location.href = '/index.html'; }, 1500);
        return;
    }

    try {
        // 1. 상품 정보 로드
        const productRes = await fetch(`/api/products/${productId}`);
        if (!productRes.ok) throw new Error("상품 정보를 불러오지 못했습니다.");
        const product = await productRes.json();
        
        renderProductSummary(product);
        
        // 2. 가격 변동 내역 로드
        const historyRes = await fetch(`/api/price-history/${productId}`);
        if (!historyRes.ok) throw new Error("가격 변동 내역을 불러오지 못했습니다.");
        const historyList = await historyRes.json();
        
        renderPriceHistory(historyList);
        renderPriceChart(historyList);
        
    } catch (error) {
        console.error(error);
        if (typeof showToast === 'function') {
            showToast(error.message, "error");
        } else {
            alert(error.message);
        }
    }
});

function renderProductSummary(product) {
    const summaryContainer = document.getElementById('productSummary');
    const priceStr = product.price ? product.price.toLocaleString() + '원' : '가격 정보 없음';
    
    summaryContainer.innerHTML = `
        <img src="${product.imageUrl}" alt="${product.name}">
        <div class="summary-info">
            <h2>${product.name}</h2>
            <div class="current-price">현재가: ${priceStr}</div>
        </div>
    `;
}

function renderPriceHistory(historyList) {
    const listContainer = document.getElementById('priceList');
    
    if (!historyList || historyList.length === 0) {
        listContainer.innerHTML = '<li style="justify-content: center; color: #888;">수집된 가격 변동 내역이 없습니다.</li>';
        return;
    }
    
    listContainer.innerHTML = '';
    
    // 리스트는 최신순(내림차순)으로 API에서 전달됨
    for (let i = 0; i < historyList.length; i++) {
        const history = historyList[i];
        
        // 날짜 포맷 (예: 10월 25일 14:00)
        const dateObj = new Date(history.createdAt);
        const dateStr = `${dateObj.getMonth() + 1}월 ${dateObj.getDate()}일 ${String(dateObj.getHours()).padStart(2, '0')}:${String(dateObj.getMinutes()).padStart(2, '0')}`;
        
        let trendHtml = '';
        const priceFormatted = history.price.toLocaleString() + '원';
        
        if (i === historyList.length - 1) {
            // 가장 처음 수집된 데이터 (맨 마지막 인덱스)
            trendHtml = `<span class="trend same">- 최초수집</span>`;
        } else {
            // 과거(이전 인덱스 i+1)와 비교
            const previousHistory = historyList[i + 1];
            const diff = history.price - previousHistory.price;
            
            if (diff > 0) {
                trendHtml = `<span class="trend up">▲ ${diff.toLocaleString()}원</span>`;
            } else if (diff < 0) {
                trendHtml = `<span class="trend down">▼ ${Math.abs(diff).toLocaleString()}원</span>`;
            } else {
                trendHtml = `<span class="trend same">- 변동없음</span>`;
            }
        }
        
        const li = document.createElement('li');
        li.innerHTML = `
            <span class="date">${dateStr}</span>
            <div class="price-box">
                <span class="price-value">${priceFormatted}</span>
                ${trendHtml}
            </div>
        `;
        listContainer.appendChild(li);
    }
}

function renderPriceChart(historyList) {
    const ctx = document.getElementById('priceChart');
    if (!ctx) return;

    if (!historyList || historyList.length === 0) {
        ctx.parentElement.innerHTML = '<div style="text-align:center; padding:50px; color:#888;">차트 데이터가 없습니다.</div>';
        return;
    }

    // 시간 역순(과거 -> 최신)으로 뒤집기 (차트는 왼쪽에서 오른쪽으로 흐르도록)
    const ascendingList = [...historyList].reverse();

    const labels = ascendingList.map(h => {
        const dateObj = new Date(h.createdAt);
        return `${dateObj.getMonth() + 1}/${dateObj.getDate()} ${String(dateObj.getHours()).padStart(2, '0')}:${String(dateObj.getMinutes()).padStart(2, '0')}`;
    });

    const dataPoints = ascendingList.map(h => h.price);

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: '상품 가격',
                data: dataPoints,
                borderColor: '#FF4757',
                backgroundColor: 'rgba(255, 71, 87, 0.15)',
                borderWidth: 3,
                pointBackgroundColor: '#FF4757',
                pointBorderColor: '#fff',
                pointRadius: 5,
                pointHoverRadius: 7,
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(30, 41, 59, 0.9)',
                    titleFont: { size: 13, family: 'Pretendard' },
                    bodyFont: { size: 14, weight: 'bold', family: 'Pretendard' },
                    padding: 12,
                    callbacks: {
                        label: function(context) {
                            return ' ' + context.parsed.y.toLocaleString() + '원';
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false }
                },
                y: {
                    beginAtZero: false,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)',
                        borderDash: [5, 5]
                    },
                    ticks: {
                        callback: function(value) {
                            return value.toLocaleString() + '원';
                        }
                    }
                }
            }
        }
    });
}
