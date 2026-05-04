package org.kob.matchingsystem.service.impl;

import org.kob.matchingsystem.service.MatchingService;
import org.kob.matchingsystem.service.impl.utils.MatchingPool;
import org.springframework.stereotype.Service;

@Service
public class MatchingServiceImpl implements MatchingService {
    public final static MatchingPool matchingPool = new MatchingPool();//全局只有一个线程

    @Override
    public String addPlayer(Integer userId, Integer rating) {
        System.out.println("addPlayer: " + userId + " " + rating);
        matchingPool.addPlayer(userId, rating);
        return "add player success";
    }

    @Override
    public String removePlayer(Integer userId) {
        System.out.println("removePlayer: " + userId);
        matchingPool.removePlayer(userId);
        return "remove player success";
    }
}
