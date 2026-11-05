package com.shopguide.ai;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 助手接口（由 LangChain4j 的 AiServices 动态实现）
 *
 * 返回 TokenStream 表示流式输出：模型每生成一段文字就回调一次，
 * WebSocket 逐段推给前端即可实现「打字机」效果。
 * 工具（Function Calling）通过 AiServices.tools(...) 注入，模型会根据用户问题自行决定调用哪个方法。
 */
public interface AiAssistant {

    TokenStream chat(@UserMessage String message);
}
