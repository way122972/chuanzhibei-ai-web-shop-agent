package com.shopguide.ai;

import com.shopguide.config.AiConfig;
import com.shopguide.mapper.CartMapper;
import com.shopguide.mapper.CategoryMapper;
import com.shopguide.mapper.ProductMapper;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 助手会话服务
 *
 * 每条 WebSocket 连接创建一个会话：绑定当前登录用户 + 独立的对话记忆 + 可调用的业务工具。
 * 工具与记忆都按会话隔离，互不干扰。
 */
@Service
public class AiChatService {

    /** 系统提示词：约束模型先查数据再回答、直接执行数据变更类操作 */
    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是「智能电商导购平台」内置的 AI 助手，当前登录用户是 %s（用户ID：%d）。
            你可以通过调用工具完成这些事：
            1. 按价格区间 + 分类查询在售商品；
            2. 新增商品、修改商品、删除商品；
            3. 把商品加入购物车、查看购物车、清空购物车；
            4. 查看当前用户的订单（未支付 / 已支付）。

            请遵守以下规则：
            - 推荐或列举商品前，必须先调用查询工具拿到真实数据，禁止编造商品名称和价格；
            - 用户要求“清空购物车 / 新增 / 修改 / 删除”时，直接调用对应工具执行，然后如实汇报执行结果；
            - 工具返回失败（例如商品名不存在）时，把原因转述给用户，并给出下一步建议；
            - 回答使用简体中文，简洁、分点，价格统一写成 ¥xx.xx；
            - 不要输出工具调用的 JSON 或内部细节，只给用户看结论。
            """;

    @Autowired
    private StreamingChatModel deepSeekStreamingChatModel;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    /** 是否已配置 DeepSeek API Key（未配置时前端给出友好提示） */
    public boolean isConfigured() {
        return AiConfig.apiKey() != null;
    }

    /**
     * 为一次 WebSocket 连接创建 AI 会话
     */
    public AiChatSession newSession(Integer userId, String username) {
        ShopTools tools = new ShopTools(userId, username, productMapper, cartMapper, categoryMapper);
        // 每个会话独立记忆，保留最近 20 条消息，支持多轮追问
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        AiAssistant assistant = AiServices.builder(AiAssistant.class)
                .streamingChatModel(deepSeekStreamingChatModel)
                .chatMemory(memory)
                .tools(tools)
                .systemMessageProvider(memoryId -> String.format(SYSTEM_PROMPT_TEMPLATE, username, userId))
                .build();
        return new AiChatSession(assistant, tools, memory);
    }

    /**
     * 一次会话的运行态：助手代理 + 对话记忆 + 忙碌标记（同一会话串行处理，避免并发流式输出互相干扰）
     */
    public static class AiChatSession {

        private final AiAssistant assistant;
        private final ShopTools tools;
        private final MessageWindowChatMemory memory;
        private final AtomicBoolean busy = new AtomicBoolean(false);

        public AiChatSession(AiAssistant assistant, ShopTools tools, MessageWindowChatMemory memory) {
            this.assistant = assistant;
            this.tools = tools;
            this.memory = memory;
        }

        public AiAssistant assistant() {
            return assistant;
        }

        public ShopTools tools() {
            return tools;
        }

        /** 清空对话记忆（前端点“清空对话”时调用） */
        public void clearMemory() {
            memory.clear();
        }

        /** 尝试占用会话，成功返回 true */
        public boolean tryLock() {
            return busy.compareAndSet(false, true);
        }

        /** 释放会话 */
        public void unlock() {
            busy.set(false);
        }
    }
}
