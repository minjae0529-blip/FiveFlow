package com.korai.saju;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class SajuEngine {

    // 천간 데이터
    public static final String[] CHEONGAN = {"갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"};
    public static final String[] CHEONGAN_HANJA = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    public static final String[] CHEONGAN_ELEMENT = {"목", "목", "화", "화", "토", "토", "금", "금", "수", "수"};
    public static final String[] CHEONGAN_COLOR = {"#2e7d32", "#4caf50", "#d32f2f", "#f44336", "#c77700", "#e6a100", "#cfd8dc", "#90a4ae", "#1565c0", "#1e88e5"};

    // 지지 데이터
    public static final String[] JIJI = {"자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"};
    public static final String[] JIJI_HANJA = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    public static final String[] JIJI_ELEMENT = {"수", "토", "목", "목", "토", "화", "화", "토", "금", "금", "토", "수"};
    public static final String[] JIJI_COLOR = {"#1565c0", "#c77700", "#2e7d32", "#4caf50", "#c77700", "#d32f2f", "#f44336", "#c77700", "#cfd8dc", "#90a4ae", "#c77700", "#1565c0"};
    public static final String[] JIJI_ANIMAL = {"쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양", "원숭이", "닭", "개", "돼지"};

    // 기준일: 2000년 1월 1일은 '무오(戊午)'일 (천간 무=4, 지지 오=6 -> 60간지 인덱스 54)
    private static final LocalDate BASE_DATE = LocalDate.of(2000, 1, 1);
    private static final int BASE_DAY_GANJI_INDEX = 54; // 60갑자 중 무오

    public static SajuResult calculate(String name, String gender, int year, int month, int day, Integer hour, Integer minute, boolean isLunar) {
        SajuResult res = new SajuResult();
        res.name = (name == null || name.trim().isEmpty()) ? "인연님" : name.trim();
        res.gender = "남".equals(gender) ? "남성" : "여성";
        res.birthDate = String.format("%04d년 %02d월 %02d일 (%s)", year, month, day, isLunar ? "음력" : "양력");
        res.birthTime = (hour == null) ? "시간 모름" : String.format("%02d시 %02d분", hour, (minute == null ? 0 : minute));
        res.isLunar = isLunar;

        // 음력인 경우 양력 근사 환산 (실무 약식 보정: 통상 29~30일 가산)
        LocalDate solarDate;
        if (isLunar) {
            solarDate = LocalDate.of(year, month, day).plusDays(29);
        } else {
            solarDate = LocalDate.of(year, month, day);
        }

        // 1. 년주 (Year Pillar)
        // 입춘(통상 2월 4일경) 이전 출생자는 전년도 간지로 처리
        int sajuYear = solarDate.getYear();
        if (solarDate.getMonthValue() < 2 || (solarDate.getMonthValue() == 2 && solarDate.getDayOfMonth() < 4)) {
            sajuYear -= 1;
        }
        int yearGanIndex = (sajuYear - 4) % 10;
        if (yearGanIndex < 0) yearGanIndex += 10;
        int yearJiIndex = (sajuYear - 4) % 12;
        if (yearJiIndex < 0) yearJiIndex += 12;

        res.yearPillar = createPillar(yearGanIndex, yearJiIndex);

        // 2. 월주 (Month Pillar)
        // 월지: 인월(2월경 입춘)~축월(1월경 소한)
        int monthJiIndex = getMonthJiIndex(solarDate);
        // 월간: 오호둔월법 (년간에 따라 인월의 천간 결정)
        // 갑기(0,5)->병인(2), 을경(1,6)->무인(4), 병신(2,7)->경인(6), 정임(3,8)->임인(8), 무계(4,9)->갑인(0)
        int inMonthGanBase = switch (yearGanIndex % 5) {
            case 0 -> 2; // 병
            case 1 -> 4; // 무
            case 2 -> 6; // 경
            case 3 -> 8; // 임
            case 4 -> 0; // 갑
            default -> 0;
        };
        // 인월(인덱스 2)부터 몇 번째인지 계산
        int offsetFromIn = (monthJiIndex - 2 + 12) % 12;
        int monthGanIndex = (inMonthGanBase + offsetFromIn) % 10;
        res.monthPillar = createPillar(monthGanIndex, monthJiIndex);

        // 3. 일주 (Day Pillar)
        long daysDiff = ChronoUnit.DAYS.between(BASE_DATE, solarDate);
        int dayGanjiIndex = (int) ((BASE_DAY_GANJI_INDEX + daysDiff) % 60);
        if (dayGanjiIndex < 0) dayGanjiIndex += 60;
        int dayGanIndex = dayGanjiIndex % 10;
        int dayJiIndex = dayGanjiIndex % 12;
        res.dayPillar = createPillar(dayGanIndex, dayJiIndex);

        // 4. 시주 (Time Pillar)
        if (hour != null) {
            int timeJiIndex = getTimeJiIndex(hour, minute != null ? minute : 0);
            // 시간: 오자둔시법 (일간에 따라 자시의 천간 결정)
            // 갑기(0,5)->갑자(0), 을경(1,6)->병자(2), 병신(2,7)->무자(4), 정임(3,8)->경자(6), 무계(4,9)->임자(8)
            int jaTimeGanBase = switch (dayGanIndex % 5) {
                case 0 -> 0; // 갑
                case 1 -> 2; // 병
                case 2 -> 4; // 무
                case 3 -> 6; // 경
                case 4 -> 8; // 임
                default -> 0;
            };
            int timeGanIndex = (jaTimeGanBase + timeJiIndex) % 10;
            res.timePillar = createPillar(timeGanIndex, timeJiIndex);
        } else {
            res.timePillar = null; // 시간 모름
        }

        // 5. 오행 분포 분석
        analyzeElements(res);

        // 6. 대운 계산
        calculateDaeun(res, solarDate, yearGanIndex, monthGanIndex, monthJiIndex, gender);

        // 7. 주의사항 (조심해야 할 점)
        generateCautions(res);

        // 8. 2026년 전체 운세 & 오늘의 운세
        generateFortunes(res, solarDate);

        return res;
    }

    private static SajuResult.Pillar createPillar(int ganIdx, int jiIdx) {
        return new SajuResult.Pillar(
                CHEONGAN[ganIdx],
                CHEONGAN_HANJA[ganIdx],
                CHEONGAN_ELEMENT[ganIdx],
                CHEONGAN_COLOR[ganIdx],
                JIJI[jiIdx],
                JIJI_HANJA[jiIdx],
                JIJI_ELEMENT[jiIdx],
                JIJI_COLOR[jiIdx],
                JIJI_ANIMAL[jiIdx]
        );
    }

    private static int getMonthJiIndex(LocalDate date) {
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        // 절기 대략 매핑 (4~8일 기준 절기 변화)
        // 2월 4일경: 인(2), 3월 5일경: 묘(3), 4월 5일: 진(4), 5월 5일: 사(5), 6월 6일: 오(6), 7월 7일: 미(7)
        // 8월 7일: 신(8), 9월 7일: 유(9), 10월 8일: 술(10), 11월 7일: 해(11), 12월 7일: 자(0), 1월 6일: 축(1)
        if (m == 1) return (d < 6) ? 0 : 1;
        if (m == 2) return (d < 4) ? 1 : 2;
        if (m == 3) return (d < 5) ? 2 : 3;
        if (m == 4) return (d < 5) ? 3 : 4;
        if (m == 5) return (d < 5) ? 4 : 5;
        if (m == 6) return (d < 6) ? 5 : 6;
        if (m == 7) return (d < 7) ? 6 : 7;
        if (m == 8) return (d < 7) ? 7 : 8;
        if (m == 9) return (d < 7) ? 8 : 9;
        if (m == 10) return (d < 8) ? 9 : 10;
        if (m == 11) return (d < 7) ? 10 : 11;
        return (d < 7) ? 11 : 0; // 12월
    }

    private static int getTimeJiIndex(int hour, int minute) {
        int totalMinutes = hour * 60 + minute;
        // 23:30 ~ 01:29: 자시(0)
        // 01:30 ~ 03:29: 축시(1) ...
        int adjusted = (totalMinutes + 30) % 1440;
        return adjusted / 120;
    }

    private static void analyzeElements(SajuResult res) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("목", 0);
        counts.put("화", 0);
        counts.put("토", 0);
        counts.put("금", 0);
        counts.put("수", 0);

        List<SajuResult.Pillar> pillars = new ArrayList<>();
        pillars.add(res.yearPillar);
        pillars.add(res.monthPillar);
        pillars.add(res.dayPillar);
        if (res.timePillar != null) pillars.add(res.timePillar);

        for (SajuResult.Pillar p : pillars) {
            counts.put(p.cheonganElement, counts.get(p.cheonganElement) + 1);
            counts.put(p.jijiElement, counts.get(p.jijiElement) + 1);
        }

        int total = pillars.size() * 2;
        Map<String, Integer> pcts = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            pcts.put(e.getKey(), Math.round((e.getValue() * 100.0f) / total));
        }

        res.elementCounts = counts;
        res.elementPercentages = pcts;

        // 최다 오행 & 부족 오행 추출
        String maxEl = "목";
        int maxVal = -1;
        String minEl = "목";
        int minVal = 999;

        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > maxVal) {
                maxVal = e.getValue();
                maxEl = e.getKey();
            }
            if (e.getValue() < minVal) {
                minVal = e.getValue();
                minEl = e.getKey();
            }
        }

        res.dominantElement = maxEl;
        res.dominantDesc = getDominantDescription(maxEl, maxVal);
        res.deficientElement = minEl;
        res.deficientDesc = getDeficientDescription(minEl, minVal);
    }

    private static String getDominantDescription(String el, int count) {
        return switch (el) {
            case "목" -> "사주에 푸른 나무의 기운(木)이 " + count + "개로 매우 왕성합니다. 새로운 일을 기획하고 밀어붙이는 진취성과 어진 마음이 강점이지만, 지나치면 고집이 세어지거나 타협을 어려워할 수 있습니다.";
            case "화" -> "사주에 타오르는 불의 기운(火)이 " + count + "개로 강렬합니다. 열정과 추진력, 남다른 표현력과 화려함을 지녔으나, 성급한 판단이나 쉽게 타오르고 지치는 번아웃을 경계해야 합니다.";
            case "토" -> "사주에 대지의 기운(土)이 " + count + "개로 두텁습니다. 깊은 신뢰감, 포용력, 흔들리지 않는 묵직함이 큰 무기입니다. 다만 변화에 둔감해지거나 생각이 너무 많아 실행이 늦어질 수 있습니다.";
            case "금" -> "사주에 서슬 퍼런 쇠와 바위의 기운(金)이 " + count + "개로 굳건합니다. 맺고 끊음이 확실한 결단력과 높은 정의감, 정밀함을 자랑합니다. 그러나 타인에게 너무 엄격하거나 날카로운 언행으로 비칠 수 있습니다.";
            case "수" -> "사주에 깊은 물의 기운(水)이 " + count + "개로 맑고 풍부합니다. 깊은 지혜와 직관, 유연한 융통성을 가졌으나 속마음을 잘 드러내지 않아 오해를 사거나 홀로 생각이 깊어져 고독해질 수 있습니다.";
            default -> "기운이 비교적 고르게 조화를 이루고 있습니다.";
        };
    }

    private static String getDeficientDescription(String el, int count) {
        String base = (count == 0) ? "현재 사주에 기운이 전혀 없어(0개) " : "기운이 " + count + "개로 상대적으로 약하여 ";
        return switch (el) {
            case "목" -> base + "시작하는 결단력과 끈기가 흔들릴 수 있습니다. 초록색 계열의 옷, 식물 기르기, 아침 산책으로 목 기운을 채워주면 운이 크게 트입니다.";
            case "화" -> base + "열정과 행동력이 저하되거나 온기가 부족할 수 있습니다. 붉은색 계열 소품, 활발한 유산소 운동, 햇볕을 자주 쬐는 습관이 운을 보강합니다.";
            case "토" -> base + "삶의 중심을 잡는 안정감과 끈기가 부족할 수 있습니다. 베이지/황토색 계열의 인테리어, 흙을 밟는 맨발 걷기, 규칙적인 식사가 복을 부릅니다.";
            case "금" -> base + "단호한 결단력이나 정리정돈의 절제력이 아쉬울 수 있습니다. 흰색/메탈 액세서리 착용, 일상의 불필요한 인간관계 및 물건 비우기가 큰 개운법입니다.";
            case "수" -> base + "유연함과 깊은 통찰력, 내면의 휴식이 부족할 수 있습니다. 짙은 네이비/블랙 계열 활용, 물을 자주 섭취하고 반신욕을 즐기는 것이 균형을 맞춥니다.";
            default -> "오행이 조화롭게 갖추어져 있습니다.";
        };
    }

    private static void calculateDaeun(SajuResult res, LocalDate solarDate, int yearGanIdx, int monthGanIdx, int monthJiIdx, String gender) {
        // 양남음녀는 순행(1), 음남양녀는 역행(-1)
        boolean isYearGanYang = (yearGanIdx % 2 == 0); // 갑, 병, 무, 경, 임이 양(0,2,4,6,8)
        boolean isMale = "남".equals(gender) || "남성".equals(gender);
        boolean forward = (isYearGanYang && isMale) || (!isYearGanYang && !isMale);

        // 대운수 (1~9세 중 간이 산출: 일자 기반 안정적 해시)
        int daeunNum = (Math.abs(solarDate.getDayOfMonth() * 3 + solarDate.getMonthValue()) % 8) + 2; // 2 ~ 9세
        res.daeunNumber = daeunNum;

        int currentYear = 2026;
        int currentAge = currentYear - solarDate.getYear() + 1; // 한국 세는나이

        List<SajuResult.DaeunPeriod> list = new ArrayList<>();
        int curStart = daeunNum;
        int activeIdx = -1;

        for (int i = 0; i < 8; i++) {
            int startAge = curStart + (i * 10);
            int endAge = startAge + 9;

            int step = forward ? (i + 1) : -(i + 1);
            int gIdx = (monthGanIdx + step) % 10;
            if (gIdx < 0) gIdx += 10;
            int jIdx = (monthJiIdx + step) % 12;
            if (jIdx < 0) jIdx += 12;

            String kanji = CHEONGAN[gIdx] + JIJI[jIdx] + "(" + CHEONGAN_HANJA[gIdx] + JIJI_HANJA[jIdx] + ")";
            String el = CHEONGAN_ELEMENT[gIdx] + "/" + JIJI_ELEMENT[jIdx];

            String theme = switch (i % 4) {
                case 0 -> "기반 구축과 학업·성장의 시기";
                case 1 -> "활동 반경 확장 및 도약의 황금기";
                case 2 -> "재물 성취와 명예가 결실을 맺는 시기";
                default -> "내실을 다지고 안정을 도모하는 성숙기";
            };

            boolean isCur = (currentAge >= startAge && currentAge <= endAge);
            if (isCur) {
                activeIdx = i;
                res.currentDaeunStartAge = startAge;
                res.currentDaeunEndAge = endAge;
                res.currentDaeunKanJi = kanji;
            }

            list.add(new SajuResult.DaeunPeriod(startAge, endAge, kanji, el, theme, isCur));
        }

        if (activeIdx == -1) {
            // 범위 밖일 경우 기본 첫번째 혹은 최근 지정
            activeIdx = Math.min(Math.max(0, (currentAge - daeunNum) / 10), list.size() - 1);
            list.get(activeIdx).isCurrent = true;
            res.currentDaeunStartAge = list.get(activeIdx).startAge;
            res.currentDaeunEndAge = list.get(activeIdx).endAge;
            res.currentDaeunKanJi = list.get(activeIdx).kanji;
        }

        res.daeunList = list;
        int remainYears = res.currentDaeunEndAge - currentAge;
        int endYear = currentYear + remainYears;

        res.daeunSummary = String.format(
                "현재 %d세(%s년)이신 귀하는 %d세부터 %d세까지 이어지는 【%s】 대운의 한가운데에 있습니다. 이 대운의 기운은 앞으로 %d년 뒤인 **%d년(%d세)**까지 지속되며, 이후 인생의 새로운 도약 국면인 다음 대운으로 전환됩니다.",
                currentAge, currentYear, res.currentDaeunStartAge, res.currentDaeunEndAge, res.currentDaeunKanJi,
                Math.max(1, remainYears), endYear, res.currentDaeunEndAge
        );
    }

    private static void generateCautions(SajuResult res) {
        List<String> list = new ArrayList<>();

        // 1. 최다 오행에 따른 주의점
        switch (res.dominantElement) {
            case "목" -> list.add("【과욕 및 번아웃 주의】 의욕이 앞서 너무 많은 프로젝트를 동시에 벌이면 마무리가 흐려질 수 있으니, 우선순위를 명확히 하세요.");
            case "화" -> list.add("【언행 및 충동 조심】 순간의 감정 격화나 욱하는 마음에 내뱉은 말이 구설수로 이어질 수 있으니 3초간 숨을 고르는 지혜가 필요합니다.");
            case "토" -> list.add("【고집과 정체 경계】 내 방식만을 고집하다 보면 주변 사람과의 마찰이나 기회를 놓칠 수 있으니, 유연한 의견 수용이 필수적입니다.");
            case "금" -> list.add("【인간관계 냉각 주의】 날카롭고 엄격한 잣대를 타인에게 들이대면 주변이 떠나갈 수 있습니다. 조금은 너그럽고 온화한 태도가 행운을 부릅니다.");
            case "수" -> list.add("【비밀주의와 과도한 잡념】 생각이 꼬리를 물어 불면증이나 우울감에 빠지지 않도록 주의하고, 믿을 수 있는 사람과 솔직하게 소통하세요.");
        }

        // 2. 결핍 오행에 따른 보완 주의점
        switch (res.deficientElement) {
            case "목" -> list.add("【결단 지연 주의】 시작을 두려워해 준비만 하다가 타이밍을 놓치기 쉽습니다. 70% 준비되었을 때 과감히 발을 떼세요.");
            case "화" -> list.add("【체력 고갈 및 무기력】 열정과 에너지가 쉽게 바닥날 수 있으니 충분한 수면과 햇살 샤워로 내면의 온기를 유지하세요.");
            case "토" -> list.add("【충동 이동 및 낭비 경계】 마음이 쉽게 흔들려 거처나 직장을 가볍게 바꾸지 말고, 자산 관리에서도 충동 지출을 엄격히 통제하세요.");
            case "금" -> list.add("【우유부단 및 정리 미흡】 정에 이끌려 맺고 끊지 못하면 손해를 봅니다. 계약서나 금전 거래는 아무리 친해도 명확히 문서화하세요.");
            case "수" -> list.add("【융통성 부족 및 스트레스】 여유 없이 빡빡하게 계획을 세우면 돌발 변수에 취약해집니다. 일정에 20%의 여백을 두세요.");
        }

        // 3. 2026년 병오(丙午)년 특별 주의점
        list.add("【2026년 붉은 말의 해 특별 경고】 화(火) 기운이 극도로 강력한 해이므로, 일확천금을 노리는 무리한 단기 투자나 도박성 지출은 반드시 피하고 안정 자산 위주로 운용하십시오.");

        res.cautions = list;
    }

    private static void generateFortunes(SajuResult res, LocalDate solarDate) {
        // 2026년 전체 운세
        SajuResult.YearFortune yf = new SajuResult.YearFortune();
        yf.targetYear = 2026;
        yf.yearKanJi = "병오년 (丙午年, 붉은 말의 해)";
        yf.overallScore = 88;
        yf.headline = "역동적인 변화의 물결 속에서 내면의 주관을 세우고 결실을 거두는 비상의 해";
        yf.overview = "2026년 병오년은 강렬한 불꽃처럼 역동적이고 추진력이 돋보이는 해입니다. 본인의 사주 원국과 어우러져 숨겨져 있던 역량이 수면 위로 떠오르며, 특히 새로운 시도와 네트워크 확장에서 큰 기회가 찾아옵니다. 다만 속도감이 빠른 만큼 중간 점검 없는 폭주는 부작용을 낳을 수 있으니 완급 조절이 올해의 최대 성공 열쇠입니다.";
        yf.wealth = "상반기에는 씨를 뿌리고 기반을 닦는 시기이며, 음력 8월 이후 하반기에 실질적인 수익과 보상으로 회수되는 흐름입니다. 무리한 대출을 통한 확장은 금물이며, 꾸준한 현금 흐름 창출에 집중하세요.";
        yf.love = "솔로는 자신감 넘치고 매력적인 인연이 다가올 호운입니다. 동호회, 모임, 스터디 등 사람이 모이는 자리에서 인연을 찾으세요. 커플 및 기혼자는 사소한 자존심 대립을 피하고 감사의 표현을 자주 하세요.";
        yf.career = "기존에 해오던 일에서 실력을 인정받아 권한이 확대되거나, 평소 꿈꾸던 새로운 분야로의 전직 및 사업 확장의 물꼬가 트입니다. 동료와의 협업에 공을 나누면 명예가 배가됩니다.";
        yf.health = "심혈관계, 안구 피로, 목/어깨 근육 뭉침을 조심해야 합니다. 늦은 밤 스마트폰 사용을 줄이고 차분한 명상과 스트레칭을 루틴화하세요.";

        yf.quarters = List.of(
                new SajuResult.QuarterGuide("1분기 (봄: 1~3월)", "도약의 씨앗을 뿌리는 시기", "새로운 계획을 구체화하고 불필요한 루틴을 정리하는 정지 작업이 중요합니다."),
                new SajuResult.QuarterGuide("2분기 (여름: 4~6월)", "기회와 대인관계의 확장", "외부 미팅과 제안이 쏟아지는 시기입니다. 옥석을 가려 진짜 내 사람을 선별하세요."),
                new SajuResult.QuarterGuide("3분기 (가을: 7~9월)", "실질적 결실과 성과 수확", "그동안 노력해 온 일에 대한 금전적, 사회적 보상이 가시화되는 최고의 수확기입니다."),
                new SajuResult.QuarterGuide("4분기 (겨울: 10~12월)", "내실 다지기 및 다음 해 준비", "성과를 축하하고 지친 심신을 충전하며 내년의 더 큰 비상을 설계하는 시기입니다.")
        );
        yf.warningMonths = List.of("음력 5월(양력 6~7월) - 감정 기복과 충동 지출 주의", "음력 11월(양력 12월) - 건강 피로 누적 및 낙상 주의");
        res.yearFortune = yf;

        // 오늘의 운세
        SajuResult.TodayFortune tf = new SajuResult.TodayFortune();
        LocalDate now = LocalDate.now();
        tf.date = String.format("%d년 %d월 %d일", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        tf.dayKanJi = "오늘의 일진: 길운이 머무는 길일";
        tf.totalScore = 92;
        tf.keyword = "귀인(貴人)과의 조우, 막힌 매듭이 풀리는 날";
        tf.commentary = "생각지도 못했던 곳에서 힌트를 얻거나 나를 진심으로 도와줄 협력자를 마주하게 되는 운세입니다. 평소 미뤄두었던 연락이나 해결하기 까다로웠던 업무가 있다면 오늘 오후에 시도해 보세요. 자신감 있는 미소와 단정한 옷차림이 행운을 극대화합니다.";
        tf.luckyColor = switch (res.deficientElement) {
            case "목" -> "포레스트 그린 & 에메랄드";
            case "화" -> "코랄 레드 & 앰버 오렌지";
            case "토" -> "카멜 베이지 & 머스터드";
            case "금" -> "클래식 화이트 & 실버";
            default -> "미드나잇 네이비 & 차콜";
        };
        tf.luckyNumber = "3, 7, 8";
        tf.luckyDirection = "남동쪽 (따뜻하고 번영의 기운이 깃든 방위)";
        tf.luckyTime = "오후 1시 30분 ~ 3시 30분 (미시: 귀인의 조력이 가장 강한 시간)";
        tf.wealthScore = 88;
        tf.loveScore = 95;
        tf.careerScore = 90;
        tf.healthScore = 85;
        res.todayFortune = tf;
    }
}
