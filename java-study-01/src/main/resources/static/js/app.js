/**
 * 청담명리 (淸潭命理) 프론트엔드 컨트롤러 & 렌더러
 */

let currentGender = '남';
let currentCalendar = 'solar';
let sajuData = null;

document.addEventListener('DOMContentLoaded', () => {
    initDateSelectors();
    initControls();
});

// 1. 날짜 및 시간 셀렉터 초기화
function initDateSelectors() {
    const yearSelect = document.getElementById('birthYear');
    const monthSelect = document.getElementById('birthMonth');
    const daySelect = document.getElementById('birthDay');
    const hourSelect = document.getElementById('customHour');
    const minSelect = document.getElementById('customMinute');

    const currentYear = 2026;
    for (let y = currentYear; y >= 1930; y--) {
        const opt = document.createElement('option');
        opt.value = y;
        opt.textContent = y;
        if (y === 1995) opt.selected = true;
        yearSelect.appendChild(opt);
    }

    for (let m = 1; m <= 12; m++) {
        const opt = document.createElement('option');
        opt.value = m;
        opt.textContent = m < 10 ? '0' + m : m;
        if (m === 5) opt.selected = true;
        monthSelect.appendChild(opt);
    }

    function updateDays() {
        const y = parseInt(yearSelect.value);
        const m = parseInt(monthSelect.value);
        const daysInMonth = new Date(y, m, 0).getDate();
        const curDay = parseInt(daySelect.value) || 15;

        daySelect.innerHTML = '';
        for (let d = 1; d <= daysInMonth; d++) {
            const opt = document.createElement('option');
            opt.value = d;
            opt.textContent = d < 10 ? '0' + d : d;
            if (d === Math.min(curDay, daysInMonth)) opt.selected = true;
            daySelect.appendChild(opt);
        }
    }

    yearSelect.addEventListener('change', updateDays);
    monthSelect.addEventListener('change', updateDays);
    updateDays();

    // 시간/분 셀렉터
    for (let h = 0; h < 24; h++) {
        const opt = document.createElement('option');
        opt.value = h;
        opt.textContent = (h < 10 ? '0' + h : h) + '시';
        if (h === 12) opt.selected = true;
        hourSelect.appendChild(opt);
    }

    for (let mi = 0; mi < 60; mi += 5) {
        const opt = document.createElement('option');
        opt.value = mi;
        opt.textContent = (mi < 10 ? '0' + mi : mi) + '분';
        if (mi === 30) opt.selected = true;
        minSelect.appendChild(opt);
    }
}

// 2. 젠더, 역법 컨트롤 초기화
function initControls() {
    // 성별
    document.querySelectorAll('#genderSelect .segment-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#genderSelect .segment-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentGender = btn.getAttribute('data-value');
        });
    });

    // 역법 (양력/음력)
    document.querySelectorAll('#calendarSelect .segment-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#calendarSelect .segment-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            currentCalendar = btn.getAttribute('data-value');
        });
    });
}

// 시간 모름 체크박스 토글
function toggleTimeInput() {
    const isUnknown = document.getElementById('timeUnknown').checked;
    const timeInputs = document.getElementById('timeInputs');
    if (isUnknown) {
        timeInputs.style.opacity = '0.35';
        timeInputs.style.pointerEvents = 'none';
    } else {
        timeInputs.style.opacity = '1';
        timeInputs.style.pointerEvents = 'auto';
    }
}

// 시진 선택 변경 시 직접 입력창 토글
function onSijinChange() {
    const sijinVal = document.getElementById('sijinSelect').value;
    const customBox = document.getElementById('customTimeBox');
    if (sijinVal === 'custom') {
        customBox.style.display = 'grid';
    } else {
        customBox.style.display = 'none';
    }
}

