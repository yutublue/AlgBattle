<template>
    <div class="playground" >
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
    /* 60%的浏览器宽度 */
    height: 70vh;
    /* 70%屏幕的的高度 */
    margin: 60px auto;
}
</style>