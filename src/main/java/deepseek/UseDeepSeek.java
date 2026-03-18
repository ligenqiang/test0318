package deepseek;

public class UseDeepSeek {


    public static void main(String[] args) {
        DeepSeekClient client = new DeepSeekClient();
        String sql = "SELECT * FROM users WHERE age > 30 ORDER BY name LIMIT 10";

    }
}