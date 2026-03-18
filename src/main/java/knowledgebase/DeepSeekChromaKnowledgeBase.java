package knowledgebase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import deepseek.DeepSeekClient;
import deepseek.DeepSeekResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * DeepSeek与Milvus知识库集成
 * 实现知识库沉淀、相似性检索和智能问答功能
 */
public class DeepSeekChromaKnowledgeBase implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekChromaKnowledgeBase.class);
    // DeepSeek API 端点
    private static final String EMBEDDING_API = "https://api.deepseek.com/v1/embeddings";
    private static final String CHAT_API = "https://api.deepseek.com/v1/chat/completions";
    private static final String api_key = "sk-af385a5272b74148bfc9947469ca73cb";
    private static final String milvus_host = "192.168.1.100";
    private static final int milvus_port = 19530;

    private final String deepSeekApiKey;
    private final String milvusHost;
    private final int milvusPort;
    private final String collectionName;
    private final int embeddingDimension;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;
    private final MyMilvusClient milvusClient; // 新增 MyMilvusClient 实例
    // 最大重试次数和超时设置
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final int CONNECTION_TIMEOUT_MS = 10000;

    // 修复构造函数声明
    public DeepSeekChromaKnowledgeBase(String collectionName, int embeddingDimension, int threadPoolSize,boolean isDropCollection) {
        this.deepSeekApiKey = api_key;
        this.milvusHost = milvus_host;
        this.milvusPort = milvus_port;
        this.collectionName = collectionName;
        // 设置 向量嵌入的维度大小
        this.embeddingDimension = embeddingDimension;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.httpClient = HttpClients.createDefault();
        // 设置线程池大小
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);

        // 初始化 Milvus 客户端
        this.milvusClient = new MyMilvusClient(collectionName, this.embeddingDimension, objectMapper, httpClient, executorService);
        // 初始化集合
        this.milvusClient.initMilvusCollection(isDropCollection);

    }


    // 将知识添加到知识库
    public void addKnowledge(String content, Map<String, Object> metadata) throws IOException {
        float[] embedding = getEmbedding(content);
        Float[] floatEmbedding = convertToFloatArray(embedding);

        String url = buildMilvusApiUrl("/vectors/insert");
        Map<String, Object> request = new HashMap<>();
        request.put("collection_name", collectionName);

        List<Map<String, Object>> records = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("embedding", Arrays.stream(floatEmbedding).collect(Collectors.toList()));
        record.put("content", content);
        record.put("metadata", metadata);
        records.add(record);

        request.put("records", records);

        executeMilvusRequest(url, request);
        logger.info(String.format("已添加知识到知识库: %s, 摘要", content.substring(0, Math.min(50, content.length()))));
    }

    // 批量添加知识
    public void addKnowledgeBatch(List<String> contents, List<Map<String, Object>> metadatas) throws IOException {
        if (contents.size() != metadatas.size()) {
            throw new IllegalArgumentException("内容列表和元数据列表长度必须一致");
        }

        List<float[]> embeddings = new ArrayList<>();
        for (String content : contents) {
            embeddings.add(getEmbedding(content));
        }

        String url = buildMilvusApiUrl("/vectors/insert");
        Map<String, Object> request = new HashMap<>();
        request.put("collection_name", collectionName);

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            Map<String, Object> record = new HashMap<>();
            Float[] floatEmbedding = convertToFloatArray(embeddings.get(i));
            record.put("embedding", Arrays.stream(floatEmbedding).collect(Collectors.toList()));
            record.put("content", contents.get(i));
            record.put("metadata", metadatas.get(i));
            records.add(record);
        }

        request.put("records", records);

        executeMilvusRequest(url, request);
        logger.info(String.format("已批量添加 %d 条知识到知识库: 操作完成", contents.size()));

    }

    // 检索相关知识
    public List<KnowledgeItem> retrieveKnowledge(String query, int topK, String filter) throws IOException {
        float[] queryEmbedding = getEmbedding(query);
        Float[] floatQueryEmbedding = convertToFloatArray(queryEmbedding);

        String url = buildMilvusApiUrl("/vectors/search");
        Map<String, Object> request = new HashMap<>();
        request.put("collection_name", collectionName);
        request.put("vector", Arrays.stream(floatQueryEmbedding).collect(Collectors.toList()));
        request.put("topk", topK);
        request.put("metric_type", "IP");
        request.put("params", "{\"ef\":50}");
        request.put("output_fields", Arrays.asList("content", "metadata"));

        if (filter != null && !filter.isEmpty()) {
            request.put("filter", filter);
        }

        String response = executeMilvusRequest(url, request);
        return parseSearchResults(response);
    }

    // 基于知识库回答问题
    public String answerQuestion(String question, int topK, String filter) throws IOException {
        // 检索相关知识
        List<KnowledgeItem> relatedKnowledge = retrieveKnowledge(question, topK, filter);

        // 构建提示
        String prompt = buildPrompt(question, relatedKnowledge);

        // 调用DeepSeek生成答案
        return generateAnswer(prompt);
    }

    // 异步回答问题
    public CompletableFuture<String> answerQuestionAsync(String question, int topK, String filter) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return answerQuestion(question, topK, filter);
            } catch (Exception e) {
                logger.error("异步回答问题失败", e);
                throw new CompletionException(e);
            }
        }, executorService);
    }

    // 获取文本嵌入向量
    private float[] getEmbedding(String text) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "deepseek-embedding");
        request.put("input", text);

        String response = executeDeepSeekRequest(EMBEDDING_API, request);

        try {
            EmbeddingResponse embeddingResponse = objectMapper.readValue(response, EmbeddingResponse.class);
            if (embeddingResponse.getData() != null && !embeddingResponse.getData().isEmpty()) {
                return embeddingResponse.getData().get(0).getEmbedding();
            }
            throw new IOException("未能获取有效的嵌入向量");
        } catch (Exception e) {
            logger.error("解析嵌入响应失败: {}", response);
            throw new IOException("解析嵌入响应失败", e);
        }
    }

    // 生成答案
    private String generateAnswer(String prompt) throws IOException {
        Map<String, Object> request = new HashMap<>();
        request.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        request.put("messages", messages);
        request.put("temperature", 0.3);
        request.put("max_tokens", 1024);

        String response = executeDeepSeekRequest(CHAT_API, request);

        try {
            ChatResponse chatResponse = objectMapper.readValue(response, ChatResponse.class);
            if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                return chatResponse.getChoices().get(0).getMessage().getContent();
            }
            throw new IOException("未能获取有效的回答");
        } catch (Exception e) {
            logger.error("解析聊天响应失败: {}", response);
            throw new IOException("解析聊天响应失败", e);
        }
    }

    // 构建提示
    private String buildPrompt(String question, List<KnowledgeItem> knowledgeItems) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("知识库内容：\n");

        for (KnowledgeItem item : knowledgeItems) {
            prompt.append("- ").append(item.getContent()).append("\n");
            if (item.getMetadata() != null && !item.getMetadata().isEmpty()) {
                prompt.append("  (来源: ").append(item.getMetadata().getOrDefault("source", "未知")).append(")\n");
            }
        }

        prompt.append("\n用户问题：").append(question).append("\n");
        prompt.append("请基于上述知识库内容，提供专业、准确的回答：");

        return prompt.toString();
    }

    // 执行DeepSeek API请求
    private String executeDeepSeekRequest(String url, Map<String, Object> requestBody) throws IOException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
