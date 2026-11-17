package com.shopguide.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopguide.ai.AiChatService;
import com.shopguide.ai.ProductVisionService;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI 助手 WebSocket 处理器
 *
 * 协议（JSON 文本帧）：
 *   客户端 → {"message":"帮我推荐 1-100 元的生活用品"}
 *   客户端 → {"message":"这个鼠标有什么优势？","imageUrl":"https://.../xxx.jpg"}   带图片提问
 *   服务端 → {"type":"ready","configured":true,"message":"..."}       连接就绪
 *          {"type":"start"}                                          开始回答
 *          {"type":"tool","name":"查询商品"}                          模型调用了工具（前端可展示）
 *          {"type":"delta","content":"保温"}                          流式片段（逐个 token 推送）
 *          {"type":"done","dataChanged":true}                        回答结束；dataChanged 表示数据被改动
 *          {"type":"error","message":"..."}                          出错
 */
@Slf4j
@Component
public class AiWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 会让数据库数据发生变化的工具（前端据此刷新购物车/列表） */
    private static final Set<String> MUTATING_TOOLS =
            Set.of("addProduct", "updateProduct", "deleteProduct", "addToCart", "clearCart");

    /** 工具方法名 → 中文提示 */
    private static final Map<String, String> TOOL_LABELS = new HashMap<>();

    static {
        TOOL_LABELS.put("searchProducts", "查询商品");
        TOOL_LABELS.put("listCategories", "查询商品分类");
        TOOL_LABELS.put("viewCart", "查看购物车");
        TOOL_LABELS.put("listOrders", "查询订单");
        TOOL_LABELS.put("addProduct", "新增商品");
        TOOL_LABELS.put("updateProduct", "修改商品");
        TOOL_LABELS.put("deleteProduct", "删除商品");
        TOOL_LABELS.put("addToCart", "加入购物车");
        TOOL_LABELS.put("clearCart", "清空购物车");
        TOOL_LABELS.put("analyzeProductImage", "分析商品图片");
    }

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ProductVisionService productVisionService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        if (userId == null) {
            send(session, Map.of("type", "error", "message", "未登录或登录已过期，请重新登录后再使用 AI 助手"));
            close(session);
            return;
        }

        // 一条连接 = 一个会话：绑定当前用户 + 独立对话记忆 + 专属工具集
        AiChatService.AiChatSession aiSession = aiChatService.newSession(userId, username);
        session.getAttributes().put("aiSession", aiSession);

        if (!aiChatService.isConfigured()) {
            send(session, Map.of("type", "ready", "configured", false,
                    "message", "后端未检测到环境变量 DEEPSEEK_API_KEY，AI 助手暂不可用。"));
        } else {
            send(session, Map.of("type", "ready", "configured", true,
                    "message", "你好，" + username + "！我是导购助手，可以帮你找商品、增删改商品、清空购物车、查订单，也可以上传商品图片帮你分析卖点。"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AiChatService.AiChatSession ai = (AiChatService.AiChatSession) session.getAttributes().get("aiSession");
        if (ai == null) {
            return;
        }

        String text;
        String imageUrl;
        try {
            JsonNode node = JSON.readTree(message.getPayload());
            // 前端点“清空对话”：只清空服务端会话记忆，不调用模型
            if ("reset".equals(node.path("type").asText(""))) {
                ai.clearMemory();
                send(session, Map.of("type", "reset"));
                return;
            }
            text = node.path("message").asText("").trim();
            imageUrl = node.path("imageUrl").asText("").trim();
        } catch (Exception e) {
            send(session, Map.of("type", "error", "message", "消息格式不正确"));
            return;
        }
        if (imageUrl.isEmpty()) {
            imageUrl = null;
        }
        // 只传图片不写文字时，用一个默认提示词，让模型直接看图说话
        if (text.isEmpty() && imageUrl != null) {
            text = "请分析这张图片里的商品：它是什么、外观有哪些特征，以及相比同类商品有哪些优势（性能倾向、适用人群与场景）。";
        }
        if (text.isEmpty()) {
            return;
        }
        if (!aiChatService.isConfigured()) {
            send(session, Map.of("type", "error",
                    "message", "未配置 DEEPSEEK_API_KEY 环境变量，请配置后重启后端服务。"));
            return;
        }
        if (!ai.tryLock()) {
            send(session, Map.of("type", "error", "message", "上一条问题还在回答中，请稍等一下～"));
            return;
        }

        send(session, Map.of("type", "start"));
        final boolean[] dataChanged = {false};
        try {
            String question = text;
            if (imageUrl != null) {
                // 带图片：先做一次视觉分析（同步，非流式），再把结论交给流式助手组织语言
                send(session, Map.of("type", "tool", "name", "分析图片"));
                String analysis = productVisionService.analyzeUploadedImage(imageUrl, text);
                question = composeImageQuestion(analysis, text);
            }
            TokenStream stream = ai.assistant().chat(question);
            stream
                    // 流式片段：模型每吐出一小段就推给前端
                    .onPartialResponse(token -> send(session, Map.of("type", "delta", "content", token)))
                    // 模型决定调用工具时通知前端（例如“正在查询商品”）
                    .onToolExecuted(execution -> {
                        String name = execution.request().name();
                        if (MUTATING_TOOLS.contains(name)) {
                            dataChanged[0] = true;
                        }
                        send(session, Map.of("type", "tool", "name", TOOL_LABELS.getOrDefault(name, name)));
                    })
                    .onCompleteResponse(response -> {
                        Map<String, Object> done = new HashMap<>();
                        done.put("type", "done");
                        done.put("dataChanged", dataChanged[0]);
                        send(session, done);
                        ai.unlock();
                    })
                    .onError(error -> {
                        log.warn("AI 流式回答失败：{}", error.toString());
                        send(session, Map.of("type", "error", "message", friendlyError(error)));
                        ai.unlock();
                    })
                    .start();
        } catch (Exception e) {
            log.error("AI 调用异常", e);
            send(session, Map.of("type", "error", "message", friendlyError(e)));
            ai.unlock();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("AI WebSocket 传输异常：{}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 会话随连接关闭自动回收（对话记忆保存在会话对象中）
        session.getAttributes().remove("aiSession");
    }

    /* ==================== 工具方法 ==================== */

    /** 线程安全地推送 JSON 消息（流式回调可能来自模型 SDK 的线程） */
    private void send(WebSocketSession session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = JSON.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.debug("推送消息失败：{}", e.getMessage());
        }
    }

    private void close(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (Exception ignored) {
        }
    }

    /**
     * 把「图片分析结论 + 用户问题」拼成给流式助手的提问
     * （让助手像自己看过图一样作答，同时保留工具调用能力，例如顺势推荐同类商品）
     */
    private String composeImageQuestion(String analysis, String userQuestion) {
        return "用户上传了一张商品图片。以下是对这张图片的真实视觉分析结果：\n"
                + analysis
                + "\n\n用户的问题是：" + userQuestion
                + "\n\n请基于上面的图片分析结果回答用户，像你自己看到图片一样自然描述；"
                + "不要再提“系统分析 / 视觉分析结果”这类字眼，也不要编造分析里没有的外观细节。";
    }

    /** 把底层异常翻译成用户能看懂的提示 */
    private String friendlyError(Throwable error) {
        String msg = error.getMessage() == null ? error.toString() : error.getMessage();
        String lower = msg.toLowerCase();
        if (lower.contains("401") || lower.contains("invalid api key") || lower.contains("authentication")) {
            return "DeepSeek API Key 无效或已过期，请检查环境变量 DEEPSEEK_API_KEY。";
        }
        if (lower.contains("insufficient balance") || lower.contains("余额")) {
            return "DeepSeek 账户余额不足，请充值后重试。";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "调用大模型超时了，请稍后重试。";
        }
        return "AI 服务暂时不可用：" + (msg.length() > 160 ? msg.substring(0, 160) + "..." : msg);
    }
}
