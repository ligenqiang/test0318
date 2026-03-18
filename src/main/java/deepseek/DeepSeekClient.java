package deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions"; // 根据实际API地址修改
    private static final String EMBEDDING_API = "https://api.deepseek.com/v1/embeddings";
    private static final String API_KEY = "sk-af385a5272b74148bfc9947469ca73cb";

    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 分析 SQL 查询并获取优化建议
     * @param sqlQuery 待分析的 SQL 查询
     * @return 包含分析结果的 SQL 分析响应对象
     * @throws IOException 如果请求过程中发生错误
     */
    public DeepSeekResponse analyzeSql(int type,int requestType,String sqlQuery) throws IOException {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            throw new IllegalArgumentException("问题为空");
        }

        // 构建 API 请求
        String requestBody = buildRequest(requestType,type,sqlQuery);
        String responseJson = sendRequest(requestType,requestBody);

        // 解析响应
        return parseResponse(responseJson);
    }

    private String buildRequest(int requestType,int type,String sqlQuery) throws IOException {
        // 构建请求体
        DeepSeekRequest request = new DeepSeekRequest();
        if(requestType==1){
            request.setModel("deepseek-chat");
        }else {
            request.setModel("text-embedding-3-large");
        }

        request.setTemperature(0.7);

        // 设置消息内容
        Message message = new Message();
        message.setRole("user");
        message.setContent(AnalyzeContent.analyze(type,sqlQuery));

        request.setMessages(message);

        // 转换为 JSON
        return objectMapper.writeValueAsString(request);
    }

    private String sendRequest(int requestType,String requestBody) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost =null;
        if(requestType==1){
            httpPost = new HttpPost(API_URL);
        }else if(requestType==2){
            httpPost = new HttpPost(EMBEDDING_API);
        }else {
            throw new IOException("无效的请求类型");
        }


        // 设置请求头
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + API_KEY);

        // 设置请求体
        httpPost.setEntity(new StringEntity(requestBody, "UTF-8"));

        // 执行请求
        HttpResponse response = httpClient.execute(httpPost);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();

        if (entity == null) {
            throw new IOException("响应体为空，状态码: " + statusCode);
        }

        String responseJson = EntityUtils.toString(entity, "UTF-8");

        // 检查状态码
        if (statusCode == 200) {
            return responseJson;
        } else {
            // 解析错误响应
            ErrorResponse errorResponse = objectMapper.readValue(responseJson, ErrorResponse.class);
            throw new IOException("API 请求失败 [" + statusCode + "]: " + errorResponse.getError().getMessage());
        }
    }

    private DeepSeekResponse parseResponse(String responseJson) throws IOException {
        // 解析 API 响应
        ApiResponse apiResponse = objectMapper.readValue(responseJson, ApiResponse.class);

        if (apiResponse.getChoices() == null || apiResponse.getChoices().isEmpty()) {
            throw new IOException("无效的 API 响应: 缺少 choices 字段");
        }

        // 获取第一个回复
        Choice choice = apiResponse.getChoices().get(0);
        String content = choice.getMessage().getContent();
        if(content.contains("```json")){
            content=content.replace("```json","").replace("```","");
        }else {
            return DeepSeekResponse.fromText(content);
        }

        try {
            // 尝试将回复内容解析为 SQL 分析结果
            return objectMapper.readValue(content, DeepSeekResponse.class) ;
        } catch (Exception e) {
            // 如果无法解析为 JSON，直接返回原始内容
            DeepSeekResponse fallbackResponse = new DeepSeekResponse();
            fallbackResponse.setRawResponse(content);
            fallbackResponse.setSuccess(false);
            fallbackResponse.getErrors().add("无法解析结构化结果，返回原始响应");
            return fallbackResponse;
        }
    }

    // 使用示例
    public static void main(String[] args) {

        // 示例 SQL 查询
        int type= 1 ;
        int requestType= 1 ;
        String sqlQuery = "地球周长是多少";
        sqlQuery = "select * from kpi_codes where id is not null limit 10";

        try {

            // 分析 SQL
            DeepSeekClient deepSeekClient = new DeepSeekClient();
            DeepSeekResponse response = deepSeekClient.analyzeSql(type,requestType,sqlQuery);

            // 输出结果
            System.out.println(response);

        } catch (Exception e) {
            System.err.println("SQL 分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}