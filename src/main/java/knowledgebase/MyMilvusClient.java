package knowledgebase;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.grpc.*;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.LoadCollectionRequest;
import io.milvus.grpc.MilvusServiceGrpc;
import io.milvus.grpc.Status;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
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

public class MyMilvusClient {
    
    private final String collectionName;
    private final int embeddingDimension;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;
    private final MilvusClient milvusClient; // 新增 MyMilvusClient 实例
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekChromaKnowledgeBase.class);
    private static final String milvus_host = "192.168.1.100";
    private static final int milvus_port = 19530;
    private static final String fieldName="feature_vector";

    public MyMilvusClient(String collectionName,
                          int embeddingDimension,
                          ObjectMapper objectMapper,
                          CloseableHttpClient httpClient,
                          ExecutorService executorService
    ) {
        this.collectionName = collectionName;
        this.embeddingDimension = embeddingDimension;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.executorService = executorService;
        this.milvusClient = createMilvusClient();
    }

    public static MilvusClient createMilvusClient() {
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(milvus_host)
                        .withPort(milvus_port)
                         .build()
        );
        return client;
    }

    // 初始化Milvus集合
    public void initMilvusCollection(boolean isDropCollection) {
        try {
            // 检查集合是否存在
            boolean exists = checkCollectionExists();
            if (!exists) {
                createCollection();
                createIndex();
                loadCollection();
                logger.info(String.format("成功创建并初始化Milvus集合: %s, 状态", this.collectionName));
            } else {
                if(isDropCollection){
                    dropCollection(this.collectionName);
                    createCollection();
                    createIndex();
                    loadCollection();
                    logger.info(String.format("成功创建并初始化Milvus集合: %s, 状态", this.collectionName));
                }else {
                    loadCollection();
                    logger.info(String.format("已加载现有Milvus集合: %s, 信息", this.collectionName));
                }

            }
        } catch (Exception e) {
            logger.error("初始化Milvus集合失败", e);
            throw new RuntimeException("初始化知识库失败", e);
        }
    }

    // 检查集合是否存在
    private boolean checkCollectionExists() throws IOException {
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(this.collectionName)
                .build();

        R<Boolean> response = this.milvusClient.hasCollection(hasCollectionParam);
        if (response != null && response.getData() != null) {
            return response.getData();
        } else {
            throw new IOException("无法确认集合是否存在: " + response.getMessage());
        }
    }

    // 检查集合是否存在
    private boolean checkCollectionExists(String collectionName) throws IOException {
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        R<Boolean> response = this.milvusClient.hasCollection(hasCollectionParam);
        if (response != null && response.getData() != null) {
            return response.getData();
        } else {
            throw new IOException("无法确认集合是否存在: " + response.getMessage());
        }
    }


    // 创建集合
    private void createCollection() throws IOException {

        // 1. 定义主键字段 (必须)
        FieldType primaryKeyField = FieldType.newBuilder()
                .withName("id")                   // 主键字段名
                .withDataType(DataType.Int64)     // 数据类型，支持 Int64 或 VarChar
                .withPrimaryKey(true)             // 设置为主键
                .withAutoID(true)                 // 自动生成 ID
                .build();

        // 2. 定义向量字段
        FieldType fieldType = FieldType.newBuilder()
                .withName(fieldName)
                .withDataType(DataType.FloatVector)
                .withDimension(128)
                .build();

        // 3. 创建集合参数，包含主键和向量字段
        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldTypes(Arrays.asList(primaryKeyField, fieldType))
                .withShardsNum(2)  // 设置分片数
                .build();

        R<RpcStatus> response = this.milvusClient.createCollection(createParam);
        // 检查响应是否成功
        if (response == null || response.getStatus() != R.Status.Success.getCode()) {
            throw new IOException("创建集合请求失败: " +
                    (response != null ? response.getMessage() : "未知错误"));
        }

        // 检查 RPC 状态
        RpcStatus status = response.getData();
        if (status == null || !status.getMsg().equals("Success")) {
            throw new IOException("创建集合失败: " +
                    (status != null ? status.getMsg() : "未知错误"));
        }

        System.out.println("集合 " + collectionName + " 创建成功");

    }


    // 创建索引
    private void createIndex() throws IOException {

        IndexType indexType = IndexType.IVF_FLAT;
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(this.collectionName)
                .withFieldName(fieldName)
                .withIndexType(indexType)
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\":1024}")
                .build();

        R<RpcStatus> response = this.milvusClient.createIndex(indexParam);
        // 检查响应是否成功
        if (response == null || response.getStatus() != R.Status.Success.getCode()) {
            throw new IOException("创建索引请求失败: " +
                    (response != null ? response.getMessage() : "未知错误"));
        }

        // 检查 RPC 状态
        RpcStatus status = response.getData();
        if (status == null || !status.getMsg().equals("Success")) {
            throw new IOException("创建索引失败: " +
                    (status != null ? status.getMsg() : "未知错误"));
        }
        System.out.println("索引创建成功");
        this.milvusClient.createIndex(indexParam);
    }


    // 加载集合
    /**
     * @description: 该方法会引起  protobuf 冲突 ，弃用

     * @return void
     */

    private void loadCollection() throws IOException {
        LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
                .withCollectionName(this.collectionName)
                .build();

        R<RpcStatus> response = this.milvusClient.loadCollection(loadCollectionParam);
        // 检查响应是否成功
        if (response == null || response.getStatus() != R.Status.Success.getCode()) {
            throw new IOException("加载集合请求失败: " +
                    (response != null ? response.getMessage() : "未知错误"));
        }

        // 检查 RPC 状态
        RpcStatus status = response.getData();
        if (status == null || !status.getMsg().equals("Success")) {
            throw new IOException("加载集合失败: " +
                    (status != null ? status.getMsg() : "未知错误"));
        }
        System.out.println("集合 " + collectionName + " 加载成功");
    }


    // 直接调用 gRPC
    public void loadCollectionOld() {

        // 1. 创建 gRPC Channel
        ManagedChannel channel = ManagedChannelBuilder.forAddress(milvus_host, milvus_port)
                .usePlaintext()
                .build();

        try {
            // 2. 创建 gRPC Stub
            MilvusServiceGrpc.MilvusServiceBlockingStub blockingStub =
                    MilvusServiceGrpc.newBlockingStub(channel);

            // 3. 构造 LoadCollection 请求
            LoadCollectionRequest request = LoadCollectionRequest.newBuilder()
                    .setCollectionName(this.collectionName)
                    .build();

            // 4. 发送请求
            Status response = blockingStub.loadCollection(request);

            // 5. 检查响应
            if (response.getCode() != 0) {
                System.err.println("加载失败: " + response.getReason());
            } else {
                System.out.println("✅ 集合加载成功！");
            }
        } finally {
            // 6. 关闭 Channel（避免资源泄漏）
            channel.shutdown();
        }

    }


    // 删除指定名称的集合
    public void dropCollection(String collectionName) throws IOException {
        // 检查集合是否存在
        if (!checkCollectionExists(collectionName)) {
            System.out.println("集合 " + collectionName + " 不存在，无需删除");
            return;
        }

        // 构建删除集合的参数
        DropCollectionParam dropParam = DropCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();

        // 执行删除操作
        R<RpcStatus> response = milvusClient.dropCollection(dropParam);

        // 检查响应状态
        if (response == null || response.getStatus() != R.Status.Success.getCode()) {
            throw new IOException("删除集合请求失败: " +
                    (response != null ? response.getMessage() : "未知错误"));
        }

        RpcStatus status = response.getData();
        if (status == null || !status.getMsg().equals("Success")) {
            throw new IOException("删除集合失败: " +
                    (status != null ? status.getMsg() : "未知错误"));
        }

        System.out.println("集合 " + collectionName + " 删除成功");
    }

}