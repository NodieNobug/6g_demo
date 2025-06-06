package org.edgeServer.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import org.edgeServer.utils.EdgeManager;

public class EdgeHandler implements HttpHandler {
    private static final ConcurrentHashMap<String, EdgeManager> clients = new ConcurrentHashMap<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String clientId = exchange.getRequestHeaders().getFirst("Client-ID");

        if (clientId == null) {
            sendResponse(exchange, 400, "Missing Client-ID header");
            return;
        }

        String response;
        if (path.equals("/get/totalclientNum")) {
            response = "totalclientNum:" + String.valueOf(EdgeManager.getClientCount());
        } else if (path.equals("/get/sumcipherText")) {
            response = "sumcipherText:" + EdgeManager.getAggregatedCipherText();
        } else if (path.equals("/post/cipherText")) {
            if (exchange.getRequestMethod().equals("POST")) {
                // 读取请求体中的密文
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String cipherText = reader.readLine();

                // 注册或更新客户端密文
                EdgeManager.registerClient(clientId, cipherText);
                response = "Success";
            } else {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
        } else {
            sendResponse(exchange, 404, "Path not found");
            return;
        }

        sendResponse(exchange, 200, response);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}