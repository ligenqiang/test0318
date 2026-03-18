package deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 错误信息模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Error {
    private String message;
    private String type;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}