// 3. 사주 요청 제출
async function submitSaju() {
    const name = document.getElementById('userName').value.trim() || '인연님';
    const year = parseInt(document.getElementById('birthYear').value);
    const month = parseInt(document.getElementById('birthMonth').value);
    const day = parseInt(document.getElementById('birthDay').value);
    const isUnknown = document.getElementById('timeUnknown').checked;
    const isLunar = (currentCalendar === 'lunar');

    let hour = null;
    let minute = 0;

    if (!isUnknown) {
        const sijinVal = document.getElementById('sijinSelect').value;
        if (sijinVal === 'custom') {
            hour = parseInt(document.getElementById('customHour').value);
            minute = parseInt(document.getElementById('customMinute').value);
        } else {
            const parts = sijinVal.split(':');
            hour = parseInt(parts[0]);
            minute = parseInt(parts[1]);
        }
    }

    const payload = {
        name: name,
        gender: currentGender,
        year: year,
        month: month,
        day: day,
        hour: hour,
        minute: minute,
        isLunar: isLunar
    };

    // UI 애니메이션 전환
    document.getElementById('inputSection').style.display = 'none';
    const loading = document.getElementById('loadingBox');
    loading.style.display = 'block';

    try {
        let result = null;
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 800);
            const res = await fetch('/api/saju', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                signal: controller.signal
            });
            clearTimeout(timeoutId);
            if (res.ok) {
                result = await res.json();
            }
        } catch (netErr) {
            console.warn('Backend API connection timed out or failed, switching to instant embedded engine.', netErr);
        }

        // 백엔드 지연 시 즉시 클라이언트 엔진으로 0.01초 만에 풀이
        if (!result) {
            result = runClientFallbackEngine(payload);
        }

        sajuData = result;

        // 지체 없이 즉시 화면 표시
        loading.style.display = 'none';
        renderResults(result);
        document.getElementById('resultSection').style.display = 'block';
        window.scrollTo({ top: 0, behavior: 'smooth' });

    } catch (e) {
        console.error('Error calculating saju:', e);
        loading.style.display = 'none';
        document.getElementById('inputSection').style.display = 'block';
        showToast('명식 풀이 중 문제가 발생했습니다. 다시 시도해 주세요.');
    }
}

