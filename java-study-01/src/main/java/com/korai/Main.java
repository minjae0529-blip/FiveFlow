package com.korai;

import com.korai.saju.SajuEngine;
import com.korai.saju.SajuResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 8080;
    private static final Path STATIC_DIR = Paths.get("src/main/resources/static");

    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // API 엔드포인트
            server.createContext("/api/saju", new SajuApiHandler());

            // 정적 자원 핸들러
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();

            System.out.println("=================================================");
            System.out.println("   [청담 운명학당] 사주 & 명리 웹 서버 가동 완료   ");
            System.out.println("   접속 주소: http://localhost:" + PORT);
            System.out.println("=================================================");

            // 브라우저 자동 실행 시도
            try {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    new ProcessBuilder("cmd", "/c", "start", "http://localhost:" + PORT).start();
                }
            } catch (Exception ignored) {
            }

        } catch (IOException e) {
            System.err.println("서버 구동 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class SajuApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS 헤더 설정
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            Map<String, String> params = new HashMap<>();

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String clStr = exchange.getRequestHeaders().getFirst("Content-Length");
                byte[] bodyBytes;
                if (clStr != null && !clStr.trim().isEmpty()) {
                    int len = Integer.parseInt(clStr.trim());
                    bodyBytes = is.readNBytes(len);
                } else {
                    bodyBytes = is.readAllBytes();
                }
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                parseParams(body, params);
            } else {
                String query = exchange.getRequestURI().getQuery();
                if (query != null) {
                    parseParams(query, params);
                }
            }

            String name = params.getOrDefault("name", "홍길동");
            String gender = params.getOrDefault("gender", "남");
            int year = parseInt(params.get("year"), 1995);
            int month = parseInt(params.get("month"), 5);
            int day = parseInt(params.get("day"), 15);
            
            String hourStr = params.get("hour");
            Integer hour = (hourStr == null || hourStr.isEmpty() || hourStr.equals("-1")) ? null : parseInt(hourStr, 12);
            
            String minStr = params.get("minute");
            Integer minute = (minStr == null || minStr.isEmpty()) ? 0 : parseInt(minStr, 0);

            boolean isLunar = "true".equalsIgnoreCase(params.get("isLunar"));

            SajuResult result = SajuEngine.calculate(name, gender, year, month, day, hour, minute, isLunar);
            String jsonResponse = result.toJson();

            byte[] respBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, respBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(respBytes);
            }
        }

        private int parseInt(String val, int def) {
            try {
                return Integer.parseInt(val.trim());
            } catch (Exception e) {
                return def;
            }
        }

        private void parseParams(String source, Map<String, String> out) {
            if (source.startsWith("{") && source.endsWith("}")) {
                // 초간단 JSON 파싱
                String clean = source.substring(1, source.length() - 1);
                String[] pairs = clean.split(",");
                for (String p : pairs) {
                    String[] kv = p.split(":", 2);
                    if (kv.length == 2) {
                        String k = kv[0].trim().replace("\"", "");
                        String v = kv[1].trim().replace("\"", "");
                        out.put(k, v);
                    }
                }
            } else {
                // 쿼리스트링 혹은 x-www-form-urlencoded
                String[] pairs = source.split("&");
                for (String p : pairs) {
                    String[] kv = p.split("=", 2);
                    if (kv.length == 2) {
                        String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                        String v = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                        out.put(k, v);
                    }
                }
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            Path filePath = STATIC_DIR.resolve(path.substring(1)).normalize();

            // 디렉토리 탈출 방지
            if (!filePath.startsWith(STATIC_DIR) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
                // 대체 시도: index.html
                filePath = STATIC_DIR.resolve("index.html");
                if (!Files.exists(filePath)) {
                    String notFound = "<h1>404 Not Found</h1>";
                    exchange.sendResponseHeaders(404, notFound.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }
            }

            String contentType = "text/plain; charset=UTF-8";
            String fn = filePath.getFileName().toString().toLowerCase();
            if (fn.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (fn.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (fn.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (fn.endsWith(".svg")) contentType = "image/svg+xml";
            else if (fn.endsWith(".png")) contentType = "image/png";
            else if (fn.endsWith(".jpg") || fn.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (fn.endsWith(".json")) contentType = "application/json; charset=UTF-8";

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
