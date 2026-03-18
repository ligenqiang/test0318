package knowledgebase;

// 简单的日志记录器实现
public class LoggerFactory {
    public static Logger getLogger(Class<?> clazz) {
        return new Logger(clazz.getName());
    }

}