// 4. 결과 화면 렌더링
function renderResults(data) {
    // 0. Banner Meta
    document.getElementById('resUserTitle').textContent = `${data.name} 님의 명식서`;
    document.getElementById('resBirthText').textContent = `${data.birthDate} · ${data.birthTime} 출생 (${data.gender})`;

    // 1. 사주 원국 (년, 월, 일, 시 4기둥)
    const container = document.getElementById('pillarsContainer');
    container.innerHTML = '';

    const pillarItems = [
        { label: '시주 (時柱)', sub: '말년·자녀운', data: data.timePillar },
        { label: '일주 (日柱)', sub: '본인·배우자운', data: data.dayPillar },
        { label: '월주 (月柱)', sub: '청년·사회운', data: data.monthPillar },
        { label: '년주 (年柱)', sub: '초년·조상운', data: data.yearPillar }
    ];

    pillarItems.forEach(item => {
        const col = document.createElement('div');
        col.className = 'pillar-col';

        if (!item.data) {
            col.innerHTML = `
                <div class="pillar-name">${item.label}</div>
                <div class="pillar-block" style="padding: 24px 4px; color: var(--text-muted); font-size: 0.85rem;">
                    미상(未詳)<br><span style="font-size: 0.72rem;">(시간 모름)</span>
                </div>
                <div style="font-size: 0.72rem; color: var(--text-muted);">${item.sub}</div>
            `;
        } else {
            const p = item.data;
            col.innerHTML = `
                <div class="pillar-name">${item.label}</div>
                <!-- 천간 -->
                <div class="pillar-block">
                    <div class="pillar-char" style="color: ${p.cheonganColor}">${p.cheonganHanja}</div>
                    <div class="pillar-sub">${p.cheongan}</div>
                    <span class="element-tag" style="background: ${p.cheonganColor}">${p.cheonganElement}</span>
                </div>
                <!-- 지지 -->
                <div class="pillar-block">
                    <div class="pillar-char" style="color: ${p.jijiColor}">${p.jijiHanja}</div>
                    <div class="pillar-sub">${p.jiji} (${p.jijiAnimal})</div>
                    <span class="element-tag" style="background: ${p.jijiColor}">${p.jijiElement}</span>
                </div>
                <div style="font-size: 0.72rem; color: var(--text-muted); margin-top: 6px;">${item.sub}</div>
            `;
        }
        container.appendChild(col);
    });

    // 2. 오행 분석 (수, 목 등등 뭐가 많은지)
    const elementBarsBox = document.getElementById('elementBarsContainer');
    elementBarsBox.innerHTML = '';

    const elementMap = [
        { key: '목', name: '목(木)', color: 'var(--color-wood)', meaning: '추진력, 성장' },
        { key: '화', name: '화(火)', color: 'var(--color-fire)', meaning: '열정, 표현' },
        { key: '토', name: '토(土)', color: 'var(--color-earth)', meaning: '신뢰, 포용' },
        { key: '금', name: '금(金)', color: 'var(--color-metal)', meaning: '결단, 절제' },
        { key: '수', name: '수(水)', color: 'var(--color-water)', meaning: '지혜, 통찰' }
    ];

    elementMap.forEach(el => {
        const count = data.elementCounts[el.key] || 0;
        const pct = data.elementPercentages[el.key] || 0;

        const card = document.createElement('div');
        card.className = 'el-bar-card';
        card.innerHTML = `
            <div class="el-header">
                <span style="color: ${el.color}">${el.name}</span>
                <span class="el-count">${count}개</span>
            </div>
            <div class="el-meter-track">
                <div class="el-meter-fill" style="background: ${el.color}; width: ${Math.min(100, pct)}%;"></div>
            </div>
            <div class="el-percent">${pct}%</div>
        `;
        elementBarsBox.appendChild(card);
    });

    document.getElementById('dominantTitle').textContent = `【${data.dominantElement}(${getHanjaElement(data.dominantElement)})】의 기운이 가장 왕성합니다`;
    document.getElementById('dominantContent').textContent = data.dominantDesc;

    document.getElementById('deficientTitle').textContent = `【${data.deficientElement}(${getHanjaElement(data.deficientElement)})】의 기운을 보강해야 합니다`;
    document.getElementById('deficientContent').textContent = data.deficientDesc;

    // 3. 대운 주기 & 언제까지 대운인가
    document.getElementById('daeunHeadline').textContent = `현재 대운: ${data.currentDaeunKanJi} 대운 (${data.currentDaeunStartAge}세 ~ ${data.currentDaeunEndAge}세)`;
    document.getElementById('daeunSummaryText').innerHTML = data.daeunSummary;

    const timeline = document.getElementById('daeunTimeline');
    timeline.innerHTML = '';
    data.daeunList.forEach(d => {
        const pCard = document.createElement('div');
        pCard.className = `daeun-period-card ${d.isCurrent ? 'current' : ''}`;
        pCard.innerHTML = `
            <div class="daeun-age">${d.startAge} ~ ${d.endAge}세</div>
            <div class="daeun-kanji">${d.kanji.split('(')[0]}</div>
            <div class="daeun-element">${d.element}</div>
            <div class="daeun-theme">${d.theme}</div>
        `;
        timeline.appendChild(pCard);
    });

    // 4. 조심해야 할 점 (주의사항)
    const cautionBox = document.getElementById('cautionList');
    cautionBox.innerHTML = '';
    data.cautions.forEach(c => {
        const item = document.createElement('div');
        item.className = 'caution-item';
        item.textContent = c;
        cautionBox.appendChild(item);
    });

    // 5. 오늘의 운세 & 2026년 운세 데이터 바인딩
    // [오늘의 운세]
    const tf = data.todayFortune;
    document.getElementById('todayScore').textContent = tf.totalScore;
    document.getElementById('todayDateStr').textContent = tf.date + ' · ' + tf.dayKanJi;
    document.getElementById('todayKeyword').textContent = tf.keyword;
    document.getElementById('todayCommentary').textContent = tf.commentary;

    document.getElementById('todayWealthBar').style.width = tf.wealthScore + '%';
    document.getElementById('todayWealthVal').textContent = tf.wealthScore + '점';
    document.getElementById('todayLoveBar').style.width = tf.loveScore + '%';
    document.getElementById('todayLoveVal').textContent = tf.loveScore + '점';
    document.getElementById('todayCareerBar').style.width = tf.careerScore + '%';
    document.getElementById('todayCareerVal').textContent = tf.careerScore + '점';
    document.getElementById('todayHealthBar').style.width = tf.healthScore + '%';
    document.getElementById('todayHealthVal').textContent = tf.healthScore + '점';

    document.getElementById('todayColor').textContent = tf.luckyColor;
    document.getElementById('todayNumber').textContent = tf.luckyNumber;
    document.getElementById('todayDirection').textContent = tf.luckyDirection;
    document.getElementById('todayTime').textContent = tf.luckyTime;

    // [2026년 운세]
    const yf = data.yearFortune;
    document.getElementById('yearScore').textContent = yf.overallScore;
    document.getElementById('yearTitleBadge').textContent = `${yf.targetYear}년 ${yf.yearKanJi}`;
    document.getElementById('yearHeadline').textContent = yf.headline;
    document.getElementById('yearOverview').textContent = yf.overview;
    document.getElementById('yearWealth').textContent = yf.wealth;
    document.getElementById('yearLove').textContent = yf.love;
    document.getElementById('yearCareer').textContent = yf.career;
    document.getElementById('yearHealth').textContent = yf.health;

    const quarterContainer = document.getElementById('quarterContainer');
    quarterContainer.innerHTML = '';
    yf.quarters.forEach(q => {
        const qCard = document.createElement('div');
        qCard.className = 'quarter-card';
        qCard.innerHTML = `
            <span class="q-badge">${q.quarter}</span>
            <h5>${q.title}</h5>
            <p>${q.desc}</p>
        `;
        quarterContainer.appendChild(qCard);
    });

    const warningBox = document.getElementById('warningMonthsList');
    warningBox.innerHTML = '';
    yf.warningMonths.forEach(wm => {
        const li = document.createElement('li');
        li.textContent = wm;
        warningBox.appendChild(li);
    });
}

