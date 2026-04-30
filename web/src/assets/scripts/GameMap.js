import { GameObject } from './GameObject.js';
import { Snake } from './Snake.js';
import { Wall } from './Wall.js';

export class GameMap extends GameObject {
    constructor(ctx, parent, store) {//ctx是画布, parent是画布的父元素, 用来动态修改画布的长宽
        super();

        this.ctx = ctx;
        this.parent = parent;
        this.store = store;
        this.L = 0;//L是一个单位的长度, 一个地图的是13×13个单位

        this.rows = 13;
        this.cols = 14;

        this.innner_walls_count = 20;//内部障碍物数量
        this.walls = [];

        this.snakes = [
            new Snake({ id: 0, color: "#4876EC", r: this.rows - 2, c: 1 }, this),
            new Snake({ id: 1, color: "#F94848", r: 1, c: this.cols - 2 }, this)
        ];
    }

    create_walls() {
        const g = this.store.state.pk.gamemap;

        //bool数组判断完毕之后就把墙加进去
        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if (g[r][c]) {
                    this.walls.push(new Wall(r, c, this));
                }
            }
        }

    }

    add_listening_events() {
        this.ctx.canvas.focus();

        const [snake0, snake1] = this.snakes;
        this.ctx.canvas.addEventListener("keydown", e => {
            if (e.key === 'w') snake0.set_direction(0);
            else if (e.key === 'd') snake0.set_direction(1);
            else if (e.key === 's') snake0.set_direction(2);
            else if (e.key === 'a') snake0.set_direction(3);
            else if (e.key === 'ArrowUp') snake1.set_direction(0);
            else if (e.key === 'ArrowRight') snake1.set_direction(1);
            else if (e.key === 'ArrowDown') snake1.set_direction(2);
            else if (e.key === 'ArrowLeft') snake1.set_direction(3);
        });
    }

    start() {
        this.create_walls();
        this.add_listening_events();
    }

    update_size() {
        this.L = parseInt(Math.min(this.parent.clientWidth / this.cols, this.parent.clientHeight / this.rows));
        this.ctx.canvas.width = this.L * this.cols;
        this.ctx.canvas.height = this.L * this.rows;
    }

    //检查两条蛇的状态, 如果状态为静止或者方向没有指令那么就算没准备好
    check_ready() {
        for (const snake of this.snakes) {
            if (snake.status !== "idle")
                return false;
            if (snake.direction === -1)
                return false;
        }
        return true;
    }

    next_step() {

        for (const snake of this.snakes) {
            snake.next_step();
        }
    }

    check_valid(snakebody) {
        for (const wall of this.walls) {
            if (wall.r === snakebody.r && wall.c === snakebody.c) {
                return false;
            }
        }

        for (const snake of this.snakes) {
            let k = snake.snakebodys.length;
            if (!snake.check_tail_increasing()) {//如果当前判断回合蛇尾会前进, 即不会变长, 那么就不判断蛇尾
                k--;
            }

            for (let i = 0; i < k; i++) {
                if (snake.snakebodys[i].r === snakebody.r && snake.snakebodys[i].c === snakebody.c) {
                    return false;
                }
            }
        }

        return true;
    }

    update() {
        this.update_size();

        if (this.check_ready()) {
            this.next_step();
        }
        this.render();
    }

    render() {//渲染函数
        const color_even = '#AAD751';//偶数格子的颜色
        const color_odd = '#A2D149';//奇数格子的颜色

        for (let r = 0; r < this.rows; r++) {
            for (let c = 0; c < this.cols; c++) {
                if ((r + c) % 2 == 0) {
                    this.ctx.fillStyle = color_even;
                } else {
                    this.ctx.fillStyle = color_odd;
                }
                this.ctx.fillRect(c * this.L, r * this.L, this.L, this.L);
            }
        }
    }
}