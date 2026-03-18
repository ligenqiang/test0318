package deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 错误响应模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ErrorResponse {
    private Error error;

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}