package com.enterprise.chat.engine.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class ReactiveChatHandler implements WebSocketHandler {

    // Global pipeline sink jo saare concurrent connections me message broadcast karega
    private final Sinks.Many<String> chatSink = Sinks.many().multicast().directBestEffort();

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        // 1. RECEIVE PIPELINE: Frontend se message receive karna aur use sink me push karna
        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(chatSink::tryEmitNext) // Har message ko sink me broadcast ke liye daalna
                .doOnError(e -> System.err.println("Pipeline Sync Error: " + e.getMessage()))
                .then();

        // 2. TRANSMIT PIPELINE: Sink se message uthakar saare connected browsers ko wapas bhejnah
        Flux<WebSocketMessage> output = chatSink.asFlux()
                .map(session::textMessage);

        // Dono pipelines ko merge karke run karna
        return Mono.zip(input, session.send(output)).then();
    }
}