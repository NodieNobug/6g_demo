package org.edgeServer.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import org.edgeServer2.utils.Paillier;

public class EdgeManager {
    private static final ConcurrentHashMap<String, String> clientCipherTexts = new ConcurrentHashMap<>();
    private static String aggregatedCipherText = "";
    private static final String EDGE_SERVER2_URL = "http://localhost:33456";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void registerClient(String clientId, String cipherText) {
        clientCipherTexts.put(clientId, cipherText);
        updateAggregatedCipherText();
    }

    private static void updateAggregatedCipherText() {
        if (clientCipherTexts.isEmpty()) {
            aggregatedCipherText = "";
            return;
        }

        // 初始化聚合结果为第一个密文
        BigInteger result = new BigInteger(clientCipherTexts.values().iterator().next());

        // 对剩余的密文进行乘法运算（对应明文加法）
        for (String cipherText : clientCipherTexts.values()) {
            if (cipherText.equals(clientCipherTexts.values().iterator().next())) {
                continue; // 跳过第一个已经使用的密文
            }
            BigInteger currentCipher = new BigInteger(cipherText);
            // 在模n^2下进行乘法运算
            result = result.multiply(currentCipher).mod(Paillier.getN2());
        }

        aggregatedCipherText = result.toString();

        // 发送聚合后的密文到edgeServer2
        sendAggregatedCipherTextToEdgeServer2(aggregatedCipherText);
    }

    private static void sendAggregatedCipherTextToEdgeServer2(String cipherText) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EDGE_SERVER2_URL + "/post/aggregatedCipherText"))
                    .POST(HttpRequest.BodyPublishers.ofString(cipherText))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Failed to send aggregated cipher text to edge server 2: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending aggregated cipher text to edge server 2: " + e.getMessage());
        }
    }

    public static String getAggregatedCipherText() {
        return aggregatedCipherText;
    }

    public static int getClientCount() {
        return clientCipherTexts.size();
    }
}