//                DeepSeekClient deepSeekClient = new DeepSeekClient();
//                DeepSeekResponse response = deepSeekClient.analyzeSql(1,sqlQuery);
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + deepSeekApiKey);

                StringEntity entity = new StringEntity(
                        objectMapper.writeValueAsString(requestBody),
                        StandardCharsets.UTF_8
                );
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode == 200) {
                        return responseBody;
                    } else {
                        logger.warn(String.format("DeepSeek API请求失败 (尝试 %d): HTTP %d, %s", attempt, statusCode, responseBody));
                        if (attempt == MAX_RETRIES) {
                            throw new IOException("DeepSeek API请求失败: " + statusCode);
                        }
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn(String.format("DeepSeek API请求异常 (尝试 %d): %s", attempt, e.getMessage()));
                if (attempt == MAX_RETRIES) {
                    throw new IOException("DeepSeek API请求失败", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IOException("DeepSeek API请求达到最大重试次数");
    }

    // 执行Milvus API请求
    private String executeMilvusRequest(String url, Map<String, Object> requestBody) throws IOException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");

                StringEntity entity = new StringEntity(
                        objectMapper.writeValueAsString(requestBody),
                        StandardCharsets.UTF_8
                );
                httpPost.setEntity(entity);

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode == 200) {
                        return responseBody;
                    } else {
                        logger.warn(String.format("Milvus API请求失败 (尝试 %d): HTTP %d, %s", attempt, statusCode, responseBody));
                        if (attempt == MAX_RETRIES) {
                            throw new IOException("Milvus API请求失败: " + statusCode);
                        }
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn(String.format("Milvus API请求异常 (尝试 %d): %s", attempt, e.getMessage()));
                if (attempt == MAX_RETRIES) {
                    throw new IOException("Milvus API请求失败", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IOException("Milvus API请求达到最大重试次数");
    }

    // 解析Milvus响应中的特定值
    private String parseMilvusResponseValue(String response, String key) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path(key).asText();
        } catch (Exception e) {
            logger.error("解析Milvus响应失败: {}", response);
            return null;
        }
    }

    // 解析搜索结果
    private List<KnowledgeItem> parseSearchResults(String response) {
        List<KnowledgeItem> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode resultsNode = root.path("results");

            if (resultsNode.isArray()) {
                for (JsonNode result : resultsNode) {
                    String content = result.path("content").asText();
                    float score = (float) result.path("distance").asDouble();

                    Map<String, Object> metadata = new HashMap<>();
                    JsonNode metadataNode = result.path("metadata");
                    if (!metadataNode.isMissingNode() && metadataNode.isObject()) {
                        metadata = objectMapper.convertValue(metadataNode, Map.class);
                    }

                    results.add(new KnowledgeItem(content, metadata, score));
                }
            }
        } catch (Exception e) {
            logger.error("解析搜索结果失败: {}", response);
        }

        return results;
    }

    // 构建Milvus API URL
    private String buildMilvusApiUrl(String path) {
        return String.format("http://%s:%d/api/v1%s", milvusHost, milvusPort, path);
    }

    // 关闭资源
    @Override
    public void close() {
        logger.info(String.format("关闭DeepSeek知识库资源: %s, 开始", "开始"));

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                logger.error("关闭HTTP客户端失败", e);
            }
        }
    }

    private Float[] convertToFloatArray(float[] primitive) {
        Float[] result = new Float[primitive.length];
        for (int i = 0; i < primitive.length; i++) {
            result[i] = primitive[i];
        }
        return result;
    }

    // 使用示例
    public static void main(String[] args) {
        boolean isDropCollection=true;
        try (DeepSeekChromaKnowledgeBase kb = new DeepSeekChromaKnowledgeBase(
                "knowledge_base", // 集合名称
                1536, // 根据 DeepSeek 模型的向量维度调整
                5 ,// 线程池大小
                isDropCollection // 是否删除原有集合
        )) {
            // 沉淀知识库
            Map<String, Object> metadata1 = new HashMap<>();
            metadata1.put("source", "SQL优化指南");
            metadata1.put("category", "数据库");

            Map<String, Object> metadata2 = new HashMap<>();
            metadata2.put("source", "Java编程规范");
            metadata2.put("category", "编程");

            kb.addKnowledge("SQL优化的关键是创建合适的索引", metadata1);
            kb.addKnowledge("复合索引适用于多条件查询", metadata1);
            kb.addKnowledge("避免在WHERE子句中使用函数，会导致索引失效", metadata1);
            kb.addKnowledge("Java中应该优先使用接口而不是具体类", metadata2);
            kb.addKnowledge("使用try-with-resources自动关闭资源", metadata2);

            // 提问
            String question = "如何优化SQL查询？";
            String answer = kb.answerQuestion(question, 3, "category == \"数据库\"");

            System.out.println("问题：" + question);
            System.out.println("答案：" + answer);

            // 异步提问示例
            CompletableFuture<String> future = kb.answerQuestionAsync(
                    "Java中有哪些自动资源管理的方法？",
                    2,
                    "category == \"编程\""
            );

            future.thenAccept(ans -> {
                System.out.println("\n异步问题：Java中有哪些自动资源管理的方法？");
                System.out.println("异步答案：" + ans);
            }).exceptionally(ex -> {
                System.err.println("异步查询失败：" + ex.getMessage());
                return null;
            });

            // 等待异步操作完成
            Thread.sleep(3000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}