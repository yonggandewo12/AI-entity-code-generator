package org.example.demotest.llm.dto;

/**
 * OpenAI兼容请求/响应载荷的聊天消息。
 *
 * @author Liang.Xu
 */
public class LlmChatMessage {

    private String role;

    private String content;

    public LlmChatMessage() {
    }

    public LlmChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
