package org.example.demotest.llm.dto;

import java.util.List;

/**
 * OpenAI兼容聊天补全API的响应载荷。
 *
 * @author Liang.Xu
 */
public class LlmChatResponse {

    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public static class Choice {

        private LlmChatMessage message;

        public LlmChatMessage getMessage() {
            return message;
        }

        public void setMessage(LlmChatMessage message) {
            this.message = message;
        }
    }
}
