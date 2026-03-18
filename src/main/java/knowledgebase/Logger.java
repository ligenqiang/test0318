package knowledgebase;

public class Logger {
    private final String name;

    public Logger(String name) {
        this.name = name;
    }

    public void info(String message) {
        System.out.printf("[INFO] [%s] %s%n", name, message);
    }

    public void warn(String message) {
        System.err.printf("[WARN] [%s] %s%n", name, message);
    }

    public void warn(String message,int attempt, int statusCode, String responseBody) {
        System.err.printf("[WARN] [%s] %s%n", name, message);
    }

    public void warn(String message,int attempt,  String responseBody) {
        System.err.printf("[WARN] [%s] %s%n", name, message);
    }

    public void error(String message, Throwable t) {
        System.err.printf("[ERROR] [%s] %s%n", name, message);
        t.printStackTrace();
    }
    public void error(String message, String t) {
        System.err.printf("[ERROR] [%s] %s%n", name, message);
        System.out.println( t);;
    }

    public void debug(String message) {
        System.out.printf("[DEBUG] [%s] %s%n", name, message);
    }
}