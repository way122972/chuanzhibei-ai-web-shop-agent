package com.shopguide.ai.http;

import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;

import java.time.Duration;

/**
 * {@link Utf8HttpClient} 的构建器，交给 LangChain4j 的模型 Builder 使用：
 *   OpenAiStreamingChatModel.builder().httpClientBuilder(new Utf8HttpClientBuilder())
 */
public class Utf8HttpClientBuilder implements HttpClientBuilder {

    private Duration connectTimeout = Duration.ofSeconds(15);
    private Duration readTimeout = Duration.ofSeconds(120);

    @Override
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public HttpClientBuilder readTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    @Override
    public HttpClient build() {
        return new Utf8HttpClient(connectTimeout, readTimeout);
    }
}
