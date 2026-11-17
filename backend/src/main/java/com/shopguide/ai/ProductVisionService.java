package com.shopguide.ai;

import com.shopguide.entity.Product;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 商品图片视觉分析服务
 *
 * DeepSeek 的对话模型支持图片输入（input_modalities 含 image），这里用「同步模型 + 多模态消息」
 * 做一次性分析：把商品图片 URL 和提示词一起发给模型，拿到文字结论后返回给调用方。
 *
 * 之所以不用流式：工具（Function Calling）的返回值必须是完整字符串，
 * 而用户直接上传图片的场景走的是 WebSocket 流式通道（见 AiWebSocketHandler），两者互不冲突。
 */
@Slf4j
@Service
public class ProductVisionService {

    /** 分析已有商品图片的提示词模板 */
    private static final String PRODUCT_PROMPT = """
            你是一名资深电商选品与商品文案专家。请仔细观察这张商品图片，结合下面给出的商品信息，
            输出一份简洁的「商品图片分析」：

            商品名称：%s
            商品价格：¥%s
            所属分类：%s

            请按以下结构输出（总字数 200 字以内，不要使用 Markdown 标题符号，直接分点写）：
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

            总字数 250 字以内，不要使用 Markdown 标题符号，直接分点写。
            """;

    @Autowired
    private ChatModel deepSeekChatModel;

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
     * 通用图片分析：给定图片 URL 和提示词，返回模型输出的文字结论
     */
    public String analyzeImage(String imageUrl, String prompt) {
        try {
            UserMessage userMessage = new UserMessage(
                    TextContent.from(prompt),
                    // 注意：ImageContent.from(url) 才是「图片地址」重载；
                    // from(String, String) 是 (base64 数据, mimeType)，传 URL 会被当成 base64 解析而报错
                    ImageContent.from(imageUrl));
            ChatResponse response = deepSeekChatModel.chat(userMessage);
            String text = response.aiMessage() == null ? null : response.aiMessage().text();
            if (text == null || text.trim().isEmpty()) {
                return "图片分析没有返回结果，请稍后重试。";
            }
            return text.trim();
        } catch (Exception e) {
            log.warn("商品图片分析失败：{}", e.getMessage());
            return "图片分析失败（" + shortMessage(e) + "），请稍后重试。";
        }
    }

    private String shortMessage(Exception e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        return msg.length() > 120 ? msg.substring(0, 120) + "..." : msg;
    }
}
