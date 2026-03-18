package knowledgebase;

import java.util.Map;

// 知识项类
public class KnowledgeItem {
    private final String content;
    private final Map<String, Object> metadata;
    private final float score;

    public KnowledgeItem(String content, Map<String, Object> metadata, float score) {
        this.content = content;
        this.metadata = metadata;
        this.score = score;
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public float getScore() {
        return score;
    }

    @Override
    public String toString() {
        return "KnowledgeItem{" +
                "content='" + content + '\'' +
                ", score=" + score +
                ", metadata=" + metadata +
                '}';
    }
}