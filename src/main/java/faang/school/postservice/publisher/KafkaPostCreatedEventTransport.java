package faang.school.postservice.publisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "notifications",
        name = "transport",
        havingValue = "kafka"
)
public class KafkaPostCreatedEventTransport implements PostCreatedEventTransport {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public KafkaPostCreatedEventTransport(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${notifications.kafka.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void send(String key, String json) {
        kafkaTemplate.send(topic, key, json).join();
    }
}