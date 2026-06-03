package org.kob.botrunningsystem.service.impl.utils;

import org.joor.Reflect;
import org.kob.botrunningsystem.utils.BotInterface;
import org.kob.botrunningsystem.utils.CodeSecurityScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.UUID;

@Component
public class Consumer {
    private static RestTemplate restTemplate;
    private static final String receiveBotMoveUrl = "http://127.0.0.1:3000/pk/receive/bot/move/";

    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        Consumer.restTemplate = restTemplate;
    }

    public static void consume(Integer userId, String botCode, String input) {
        // 1. 安全扫描，拦截危险代码
        String violation = CodeSecurityScanner.scan(botCode);
        if (violation != null) {
            System.out.println("[Security] 拦截用户 " + userId + " 的Bot: " + violation);
            sendMove(userId, 0);
            return;
        }

        // 2. 在新线程中编译执行，CPU时间计量替代墙上时钟超时
        Thread botThread = new Thread(() -> {
            try {
                UUID uuid = UUID.randomUUID();
                String uid = uuid.toString().substring(0, 8);

                BotInterface bi = Reflect.compile(
                        "org.kob.botrunningsystem.utils.Bot" + uid,
                        addUid(botCode, uid)
                ).create().get();

                Integer direction = bi.nextMove(input);
                sendMove(userId, direction);
            } catch (Exception e) {
                System.out.println("[BotError] 用户 " + userId + " 的Bot执行异常: " + e.getMessage());
                sendMove(userId, 0);
            }
        });

        botThread.start();

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long threadId = botThread.getId();
        // 等待线程调度, 确保CPU时间可测（未启动的线程 getThreadCpuTime 返回 -1）
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        long startCpuTime = threadMXBean.getThreadCpuTime(threadId);
        // 若JVM不支持CPU时间测量(返回-1), 降级为墙上时钟模式
        if (startCpuTime == -1) {
            System.out.println("[Warning] JVM不支持CPU时间测量, 降级为墙上时钟超时");
            try {
                botThread.join(2000);
                if (botThread.isAlive()) {
                    System.out.println("[Timeout] 用户 " + userId + " 的Bot超时(>2s wall)，兜底判负");
                    sendMove(userId, 0);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        final long cpuLimitNs = 1_000_000_000L;  // 1秒CPU时间上限
        final long wallDeadline = System.currentTimeMillis() + 5000; // 5秒墙上时钟兜底

        while (botThread.isAlive() && System.currentTimeMillis() < wallDeadline) {
            try {
                Thread.sleep(50); // 每50ms轮询一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            long cpuUsed = threadMXBean.getThreadCpuTime(threadId) - startCpuTime;
            if (cpuUsed > cpuLimitNs) {
                System.out.println("[Timeout] 用户 " + userId + " 的Bot CPU时间超限("
                        + (cpuUsed / 1_000_000) + "ms > " + (cpuLimitNs / 1_000_000) + "ms)，兜底判负");
                sendMove(userId, 0);
                return;
            }
        }

        // 墙上时钟兜底（线程可能被IO/等待阻塞）
        if (botThread.isAlive()) {
            System.out.println("[Timeout] 用户 " + userId + " 的Bot墙上时钟超时(>5s)，兜底判负");
            sendMove(userId, 0);
        }
    }

    private static String addUid(String code, String uid) {
        int k = code.indexOf(" implements org.kob.botrunningsystem.utils.BotInterface");
        return code.substring(0, k) + uid + code.substring(k);
    }

    private static void sendMove(Integer userId, Integer direction) {
        MultiValueMap<String, String> data = new LinkedMultiValueMap<>();
        data.add("user_id", userId.toString());
        data.add("direction", direction.toString());
        restTemplate.postForLocation(receiveBotMoveUrl, data, String.class);
    }
}
