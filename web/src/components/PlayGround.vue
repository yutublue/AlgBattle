<template>
    <div class="playground">
        <div class="role-indicator" v-if="$store.state.pk.role">
            <span class="snake-dot"
                :style="{ backgroundColor: $store.state.pk.role === 'A' ? '#4876EC' : '#F94848' }"></span>
            你控制<span :style="{ color: $store.state.pk.role === 'A' ? '#4876EC' : '#F94848', fontWeight: 'bold' }">{{
                $store.state.pk.role === 'A' ? '蓝' : '红' }}色蛇</span>
            ({{ $store.state.pk.role === 'A' ? '左下角' : '右上角' }})
        </div>
        <GameMap />
    </div>
</template>

<script>
import GameMap from './GameMap.vue'

export default {
    components: {
        GameMap,
    }
}

</script>

<style scoped>
div.playground {
    width: 60vw;
    height: 70vh;
    margin: 60px auto;
}

.role-indicator {
    text-align: center;
    font-size: 18px;
    margin-bottom: 10px;
    color: #333;
}

.snake-dot {
    display: inline-block;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    margin-right: 6px;
    vertical-align: middle;
}
</style>