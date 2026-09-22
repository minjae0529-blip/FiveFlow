package com.korai.saju;

import java.util.*;

public class SajuResult {
    public String name;
    public String gender;
    public String birthDate;
    public String birthTime;
    public boolean isLunar;

    // 사주 원국 (천간, 지지, 십신, 오행)
    public Pillar yearPillar;
    public Pillar monthPillar;
    public Pillar dayPillar;
    public Pillar timePillar;

    // 오행 통계 (목, 화, 토, 금, 수)
    public Map<String, Integer> elementCounts;
    public Map<String, Integer> elementPercentages;
    public String dominantElement;     // 가장 많은 오행
    public String dominantDesc;        // 최다 오행 설명
    public String deficientElement;    // 가장 부족한 오행
    public String deficientDesc;       // 부족 오행 설명 & 개운법

    // 대운 정보
    public int daeunNumber;            // 대운수 (시작 나이)
    public int currentDaeunStartAge;   // 현재 대운 시작 나이
    public int currentDaeunEndAge;     // 현재 대운 끝나는 나이
    public String currentDaeunKanJi;   // 현재 대운 간지
    public String daeunSummary;        // 대운 풀이 ("언제까지 대운인가")
    public List<DaeunPeriod> daeunList; // 대운 목록

    // 조심해야 할 점 (주의사항)
    public List<String> cautions;

    // 2026년 전체 운세
    public YearFortune yearFortune;

    // 오늘의 운세
    public TodayFortune todayFortune;

    public static class Pillar {
        public String cheongan;       // 천간 한글 (예: 갑)
        public String cheonganHanja;  // 천간 한자 (예: 甲)
        public String cheonganElement;// 오행 (목, 화, 토, 금, 수)
        public String cheonganColor;  // 컬러 코드
        public String jiji;           // 지지 한글 (예: 자)
        public String jijiHanja;      // 지지 한자 (예: 子)
        public String jijiElement;    // 지지 오행
        public String jijiColor;      // 컬러 코드
        public String jijiAnimal;     // 십이지 동물 (쥐, 소, 호랑이...)

        public Pillar(String c, String cH, String cEl, String cCol,
                      String j, String jH, String jEl, String jCol, String animal) {
            this.cheongan = c;
            this.cheonganHanja = cH;
            this.cheonganElement = cEl;
            this.cheonganColor = cCol;
            this.jiji = j;
            this.jijiHanja = jH;
            this.jijiElement = jEl;
            this.jijiColor = jCol;
            this.jijiAnimal = animal;
        }
    }

    public static class DaeunPeriod {
        public int startAge;
        public int endAge;
        public String kanji;
        public String element;
        public String theme;
        public boolean isCurrent;

        public DaeunPeriod(int s, int e, String k, String el, String theme, boolean cur) {
            this.startAge = s;
            this.endAge = e;
            this.kanji = k;
            this.element = el;
            this.theme = theme;
            this.isCurrent = cur;
        }
    }

    public static class YearFortune {
        public int targetYear;
        public String yearKanJi;
        public int overallScore;
        public String headline;
        public String overview;
        public String wealth;
        public String love;
        public String career;
        public String health;
        public List<QuarterGuide> quarters;
        public List<String> warningMonths;
    }

    public static class QuarterGuide {
        public String quarter;
        public String title;
        public String desc;

        public QuarterGuide(String q, String t, String d) {
            this.quarter = q;
            this.title = t;
            this.desc = d;
        }
    }

    public static class TodayFortune {
        public String date;
        public String dayKanJi;
        public int totalScore;
        public String keyword;
        public String commentary;
        public String luckyColor;
        public String luckyNumber;
        public String luckyDirection;
        public String luckyTime;
        public int wealthScore;
        public int loveScore;
        public int careerScore;
        public int healthScore;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":").append(escape(name)).append(",");
        sb.append("\"gender\":").append(escape(gender)).append(",");
        sb.append("\"birthDate\":").append(escape(birthDate)).append(",");
        sb.append("\"birthTime\":").append(escape(birthTime)).append(",");
        sb.append("\"isLunar\":").append(isLunar).append(",");

        // pillars
        sb.append("\"yearPillar\":").append(pillarJson(yearPillar)).append(",");
        sb.append("\"monthPillar\":").append(pillarJson(monthPillar)).append(",");
        sb.append("\"dayPillar\":").append(pillarJson(dayPillar)).append(",");
        sb.append("\"timePillar\":").append(timePillar == null ? "null" : pillarJson(timePillar)).append(",");

        // elements
        sb.append("\"elementCounts\":{");
        int ei = 0;
        for (var e : elementCounts.entrySet()) {
            if (ei++ > 0) sb.append(",");
            sb.append(escape(e.getKey())).append(":").append(e.getValue());
        }
        sb.append("},");

        sb.append("\"elementPercentages\":{");
        ei = 0;
        for (var e : elementPercentages.entrySet()) {
            if (ei++ > 0) sb.append(",");
            sb.append(escape(e.getKey())).append(":").append(e.getValue());
        }
        sb.append("},");

