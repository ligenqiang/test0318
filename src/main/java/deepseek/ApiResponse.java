package deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * API 响应模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ApiResponse {
    private List<Choice> choices;
    private Usage usage;

    private List<EmbeddingData> data;

    public List<EmbeddingData> getData() {
        return data;
    }

    public void setData(List<EmbeddingData> data) {
        this.data = data;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }
}