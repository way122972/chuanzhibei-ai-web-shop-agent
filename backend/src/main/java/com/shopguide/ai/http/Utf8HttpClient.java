package com.shopguide.ai.http;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEvent;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 自定义 HTTP 客户端（UTF-8 安全的 SSE 解析）
 *
 * 背景：LangChain4j 1.0.x 自带的 JdkHttpClient 在读取 SSE 流时使用
 * {@code new InputStreamReader(inputStream)}（平台默认字符集），
 * 中文 Windows 下默认是 GBK，会把 DeepSeek 返回的中文（工具参数里的“生活用品”等）
 * 解码成乱码。这里用 HttpURLConnection 自己实现一份，固定按 UTF-8 解析。
 */
public class Utf8HttpClient implements HttpClient {

    private final Duration connectTimeout;
    private final Duration readTimeout;

    public Utf8HttpClient(Duration connectTimeout, Duration readTimeout) {
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(15) : connectTimeout;
        this.readTimeout = readTimeout == null ? Duration.ofSeconds(120) : readTimeout;
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest request) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(request);
            int code = conn.getResponseCode();
            String body = readAll(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
            if (code < 200 || code >= 300) {
                throw new HttpException(code, body);
            }
            return SuccessfulHttpResponse.builder()
                    .statusCode(code)
                    .headers(headers(conn))
                    .body(body)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("请求 AI 服务失败：" + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        HttpURLConnection conn = null;
        try {
            conn = openConnection(request);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                String errorBody = readAll(conn.getErrorStream());
                listener.onError(new HttpException(code, errorBody));
                return;
            }
            listener.onOpen(SuccessfulHttpResponse.builder()
                    .statusCode(code)
                    .headers(headers(conn))
                    .build());
            parseSse(conn.getInputStream(), listener);
            listener.onClose();
        } catch (Exception e) {
            listener.onError(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /* ==================== 内部实现 ==================== */

    private HttpURLConnection openConnection(HttpRequest request) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(request.url()).openConnection();
        conn.setRequestMethod(request.method().name());
        conn.setConnectTimeout((int) connectTimeout.toMillis());
        conn.setReadTimeout((int) readTimeout.toMillis());
        // 关闭压缩：避免拿到 gzip 流导致 SSE 逐行解析异常
        conn.setRequestProperty("Accept-Encoding", "identity");

        Map<String, List<String>> headers = request.headers();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String name = entry.getKey();
                if (name == null || "accept-encoding".equalsIgnoreCase(name) || entry.getValue() == null) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    conn.addRequestProperty(name, value);
                }
            }
        }

        String body = request.body();
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
        }
        return conn;
    }

    /** 按 SSE 规范逐行解析，固定 UTF-8 */
    private void parseSse(InputStream inputStream, ServerSentEventListener listener) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String eventName = null;
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    dispatch(listener, eventName, data);
                    eventName = null;
                    data.setLength(0);
                    continue;
                }
                if (line.startsWith(":")) {
                    continue;   // 注释/心跳
                }
                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    if (data.length() > 0) {
                        data.append('\n');
                    }
                    data.append(line.substring("data:".length()).trim());
                }
            }
            dispatch(listener, eventName, data);
        }
    }

    private void dispatch(ServerSentEventListener listener, String eventName, StringBuilder data) {
        if (data.length() > 0) {
            listener.onEvent(new ServerSentEvent(eventName, data.toString()));
        }
    }

    private String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
        }
        return sb.toString();
    }

    private Map<String, List<String>> headers(HttpURLConnection conn) {
        Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
