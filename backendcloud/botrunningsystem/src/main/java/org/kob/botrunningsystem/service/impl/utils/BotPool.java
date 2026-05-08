package org.kob.botrunningsystem.service.impl.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotPool {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public void addBot(Integer userId, String botCode, String input) {
        executor.submit(() -> Consumer.consume(userId, botCode, input));
    }

    public void shutdown() {
        executor.shutdown();
    }
}
