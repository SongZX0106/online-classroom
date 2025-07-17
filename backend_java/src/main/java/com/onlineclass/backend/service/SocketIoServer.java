package com.onlineclass.backend.service;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;


/**
 * @author songzx
 * @create 2025-07-17 11:00
 */
@Log4j2
@Component
public class SocketIoServer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Object> users = new HashMap<>();
    private final Map<String, Object> rooms = new HashMap<>();

    private SocketIOServer server;

    @PostConstruct
    public void startServer() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3000);
        server = new SocketIOServer(config);
        addHandlers();
        server.start();
    }

    private void addHandlers() {
        // 连接事件
        server.addConnectListener(this::onConnect);
        // 断开事件
        server.addDisconnectListener(this::onDisconnect);
        // 监听创建房间
        server.addEventListener("create-room", String.class, this::handleCreateRoom);
    }

    private void onConnect(SocketIOClient client) {
        log.info("已连接: " + client.getSessionId());
    }

    private void onDisconnect(SocketIOClient client) {
        log.info("已断开: " + client.getSessionId());
    }

    // 创建房间
    private void handleCreateRoom(SocketIOClient client, String data, AckRequest ack) throws Exception {
        String roomId = UUID.randomUUID().toString();
        rooms.put("roomId", roomId);
        rooms.put("client", client.getSessionId());
        rooms.put("你发送过来的参数", data);
        // 发送参数给客户端
        ack.sendAckData(rooms);
    }
}
