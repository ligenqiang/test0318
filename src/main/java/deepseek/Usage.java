package deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
class Usage {
    private int total_tokens;
    @JsonProperty("total_tokens")
    public int getTotalTokens() {
        return total_tokens;
    }

    public void setTotalTokens(int total_tokens) {
        this.total_tokens = total_tokens;
    }
}