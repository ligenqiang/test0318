package deepseek;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DeepSeekResponse {
    @JsonProperty("optimized_sql")
    private String optimizedSql;
    @JsonProperty("analysis_points")
    private List<String> analysis;
    @JsonProperty("optimization_suggestions")
    private List<String> suggestions;
    private boolean success;
    private List<String> errors;
    private String rawResponse;

    public DeepSeekResponse() {
        this.analysis = new ArrayList<>();
        this.suggestions = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.success = true;
    }

    public String getOptimizedSql() {
        return optimizedSql;
    }

    public void setOptimizedSql(String optimizedSql) {
        this.optimizedSql = optimizedSql;
    }

    public List<String> getAnalysis() {
        return analysis;
    }

    public void setAnalysis(List<String> analysis) {
        this.analysis = analysis;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (success) {
            sb.append("分析成功**************:\n");
            if (optimizedSql != null && !optimizedSql.isEmpty()) {
                sb.append("优化后的 SQL**************:\n").append(optimizedSql).append("\n\n");
            }
            if (!analysis.isEmpty()) {
                sb.append("分析要点***************:\n");
                for (String point : analysis) {
                    sb.append("- ").append(point).append("\n");
                }
                sb.append("\n");
            }
            if (!suggestions.isEmpty()) {
                sb.append("优化建议:\n");
                for (String suggestion : suggestions) {
                    sb.append("- ").append(suggestion).append("\n");
                }
            }
        } else {
            sb.append("分析失败:\n");
            for (String error : errors) {
                sb.append("- ").append(error).append("\n");
            }
            if (rawResponse != null && !rawResponse.isEmpty()) {
                sb.append("\n原始响应:\n").append(rawResponse);
            }
        }
        return sb.toString();
    }

    public static DeepSeekResponse fromText(String content) {
        DeepSeekResponse response = new DeepSeekResponse();
        response.getAnalysis().add(content);


        return response;
    }
}