package com.shopguide.ai;

import dev.langchain4j.service.TokenStream;

/**
 * AI 助手接口（由 LangChain4j 的 AiServices 动态实现）
 *
 * 返回 TokenStream 表示流式输出：模型每生成一段文字就回调一次，
 * WebSocket 逐段推给前端即可实现「打字机」效果。
 * 工具（Function Calling）通过 AiServices.tools(...) 注入，模型会根据用户问题自行决定调用哪个方法。
 *
 * 说明：用户上传图片的场景不直接走这个接口，而是先用 ProductVisionService 做一次视觉分析，
 * 再把分析结论作为上下文交给本接口流式作答（LangChain4j 1.0.x 的 AiServices 对 UserMessage
 * 参数支持不完整，直接传多模态消息会丢失图片内容）。
 */
public interface AiAssistant {

    /** 纯文本提问 */
    TokenStream chat(String message);
}
