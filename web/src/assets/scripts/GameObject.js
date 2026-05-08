const GAME_OBJECTS = [];

export class GameObject {
    constructor() {
        GAME_OBJECTS.push(this);
        this.timedelta = 0;//这一帧与上一帧之间的时间间隔
        this.has_called_start = false; //当前对象是否执行过
    }

    start() {

    }

    update() {

    }

    on_destroy() {

    }

    destroy() {
        this.on_destroy();

        for (let i in GAME_OBJECTS) {
            const obj = GAME_OBJECTS[i];
            if (obj === this) {
                GAME_OBJECTS.splice(i, 1);
                break;
            }
        }
    }
}

let last_timestamp;//上一次执行的时间
const MAX_TIMEDELTA = 200; // 帧间隔上限，5格/s × 200ms = 1格，不会越界震荡

const step = timestamp => {
    for (let obj of GAME_OBJECTS) {
        if (!obj.has_called_start) {
            obj.has_called_start = true;
            obj.start();
        } else {
            let dt = timestamp - last_timestamp;
            if (dt > MAX_TIMEDELTA) dt = MAX_TIMEDELTA;
            obj.timedelta = dt;
            obj.update();
        }
    }
    // 只推进被消耗的时间，超出部分留给后续帧追赶
    if (last_timestamp != null) {
        let consumed = timestamp - last_timestamp;
        if (consumed > MAX_TIMEDELTA) consumed = MAX_TIMEDELTA;
        last_timestamp += consumed;
    } else {
        last_timestamp = timestamp;
    }
    requestAnimationFrame(step);
}

requestAnimationFrame(step)