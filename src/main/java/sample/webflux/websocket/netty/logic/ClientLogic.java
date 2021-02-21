package sample.webflux.websocket.netty.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import sample.webflux.websocket.netty.handler.ClientWebSocketHandler;
import sample.webflux.websocket.netty.handler.WebSocketSessionHandler;

import java.net.URI;
import java.time.Duration;

public class ClientLogic {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Disposable start(WebSocketClient webSocketClient, URI uri, ClientWebSocketHandler clientWebSocketHandler) {
        clientWebSocketHandler
                .connected()
                .subscribe(this::doLogic);

        Disposable clientConnection =
                webSocketClient
                        .execute(uri, clientWebSocketHandler)
                        .subscribeOn(Schedulers.elastic())
                        .subscribe();

        return clientConnection;
    }

    private void doLogic(WebSocketSessionHandler sessionHandler) {
        sessionHandler
                .connected()
                .doOnNext(session -> logger.info("Client Connected [{}]", session.getId()))
                .map(session -> "Test Message")
                .doOnNext(message -> sessionHandler.send(message))
                .subscribe(message -> logger.info("Client Sent: [{}]", message));

        sessionHandler
                .disconnected()
                .subscribe(session -> logger.info("Client Disconnected [{}]", session.getId()));

        Flux<String> sendPing =
                Flux
				.interval(Duration.ofMillis(1000))
				.subscribeOn(Schedulers.elastic())
				.takeUntil(value -> !sessionHandler.isConnected())
				.map(value -> Long.toString(value) + " ping ")
				.doOnNext(message -> sessionHandler.sendPing())
				.doOnNext(message -> logger.info("Client ping Sent: [{}]", message));

        sessionHandler
                .receive()
                .subscribeOn(Schedulers.elastic())
                .doOnNext(message -> logger.info("Client Received: [{}]", message))
                .subscribe();

        sendPing.subscribe();

    }
}
