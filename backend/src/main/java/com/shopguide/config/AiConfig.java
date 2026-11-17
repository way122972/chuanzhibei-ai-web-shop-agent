package com.shopguide.config;

import com.shopguide.ai.http.Utf8HttpClientBuilder;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 配置：DeepSeek 大模型接入（LangChain4j）
 *
 * DeepSeek 提供 OpenAI 兼容协议，因此直接使用 LangChain4j 的 OpenAi 流式模型，
 * 只需把 baseUrl 指向 https://api.deepseek.com/v1、modelName 指定 deepseek-chat 即可。
 *
 * API Key 不写进代码/配置文件，统一从环境变量 DEEPSEEK_API_KEY 读取。
 */
@Slf4j
@Configuration
public class AiConfig {

    /** 存放 DeepSeek API Key 的环境变量名 */
    public static final String API_KEY_ENV = "DEEPSEEK_API_KEY";

    /** DeepSeek OpenAI 兼容接口地址 */
    private static final String BASE_URL = "https://api.deepseek.com/v1";

    /** 对话模型 */
    private static final String MODEL_NAME = "deepseek-chat";

    /** 读取环境变量中的 API Key，未配置时返回 null */
    public static String apiKey() {
        String key = System.getenv(API_KEY_ENV);
        return (key == null || key.trim().isEmpty()) ? null : key.trim();
    }

    /**
     * 流式对话模型（WebSocket 逐字输出依赖它）
     */
    @Bean
    public StreamingChatModel deepSeekStreamingChatModel() {
        String key = apiKey();
        if (key == null) {
            log.warn("未检测到环境变量 {}，AI 助手将不可用（接口会返回配置提示）", API_KEY_ENV);
            key = "missing-api-key";
        } else {
            log.info("DeepSeek 已接入：baseUrl={}，model={}，apiKey={}******", BASE_URL, MODEL_NAME, key.substring(0, Math.min(6, key.length())));
        }
        return OpenAiStreamingChatModel.builder()
                .baseUrl(BASE_URL)
                .apiKey(key)
                .modelName(MODEL_NAME)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(90))
                // 用 UTF-8 安全的自定义客户端替代默认 JDK 客户端（避免中文工具参数乱码）
                .httpClientBuilder(new Utf8HttpClientBuilder())
                .build();
    }
}
