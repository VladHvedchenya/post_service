package faang.school.postservice.publisher;

public interface PostCreatedEventTransport {

    void send(String key, String json);
}
