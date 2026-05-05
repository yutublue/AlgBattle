package org.kob.botrunningsystem.service.impl.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BotPool extends Thread{
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Queue<Bot> bots = new LinkedList<>();

    public void addBot(Integer userId, String botCode, String input) {
        lock.lock();//涉及对bots队列的修改, 要加锁
        try {
            bots.add(new Bot(userId, botCode, input));
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void consume(Bot bot) {
        Consumer consumer = new Consumer();
        consumer.startTimeout(2000, bot);
    }

    @Override
    public void run() {
        while(true) {
            lock.lock();
            if(bots.isEmpty()) {//如果当前队列是空的就阻塞住
                try {
                    condition.await();
                } catch (InterruptedException e) {
                    lock.unlock();
                    throw new RuntimeException(e);
                }
            } else {//如果不是空的
                Bot bot = bots.remove();//取队头
                lock.unlock();
                consume(bot);//因为要编译代码, 耗时很长, 所以放在解锁后面
            }
        }
    }
}
