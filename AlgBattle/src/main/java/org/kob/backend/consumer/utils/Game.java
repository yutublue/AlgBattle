package org.kob.backend.consumer.utils;

import java.util.Random;

public class Game {
    final private Integer rows;
    final private Integer cols;
    final private Integer inner_walls_count;
    final private int[][] g;
    final private static int[] dx = {-1, 0, 1, 0}, dy =  {0, 1, 0, -1};

    public Game(Integer rows, Integer cols, Integer inner_walls_count) {
        this.rows = rows;
        this.cols = cols;
        this.inner_walls_count = inner_walls_count;
        this.g = new int[rows][cols];
    }

    public int[][] getG() {
        return g;
    }

    private boolean check_connectivity(int sx, int  sy, int tx, int ty) {
        if(sx == tx && sy == ty) return true;
        g[sx][sy] = 1;

        for(int i = 0; i < 4; i++) {
            int x = sx + dx[i];
            int y = sy + dy[i];
            if(x >= 0 && x < this.rows && y >= 0 && y < this.cols && g[x][y] == 0) {
                if(check_connectivity(x, y, tx, ty)) {
                    g[sx][sy] = 0;
                    return true;
                }
            }
        }

        g[sx][sy] = 0;
        return false;
    }

    private boolean draw() {

        //一个bool数组, 判断哪里该放墙, 一开始先把所有位置初始化成没有墙
        for(int i = 0; i < this.rows; i++) {
            for(int j = 0; j < this.cols; j++) {
                g[i][j] = 0;
            }
        }

        //给地图两边加上墙
        for(int r = 0; r < this.rows; r++) {
            g[r][0] = g[r][this.cols - 1] = 1;
        }

        //给地图上下加上墙
        for(int c = 0; c < this.cols; c++) {
            g[0][c] = g[this.rows - 1][c] = 1;
        }

        Random random = new Random();
        //随机生成墙
        for(int i = 0; i < this.inner_walls_count / 2; i++) {//地图是中心对称的, 所以要/2
            for(int j = 0; j < 1000; j++){//每一堵墙尝试一千次生成, 一生成成功就转到下一堵墙
                int r = random.nextInt(this.rows);
                int c = random.nextInt(this.cols);

                if(g[r][c] == 1 || g[this.rows - 1 - r][this.cols - 1 - c] == 1) {//如果这个位置已经有墙了就再随机一次
                    continue;
                }

                if(r == this.rows - 2 && c == 1 || r == 1 && c == this.cols - 2) {//不能在双方出生点生成墙
                    continue;
                }

                g[r][c] = g[this.rows - 1 - r][this.cols - 1 - c] = 1;
                break;//找到位置生成墙了, 换下一堵墙

            }
        }

        return check_connectivity(this.rows - 2, 1, 1, this.cols - 2);
    }

    public void createMap() {
        for(int i = 0; i < 1000; i++) {
            if(draw()) break;
        }
    }
}