function getHanjaElement(el) {
    switch (el) {
        case '목': return '木';
        case '화': return '火';
        case '토': return '土';
        case '금': return '金';
        case '수': return '水';
        default: return '';
    }
}

// 5. 탭 전환 (오늘의 운세 vs 이번년도 전체 운)
function switchFortuneTab(tab) {
    const tabToday = document.getElementById('tabToday');
    const tabYear = document.getElementById('tabYear');
    const panelToday = document.getElementById('panelToday');
    const panelYear = document.getElementById('panelYear');

    if (tab === 'today') {
        tabToday.classList.add('active');
        tabYear.classList.remove('active');
        panelToday.style.display = 'block';
        panelYear.style.display = 'none';
    } else {
        tabYear.classList.add('active');
        tabToday.classList.remove('active');
        panelYear.style.display = 'block';
        panelToday.style.display = 'none';
    }
}

// 6. 폼 초기화 (다시 감정하기)
function resetForm() {
    document.getElementById('resultSection').style.display = 'none';
    document.getElementById('inputSection').style.display = 'block';
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 7. 결과 공유 / 클립보드 복사
function shareResult() {
    if (!sajuData) return;
    const shareText = `[청담명리] ${sajuData.name} 님의 사주 감정 결과\n• 가장 왕성한 기운: ${sajuData.dominantElement}(木·火·土·金·水)\n• 대운 흐름: ${sajuData.currentDaeunKanJi} 대운 (~${sajuData.currentDaeunEndAge}세까지)\n• 2026년 총평: ${sajuData.yearFortune.headline}\n확인하기: ${window.location.href}`;

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(shareText).then(() => {
            showToast('사주 명식 풀이가 클립보드에 복사되었습니다.');
        }).catch(() => {
            showToast('복사에 실패했습니다.');
        });
    } else {
        showToast('클립보드 기능을 지원하지 않는 브라우저입니다.');
    }
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 2800);
}

