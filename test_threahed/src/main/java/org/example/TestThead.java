package org.example;



import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;



import java.util.concurrent.*;





@RestController
public class TestThead {

    // 构造一个“容易堆满”的线程池
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
            2,
            3,
            60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(5),
            new ThreadPoolExecutor.CallerRunsPolicy() // 当线程池满，任务由主线程执行
    );

    // 模拟一个阻塞接口调用（假设 http://localhost:9999/timeout 永远不响应）
    public static CompletableFuture<String> getFuture() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse response = HttpUtil.createPost("https://baidu.com")
                        .timeout(120000) // 设置超长超时
                        .execute();
                return response.body();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, EXECUTOR);
    }

    // 🚨 启动爆量请求任务，绑定主线程（危险）
    @GetMapping("/block")
    public String blockAll() throws Exception {
        for (int i = 0; i < 1000; i++) {
            CompletableFuture<String> future = getFuture();
            // 主线程等待（❗一旦线程池满，这会阻塞主线程）
            String result = future.get();
            System.out.println(result);
        }
        return "任务已提交";
    }

    // 另一个接口，验证主线程是否还活着
    @GetMapping("/other")
    public String other() {
        System.out.println("【/other】接口被调用，当前线程：" + Thread.currentThread().getName());
        return "other接口调用成功";
    }
}