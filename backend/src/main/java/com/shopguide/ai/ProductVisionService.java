package com.shopguide.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopguide.config.AiConfig;
import com.shopguide.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * 商品图片视觉分析服务
 *
 * 关键点（踩坑记录）：
 * 1. 只有支持 image 输入的模型才看得见图。DeepSeek 当前可用模型里 deepseek-flash
 *    的 input_modalities 含 image，deepseek-chat 只是别名、会被路由，但 LangChain4j
 *    1.0.1 把 ImageContent 序列化成 image_url 时对「URL 形式」支持不完整，
 *    发出去的内容模型读不到图 —— 这就是之前「看不到图片」的根本原因。
 *    所以这里不走 LangChain4j，直接用 JDK HttpClient 调 OpenAI 兼容接口。
 * 2. 图片先由服务端下载并转成 base64 data URL 再发给模型，避免 OSS 私有读、
 *    防盗链、外链超时等问题（模型侧下载失败同样会「看不见图」）。
 * 3. DeepSeek 新版模型默认 reasoning_effort=high，会先输出一大段思考内容，
 *    这里显式设为 low，缩短耗时；返回为空时回退读取 reasoning_content。
 */
@Slf4j
@Service
public class ProductVisionService {

    /** OpenAI 兼容的对话接口 */
    private static final String CHAT_URL = "https://api.deepseek.com/v1/chat/completions";

    /** 单张图片最大字节数（接口限制 32MB，这里留点余量） */
    private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;

    private static final ObjectMapper JSON = new ObjectMapper();

    /** 下载图片 + 调模型都要等，给足超时 */
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** 分析已有商品图片的提示词模板 */
    private static final String PRODUCT_PROMPT = """
            你是一名资深电商选品与商品文案专家。请仔细观察这张商品图片，结合下面给出的商品信息，
            输出一份简洁的「商品图片分析」：

            商品名称：%s
            商品价格：¥%s
            所属分类：%s

            请按以下结构输出（总字数 200 字以内）。输出纯文本，禁止使用星号、井号等 Markdown 符号，
            分点直接写，结构名用【】包裹：
            【外观特征】从图片中能看到的真实细节（颜色、材质质感、造型、按键/接口/结构等），只写图片里确实存在的，不要编造。
            【核心卖点】3-4 条，结合外观特征推断性能与优势（例如“按键布局紧凑、轴体行程短，响应快，适合电竞操作”）。
            【适用场景/人群】1-2 句，说明适合谁、在什么场景下用。
            【一句话推荐】一句有带货感的推荐语。

            如果图片与给出的商品名称明显不符，请先说明“图片内容与商品名称可能不一致”，再按图片实际内容分析。
            """;

    /** 分析用户上传图片的提示词模板 */
    private static final String UPLOAD_PROMPT = """
            你是一名资深电商选品与商品文案专家。请仔细观察这张用户上传的图片，输出一份「商品图片分析」：

            【商品识别】这是什么商品（品类 + 具体类型），如果图片不是商品图请直接说明。
            【外观特征】只写图片中真实可见的细节：颜色、材质质感、造型结构、按键/接口/配件等，不要编造。
            【核心卖点】3-4 条，结合外观特征推断该商品的性能与优势，
            例如看到游戏鼠标可写“造型贴合掌心、侧键位置合理、轻量化机身，操作响应快，适合电竞对战”。
            【适用场景/人群】1-2 句。
            【一句话推荐】一句有带货感的推荐语。

            总字数 250 字以内。输出纯文本，禁止使用星号、井号等 Markdown 符号，
            分点直接写，结构名用【】包裹。
            """;

    /**
     * 分析用户上传的图片（图片已上传到 OSS，这里是可访问的 URL）
     *
     * @param imageUrl     图片地址
     * @param userQuestion 用户的原始问题，用来补充关注点
     * @return 视觉分析结论；失败时返回可直接展示的中文说明
     */
    public String analyzeUploadedImage(String imageUrl, String userQuestion) {
        String prompt = UPLOAD_PROMPT;
        if (userQuestion != null && !userQuestion.trim().isEmpty()) {
            prompt += "\n用户还特别想知道：" + userQuestion.trim() + "，请在分析中一并回答。";
        }
        return analyzeImage(imageUrl, prompt);
    }

    /**
     * 分析一个已有商品的图片
     *
     * @param product 商品（需要 name / price / categoryName / image）
     * @return 分析结论；没有图片或调用失败时返回可直接展示的中文说明
     */
    public String analyzeProduct(Product product, String focus) {
        if (product == null) {
            return "没有找到要分析的商品。";
        }
        String imageUrl = product.getImage();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return "商品「" + product.getName() + "」还没有上传图片，无法做图片分析。可以先在商品修改里上传一张图片。";
        }

