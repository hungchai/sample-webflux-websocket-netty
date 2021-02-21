package sample.webflux.websocket.netty.logic;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import sample.webflux.websocket.netty.handler.ServerWebSocketHandler;
import sample.webflux.websocket.netty.handler.WebSocketSessionHandler;

public class ServerLogic 
{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public void start(ServerWebSocketHandler serverWebSocketHandler, long interval)
	{
		serverWebSocketHandler
			.connected()
			.subscribe(sessionHandler -> doLogic(sessionHandler, interval));
	}
	
	private void doLogic(WebSocketSessionHandler sessionHandler, long interval)
	{
		sessionHandler
			.connected()
			.subscribe(session -> logger.info("Server Connected [{}]", session.getId()));
		
		sessionHandler
			.disconnected()
			.subscribe(session -> logger.info("Server Disconnected [{}]", session.getId()));
		
		Flux<String> receiveAll =
			sessionHandler
				.receive()				
				.subscribeOn(Schedulers.elastic())
				.doOnNext(message -> logger.info("Server Received: [{}]", message));
		
		Mono<String> receiveFirst =
			sessionHandler
				.receive()
				.subscribeOn(Schedulers.elastic())
				.next();
		
		Flux<String> send =
			Flux
				.interval(Duration.ofMillis(interval))
				.subscribeOn(Schedulers.elastic())				
				.takeUntil(value -> !sessionHandler.isConnected())
				.map(value -> Long.toString(value))
				.doOnNext(message -> sessionHandler.send(message))
				.doOnNext(message -> logger.info("Server Sent: [{}]", message));
		
		receiveAll.subscribe();
		receiveFirst.thenMany(send).subscribe();
	}
}
