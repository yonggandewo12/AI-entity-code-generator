package org.example.demotest.llm.dto;

import java.util.List;

/**
 * OpenAI兼容聊天补全API的请求载荷。
 *
 * @author Liang.Xu
 */
public class LlmChatRequest {

    private String model;

    private List<LlmChatMessage> messages;

    private Double temperature;

    public LlmChatRequest() {
    }

    public LlmChatRequest(String model, List<LlmChatMessage> messages, Double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<LlmChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<LlmChatMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
}
