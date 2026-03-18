package knowledgebase;

import java.io.*;

public class EmbeddingGenerator {
    public static float[] generateEmbedding(String text) throws IOException {
        // 准备Python脚本调用
        ProcessBuilder pb = new ProcessBuilder("python", "D:\\PycharmProjects\\deepseekknowledgebase\\deepseekknowledgebase\\generate_embedding.py", text);
        Process process = pb.start();

        // 读取Python输出
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        // 解析向量（示例格式："0.1,0.2,0.3,..."）
        String[] parts = output.toString().split(",");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            embedding[i] = Float.parseFloat(parts[i]);
        }
        return embedding;
    }
}