package org.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TimeoutController {
    @PostMapping("/timeout")
    public void timeout() throws InterruptedException {
        // 模拟接口一直挂起，或设置 Thread.sleep(60000)
        Thread.sleep(60000);
    }
}