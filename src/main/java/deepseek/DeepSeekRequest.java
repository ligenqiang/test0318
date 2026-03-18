package deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
/**
 * API 请求模型
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepSeekRequest {
    private String model;
    private double temperature;
    private List<Message> messages=new ArrayList<>();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void setMessages(Message message) {
        this.messages.add(message);
    }
}