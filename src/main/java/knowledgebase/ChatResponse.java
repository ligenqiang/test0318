package knowledgebase;
import deepseek.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// DeepSeek聊天响应模型
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatResponse {
    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }
}