        sb.append("\"dominantElement\":").append(escape(dominantElement)).append(",");
        sb.append("\"dominantDesc\":").append(escape(dominantDesc)).append(",");
        sb.append("\"deficientElement\":").append(escape(deficientElement)).append(",");
        sb.append("\"deficientDesc\":").append(escape(deficientDesc)).append(",");

        // daeun
        sb.append("\"daeunNumber\":").append(daeunNumber).append(",");
        sb.append("\"currentDaeunStartAge\":").append(currentDaeunStartAge).append(",");
        sb.append("\"currentDaeunEndAge\":").append(currentDaeunEndAge).append(",");
        sb.append("\"currentDaeunKanJi\":").append(escape(currentDaeunKanJi)).append(",");
        sb.append("\"daeunSummary\":").append(escape(daeunSummary)).append(",");

        sb.append("\"daeunList\":[");
        for (int i = 0; i < daeunList.size(); i++) {
            if (i > 0) sb.append(",");
            var d = daeunList.get(i);
            sb.append("{")
              .append("\"startAge\":").append(d.startAge).append(",")
              .append("\"endAge\":").append(d.endAge).append(",")
              .append("\"kanji\":").append(escape(d.kanji)).append(",")
              .append("\"element\":").append(escape(d.element)).append(",")
              .append("\"theme\":").append(escape(d.theme)).append(",")
              .append("\"isCurrent\":").append(d.isCurrent)
              .append("}");
        }
        sb.append("],");

        // cautions
        sb.append("\"cautions\":[");
        for (int i = 0; i < cautions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(escape(cautions.get(i)));
        }
        sb.append("],");

        // yearFortune
        sb.append("\"yearFortune\":{")
          .append("\"targetYear\":").append(yearFortune.targetYear).append(",")
          .append("\"yearKanJi\":").append(escape(yearFortune.yearKanJi)).append(",")
          .append("\"overallScore\":").append(yearFortune.overallScore).append(",")
          .append("\"headline\":").append(escape(yearFortune.headline)).append(",")
          .append("\"overview\":").append(escape(yearFortune.overview)).append(",")
          .append("\"wealth\":").append(escape(yearFortune.wealth)).append(",")
          .append("\"love\":").append(escape(yearFortune.love)).append(",")
          .append("\"career\":").append(escape(yearFortune.career)).append(",")
          .append("\"health\":").append(escape(yearFortune.health)).append(",")
          .append("\"quarters\":[");
        for (int i = 0; i < yearFortune.quarters.size(); i++) {
            if (i > 0) sb.append(",");
            var q = yearFortune.quarters.get(i);
            sb.append("{\"quarter\":").append(escape(q.quarter))
              .append(",\"title\":").append(escape(q.title))
              .append(",\"desc\":").append(escape(q.desc)).append("}");
        }
        sb.append("],\"warningMonths\":[");
        for (int i = 0; i < yearFortune.warningMonths.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(escape(yearFortune.warningMonths.get(i)));
        }
        sb.append("]},");

        // todayFortune
        sb.append("\"todayFortune\":{")
          .append("\"date\":").append(escape(todayFortune.date)).append(",")
          .append("\"dayKanJi\":").append(escape(todayFortune.dayKanJi)).append(",")
          .append("\"totalScore\":").append(todayFortune.totalScore).append(",")
          .append("\"keyword\":").append(escape(todayFortune.keyword)).append(",")
          .append("\"commentary\":").append(escape(todayFortune.commentary)).append(",")
          .append("\"luckyColor\":").append(escape(todayFortune.luckyColor)).append(",")
          .append("\"luckyNumber\":").append(escape(todayFortune.luckyNumber)).append(",")
          .append("\"luckyDirection\":").append(escape(todayFortune.luckyDirection)).append(",")
          .append("\"luckyTime\":").append(escape(todayFortune.luckyTime)).append(",")
          .append("\"wealthScore\":").append(todayFortune.wealthScore).append(",")
          .append("\"loveScore\":").append(todayFortune.loveScore).append(",")
          .append("\"careerScore\":").append(todayFortune.careerScore).append(",")
          .append("\"healthScore\":").append(todayFortune.healthScore)
          .append("}");

        sb.append("}");
        return sb.toString();
    }

    private String pillarJson(Pillar p) {
        if (p == null) return "null";
        return "{\"cheongan\":" + escape(p.cheongan) +
               ",\"cheonganHanja\":" + escape(p.cheonganHanja) +
               ",\"cheonganElement\":" + escape(p.cheonganElement) +
               ",\"cheonganColor\":" + escape(p.cheonganColor) +
               ",\"jiji\":" + escape(p.jiji) +
               ",\"jijiHanja\":" + escape(p.jijiHanja) +
               ",\"jijiElement\":" + escape(p.jijiElement) +
               ",\"jijiColor\":" + escape(p.jijiColor) +
               ",\"jijiAnimal\":" + escape(p.jijiAnimal) + "}";
    }

    private static String escape(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r") + "\"";
    }
}
