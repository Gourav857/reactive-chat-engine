package com.enterprise.chat.engine.handler;

import com.enterprise.chat.engine.model.ChatMessage;
import com.enterprise.chat.engine.model.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ReactiveChatHandler implements WebSocketHandler {

    private static final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReactiveChatHandler(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        System.out.println("⚡ Session Bound -> " + sessionId);

        final String[] sessionRoom = {"101"};
        final String[] sessionUser = {"Unknown"};

        // 1. INBOUND: Process and Database Push
        Mono<Void> inputPipeline = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    try {
                        ChatMessage msg = objectMapper.readValue(payload, ChatMessage.class);
                        sessionRoom[0] = msg.getRoomId();
                        sessionUser[0] = msg.getSender();

                        String channelName = "ROOM_" + sessionRoom[0];
                        String dbKey = "CHAT_HISTORY_" + sessionRoom[0];

                        if (msg.getType() == MessageType.CHAT) {
                            // Save directly as plain string value into Redis List Database
                            return redisTemplate.opsForList().rightPush(dbKey, payload)
                                    .then(redisTemplate.convertAndSend(channelName, payload));
                        }
                        return redisTemplate.convertAndSend(channelName, payload);
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .then();

        // 2. OUTBOUND: FIXING THE HISTORY DB CASTING
        Mono<Void> outputPipeline = Mono.defer(() -> {
            String channelName = "ROOM_" + sessionRoom[0];
            String dbKey = "CHAT_HISTORY_" + sessionRoom[0];

            // Fetching and strictly casting objects to pure Text Messages for the browser
            Flux<WebSocketMessage> historyFlux = redisTemplate.opsForList().range(dbKey, 0, 49)
                    .map(historyString -> session.textMessage(String.valueOf(historyString)));

            // Real-time listener pool
            Flux<WebSocketMessage> realTimeFlux = redisTemplate.listenTo(ChannelTopic.of(channelName))
                    .map(reactiveMessage -> session.textMessage(reactiveMessage.getMessage()));

            // Continuous stream connection
            return session.send(Flux.concat(historyFlux, realTimeFlux));
        });

        // 3. CLEANUP DISCONNECT
        return Mono.zip(inputPipeline, outputPipeline)
                .doOnTerminate(() -> {
                    activeSessions.remove(sessionId);
                    System.out.println("❌ Session Terminated Cleanly -> " + sessionId);

                    if (!"Unknown".equals(sessionUser[0])) {
                        try {
                            ChatMessage leftMsg = new ChatMessage();
                            leftMsg.setType(MessageType.LEAVE);
                            leftMsg.setSender(sessionUser[0]);
                            leftMsg.setRoomId(sessionRoom[0]);
                            leftMsg.setContent(sessionUser[0] + " left the room");

                            String payload = objectMapper.writeValueAsString(leftMsg);
                            redisTemplate.convertAndSend("ROOM_" + sessionRoom[0], payload).subscribe();
                        } catch (Exception ignored) {}
                    }
                })
                .then();
    }
}