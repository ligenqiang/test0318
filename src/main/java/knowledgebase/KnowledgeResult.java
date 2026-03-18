package knowledgebase;

import java.util.Map;

// 知识检索结果类
public class KnowledgeResult {
    private final String text;
    private final Map<String, Object> metadata;
    private final float score;

    public KnowledgeResult(String text, Map<String, Object> metadata, float score) {
        this.text = text;
        this.metadata = metadata;
        this.score = score;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "KnowledgeResult{" +
                "text='" + text + '\'' +
                ", score=" + score +
                ", metadata=" + metadata +
                '}';
    }
}