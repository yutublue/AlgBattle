package org.kob.backend.consumer.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private Integer id;
    private Integer botId;//-1表示真人操作, 其他表示机器
    private String botCode;

    //起点坐标
    private Integer sx;
    private Integer sy;

    //走过的方向
    private List<Integer> steps;

    private boolean check_tail_increasing(int step) {//检验当前回合蛇的身体是否变长
        if(step <= 10) return true;

        return step % 3 == 1;
    }

    public List<Cell> getCells() {
        List<Cell> res = new ArrayList<>();//蛇的身体

        int[] dx = {-1, 0, 1, 0};
        int[] dy = {0, 1, 0, -1};
        int x = sx, y = sy;
        int step = 0;

        res.add(new Cell(x, y));//蛇头
        for(int d : steps) {
            x += dx[d];
            y += dy[d];
            res.add(new Cell(x, y));
            if(!check_tail_increasing(++ step)) {//如果当前回合蛇不变长的话
                res.remove(0);//移除蛇尾
            }
        }

        return res;
    }

    public String getStepsString() {
        StringBuilder res = new StringBuilder();
        for(int d : steps) {
            res.append(d);
        }

        return res.toString();
    }

}