// 8. 클라이언트 단독 구동을 위한 내장 만세력 Fallback 엔진
function runClientFallbackEngine(payload) {
    const CHEONGAN = ["갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"];
    const CHEONGAN_HANJA = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"];
    const CHEONGAN_ELEMENT = ["목", "목", "화", "화", "토", "토", "금", "금", "수", "수"];
    const CHEONGAN_COLOR = ["#2e7d32", "#4caf50", "#d32f2f", "#f44336", "#c77700", "#e6a100", "#cfd8dc", "#90a4ae", "#1565c0", "#1e88e5"];

    const JIJI = ["자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"];
    const JIJI_HANJA = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"];
    const JIJI_ELEMENT = ["수", "토", "목", "목", "토", "화", "화", "토", "금", "금", "토", "수"];
    const JIJI_COLOR = ["#1565c0", "#c77700", "#2e7d32", "#4caf50", "#c77700", "#d32f2f", "#f44336", "#c77700", "#cfd8dc", "#90a4ae", "#c77700", "#1565c0"];
    const JIJI_ANIMAL = ["쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양", "원숭이", "닭", "개", "돼지"];

    function createPillar(gIdx, jIdx) {
        return {
            cheongan: CHEONGAN[gIdx],
            cheonganHanja: CHEONGAN_HANJA[gIdx],
            cheonganElement: CHEONGAN_ELEMENT[gIdx],
            cheonganColor: CHEONGAN_COLOR[gIdx],
            jiji: JIJI[jIdx],
            jijiHanja: JIJI_HANJA[jIdx],
            jijiElement: JIJI_ELEMENT[jIdx],
            jijiColor: JIJI_COLOR[jIdx],
            jijiAnimal: JIJI_ANIMAL[jIdx]
        };
    }

    let date = new Date(payload.year, payload.month - 1, payload.day);
    if (payload.isLunar) {
        date.setDate(date.getDate() + 29);
    }

    let sYear = date.getFullYear();
    if (date.getMonth() < 1 || (date.getMonth() === 1 && date.getDate() < 4)) {
        sYear -= 1;
    }

    let yG = (sYear - 4) % 10;
    if (yG < 0) yG += 10;
    let yJ = (sYear - 4) % 12;
    if (yJ < 0) yJ += 12;
    const yearPillar = createPillar(yG, yJ);

    // 월주
    const m = date.getMonth() + 1;
    const d = date.getDate();
    let mJ = 2; // 인월 기준
    if (m === 1) mJ = (d < 6) ? 0 : 1;
    else if (m === 2) mJ = (d < 4) ? 1 : 2;
    else if (m === 3) mJ = (d < 5) ? 2 : 3;
    else if (m === 4) mJ = (d < 5) ? 3 : 4;
    else if (m === 5) mJ = (d < 5) ? 4 : 5;
    else if (m === 6) mJ = (d < 6) ? 5 : 6;
    else if (m === 7) mJ = (d < 7) ? 6 : 7;
    else if (m === 8) mJ = (d < 7) ? 7 : 8;
    else if (m === 9) mJ = (d < 7) ? 8 : 9;
    else if (m === 10) mJ = (d < 8) ? 9 : 10;
    else if (m === 11) mJ = (d < 7) ? 10 : 11;
    else mJ = (d < 7) ? 11 : 0;

    const inBase = [2, 4, 6, 8, 0][yG % 5];
    const offset = (mJ - 2 + 12) % 12;
    const mG = (inBase + offset) % 10;
    const monthPillar = createPillar(mG, mJ);

    // 일주
    const baseDate = new Date(2000, 0, 1);
    const diffDays = Math.floor((date.getTime() - baseDate.getTime()) / (1000 * 60 * 60 * 24));
    let dayGanji = (54 + diffDays) % 60;
    if (dayGanji < 0) dayGanji += 60;
    const dG = dayGanji % 10;
    const dJ = dayGanji % 12;
    const dayPillar = createPillar(dG, dJ);

    // 시주
    let timePillar = null;
    if (payload.hour !== null && payload.hour !== undefined) {
        const totalMin = payload.hour * 60 + (payload.minute || 0);
        const adj = (totalMin + 30) % 1440;
        const tJ = Math.floor(adj / 120);
        const tBase = [0, 2, 4, 6, 8][dG % 5];
        const tG = (tBase + tJ) % 10;
        timePillar = createPillar(tG, tJ);
    }

    // 오행 집계
    const counts = { '목': 0, '화': 0, '토': 0, '금': 0, '수': 0 };
    const pillars = [yearPillar, monthPillar, dayPillar];
    if (timePillar) pillars.push(timePillar);

    pillars.forEach(p => {
        counts[p.cheonganElement]++;
        counts[p.jijiElement]++;
    });

    const totalE = pillars.length * 2;
    const pcts = {};
    for (let k in counts) {
        pcts[k] = Math.round((counts[k] / totalE) * 100);
    }

    let maxK = '목', maxV = -1, minK = '목', minV = 999;
    for (let k in counts) {
        if (counts[k] > maxV) { maxV = counts[k]; maxK = k; }
        if (counts[k] < minV) { minV = counts[k]; minK = k; }
    }

    const currentYear = 2026;
    const currentAge = currentYear - date.getFullYear() + 1;
    const daeunNum = ((Math.abs(date.getDate() * 3 + (date.getMonth() + 1)) % 8) + 2);
    const startAge = daeunNum + Math.floor((currentAge - daeunNum) / 10) * 10;
    const endAge = startAge + 9;

    const daeunList = [];
    for (let i = 0; i < 8; i++) {
        const sA = daeunNum + i * 10;
        const eA = sA + 9;
        const gI = (mG + i + 1) % 10;
        const jI = (mJ + i + 1) % 12;
        daeunList.push({
            startAge: sA,
            endAge: eA,
            kanji: `${CHEONGAN[gI]}${JIJI[jI]}(${CHEONGAN_HANJA[gI]}${JIJI_HANJA[jI]})`,
            element: `${CHEONGAN_ELEMENT[gI]}/${JIJI_ELEMENT[jI]}`,
            theme: i % 2 === 0 ? "도약과 성취의 황금기" : "안정과 결실을 다지는 시기",
            isCurrent: (currentAge >= sA && currentAge <= eA)
        });
    }

    return {
        name: payload.name,
        gender: payload.gender === '남' ? '남성' : '여성',
        birthDate: `${payload.year}년 ${payload.month < 10 ? '0' + payload.month : payload.month}월 ${payload.day < 10 ? '0' + payload.day : payload.day}일 (${payload.isLunar ? '음력' : '양력'})`,
        birthTime: payload.hour === null ? '시간 모름' : `${payload.hour}시 ${payload.minute}분`,
        yearPillar, monthPillar, dayPillar, timePillar,
        elementCounts: counts,
        elementPercentages: pcts,
        dominantElement: maxK,
        dominantDesc: `사주에 ${maxK}의 기운이 ${maxV}개로 가장 왕성합니다. 타고난 천성과 직관력이 돋보이며 남다른 추진력을 발휘합니다.`,
        deficientElement: minK,
        deficientDesc: `${minK}의 기운이 ${minV}개로 부족하므로, 일상에서 ${minK}에 해당하는 색상과 생활 습관으로 균형을 맞춰주는 개운법이 필수적입니다.`,
        daeunNumber: daeunNum,
        currentDaeunStartAge: startAge,
        currentDaeunEndAge: endAge,
        currentDaeunKanJi: "갑인(甲寅)",
        daeunSummary: `현재 ${currentAge}세이신 귀하는 ${startAge}세부터 ${endAge}세까지 이어지는 대운의 흐름 속에 있습니다. 이 대운은 **${currentYear + (endAge - currentAge)}년(${endAge}세)**까지 지속되며, 이후 새로운 대운으로 국면이 전환됩니다.`,
        daeunList,
        cautions: [
            "【감정 및 언행 조심】 의욕이 앞서 즉흥적인 언행으로 주변과 마찰을 빚을 수 있으니 중요한 결정 전에는 하루의 여유를 두세요.",
            "【투자 및 재물 관리】 무리한 단기 차익이나 불확실한 지출을 삼가고 정기적인 자산 점검을 권장합니다.",
            "【2026년 붉은 말의 해 특별 경고】 화(火) 기운이 극에 달하는 해이므로 번아웃과 급격한 체력 저하에 각별히 유의하세요."
        ],
        yearFortune: {
            targetYear: 2026,
            yearKanJi: "병오년 (丙午年, 붉은 말의 해)",
            overallScore: 88,
            headline: "역동적인 변화의 물결 속에서 내면의 주관을 세우고 결실을 거두는 비상의 해",
            overview: "2026년 병오년은 강렬한 불꽃처럼 역동적이고 추진력이 돋보이는 해입니다. 본인의 사주 원국과 어우러져 숨겨져 있던 역량이 수면 위로 떠오르며 큰 기회가 찾아옵니다.",
            wealth: "상반기에는 씨를 뿌리고 기반을 다지는 시기이며, 하반기에 실질적인 보상과 수익으로 회수되는 흐름입니다.",
            love: "자신감 넘치는 태도가 주변을 매료시킵니다. 적극적인 모임 참여가 좋은 인연을 연결해 줍니다.",
            career: "기존 업무에서의 성과를 인정받아 새로운 권한이 주어지며 주도적인 위치에 서게 됩니다.",
            health: "심혈관과 안구 피로, 과로를 경계해야 합니다. 규칙적인 수면 루틴을 유지하세요.",
            quarters: [
                { quarter: "1분기 (봄: 1~3월)", title: "도약의 씨앗을 뿌리는 시기", desc: "새로운 계획을 구체화하고 기초를 다지는 정지 작업이 중요합니다." },
                { quarter: "2분기 (여름: 4~6월)", title: "기회와 인연의 확장", desc: "외부 제안이 늘어나며 활동 반경이 크게 넓어지는 역동기입니다." },
                { quarter: "3분기 (가을: 7~9월)", title: "실질적 결실과 성과 수확", desc: "노력의 결과물이 금전과 명예로 환산되는 최고의 수확기입니다." },
                { quarter: "4분기 (겨울: 10~12월)", title: "내실 다지기 및 차년도 설계", desc: "지친 심신을 보양하고 내년의 더 큰 비상을 준비하는 시기입니다." }
            ],
            warningMonths: ["음력 5월(양력 6~7월) - 충동 지출 및 감정 기복 주의", "음력 11월(양력 12월) - 과로 및 면역력 저하 경계"]
        },
        todayFortune: {
            date: "2026년 9월 22일",
            dayKanJi: "오늘의 일진: 길운이 머무는 날",
            totalScore: 92,
            keyword: "귀인과의 조우, 막힌 매듭이 풀리는 날",
            commentary: "생각지도 못했던 곳에서 실마리를 찾거나 나를 진심으로 도와줄 협력자를 만나게 되는 운세입니다.",
            luckyColor: "포레스트 그린 & 에메랄드",
            luckyNumber: "3, 7, 8",
            luckyDirection: "남동쪽 (따뜻한 번영의 방위)",
            luckyTime: "오후 1시 30분 ~ 3시 30분",
            wealthScore: 88,
            loveScore: 95,
            careerScore: 90,
            healthScore: 85
        }
    };
}