        String prompt = String.format(PRODUCT_PROMPT,
                product.getName(),
                product.getPrice() == null ? "-" : product.getPrice().toPlainString(),
                product.getCategoryName() == null ? "未分类" : product.getCategoryName());
        if (focus != null && !focus.trim().isEmpty()) {
            prompt += "\n另外，用户特别关心：" + focus.trim() + "，请顺带回答。";
        }

        return analyzeImage(imageUrl, prompt);
    }

    /**
     * 通用图片分析：下载图片 → 转 base64 → 交给多模态模型 → 返回文字结论
     */
    public String analyzeImage(String imageUrl, String prompt) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return "没有拿到可分析的图片地址。";
        }
        String key = AiConfig.apiKey();
        if (key == null) {
            return "后端未配置 DEEPSEEK_API_KEY 环境变量，无法做图片分析。";
        }

        String dataUrl;
        try {
            dataUrl = downloadAsDataUrl(imageUrl.trim());
        } catch (Exception e) {
            log.warn("图片下载失败：url={}，原因={}", imageUrl, e.getMessage());
            return "图片读取失败（" + shortMessage(e) + "），请确认图片链接可访问后重试。";
        }

        String model = AiConfig.visionModelName();
        try {
            ObjectNode body = JSON.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.6);
            body.put("max_tokens", 1500);
            // 视觉识别不需要长推理，降低时延与成本
            body.put("reasoning_effort", "low");
            body.put("stream", false);
            ArrayNode messages = body.putArray("messages");
            ArrayNode content = messages.addObject().put("role", "user").putArray("content");
            content.addObject().put("type", "text").put("text", prompt);
            ObjectNode image = content.addObject().put("type", "image_url");
            image.putObject("image_url").put("url", dataUrl).put("detail", "high");

            HttpRequest request = HttpRequest.newBuilder(URI.create(CHAT_URL))
                    .timeout(Duration.ofSeconds(150))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Authorization", "Bearer " + key)
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String raw = response.body() == null ? "" : response.body();
            if (response.statusCode() != 200) {
                log.warn("图片分析接口返回 {}：{}", response.statusCode(), abbreviate(raw, 300));
                return "图片分析失败（接口返回 " + response.statusCode() + "）：" + apiErrorMessage(raw);
            }

            String text = extractText(raw);
            if (text.isEmpty()) {
                log.warn("图片分析返回空内容：{}", abbreviate(raw, 300));
                return "图片分析没有返回结果，请稍后重试。";
            }
            log.info("图片分析成功：model={}，图片={} 字节，结论 {} 字", model, dataUrl.length() * 3 / 4, text.length());
            return text;
        } catch (Exception e) {
            log.warn("图片分析失败：{}", e.toString());
            return "图片分析失败（" + shortMessage(e) + "），请稍后重试。";
        }
    }

    /* ==================== 内部实现 ==================== */

    /** 下载图片并编码为 data URL（模型可直接识别的内联图片） */
    private String downloadAsDataUrl(String imageUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "image/*,*/*")
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("图片链接返回 HTTP " + response.statusCode());
        }
        byte[] bytes = response.body();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("图片内容为空");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalStateException("图片超过 " + (MAX_IMAGE_BYTES / 1024 / 1024) + "MB");
        }
        String mime = detectMime(bytes, imageUrl);
        if (mime == null) {
            throw new IllegalStateException("无法识别的图片格式，请使用 JPG / PNG / WebP");
        }
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    /** 按文件头判断真实图片格式（DeepSeek 只认 JPEG/PNG/GIF/WebP） */
    private String detectMime(byte[] b, String url) {
        if (b.length > 3 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (b.length > 3 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F') {
            return "image/gif";
        }
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        // 兜底按扩展名
        String lower = url.toLowerCase();
        if (lower.contains(".png")) {
            return "image/png";
        }
        if (lower.contains(".jpg") || lower.contains(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.contains(".webp")) {
            return "image/webp";
        }
        if (lower.contains(".gif")) {
            return "image/gif";
        }
        return null;
    }

    /** 取 message.content；部分模型把结论放在 reasoning_content 里时兜底 */
    private String extractText(String raw) {
        try {
            JsonNode root = JSON.readTree(raw);
            JsonNode message = root.path("choices").path(0).path("message");
            String text = message.path("content").asText("").trim();
            if (!text.isEmpty()) {
                return text;
            }
            String reasoning = message.path("reasoning_content").asText("").trim();
            return reasoning.isEmpty() ? "" : reasoning.substring(Math.max(0, reasoning.length() - 600));
        } catch (Exception e) {
            return "";
        }
    }

    /** 从错误响应里取出可读原因 */
    private String apiErrorMessage(String raw) {
        try {
            JsonNode root = JSON.readTree(raw);
            String message = root.path("error").path("message").asText("");
            if (!message.isEmpty()) {
                return message;
            }
        } catch (Exception ignored) {
        }
        return abbreviate(raw, 200);
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String shortMessage(Exception e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        return msg.length() > 120 ? msg.substring(0, 120) + "..." : msg;
    }
}
