import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SimpleChatAPITest {

    private static final String API_URL = "http://192.168.0.5:8007/v1/chat/completions";

    public static void main(String[] args) {
        try {
            // 准备请求数据
            String jsonData = "{\n" +
                    "  \"model\": \"gwen/Qwen3-14B\",\n" +
                    "  \"messages\": [\n" +
                    "    {\n" +
                    "      \"role\": \"user\",\n" +
                    "      \"content\": \"你好！请介绍一下你自己。\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"temperature\": 0.7,\n" +
                    "  \"max_tokens\": 500\n" +
                    "}";

            // 创建连接
            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求方法和头部
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // 发送请求数据
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 获取响应
            int responseCode = connection.getResponseCode();
            System.out.println("响应码: " + responseCode);

            // 读取响应内容
            Scanner scanner;
            if (responseCode == 200) {
                scanner = new Scanner(connection.getInputStream());
                System.out.println("✅ 请求成功！");
            } else {
                scanner = new Scanner(connection.getErrorStream());
                System.out.println("❌ 请求失败！");
            }

            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            System.out.println("响应内容:");
            System.out.println(response.toString());

        } catch (IOException e) {
            System.out.println("❌ 请求异常: " + e.getMessage());
        }
    }
}