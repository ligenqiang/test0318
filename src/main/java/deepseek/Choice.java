package deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 回复选项模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {
    private Message message;

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
