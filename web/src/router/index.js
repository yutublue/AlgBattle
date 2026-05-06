import { createRouter, createWebHistory } from 'vue-router'
import PKIndexView from '@/views/pk/PKIndexView.vue'
import RecordIndexView from '@/views/record/RecordIndexView.vue'
import RecordContentView from '@/views/record/RecordContentView.vue'
import RanklistIndexView from '@/views/ranklist/RanklistIndexView.vue'
import UserBotIndexView from '@/views/user/bot/UserBotIndexView.vue'
import NotFound from '@/views/error/NotFound.vue'
import UserAccountLoginView from '@/views/user/UserAccountLoginView.vue'
import UserAccountRegisterView from '@/views/user/UserAccountRegisterView.vue'
import store from '../store/index'

const routes = [
  {
    path: "/",
    name: "home",
    redirect: "/pk/",
    meta: {//名字是自己定义的
      requestAuth: true,//表示需不需要授权
    }
  },
  {
    path: "/pk/",
    name: "pk_index",
    component: PKIndexView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: "/record/",
    name: "record_index",
    component: RecordIndexView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: "/record/:recordId/",
    name: "record_content",
    component: RecordContentView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: "/ranklist/",
    name: "ranklist_index",
    component: RanklistIndexView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: "/user/bot/",
    name: "user_bot_index",
    component: UserBotIndexView,
    meta: {
      requestAuth: true,
    }
  },
  {
    path: "/user/account/login/",
    name: "user_account_login",
    component: UserAccountLoginView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: "/user/account/register/",
    name: "user_account_register",
    component: UserAccountRegisterView,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: "/404/",
    name: "not_found_index",
    component: NotFound,
    meta: {
      requestAuth: false,
    }
  },
  {
    path: "/:catchAll(.*)",
    redirect: "/404/"
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {//router在进入到每个页面之前, 会调用这个函数
  //to: 即将进入的目标路由对象
  //from: 当前导航正要离开的路由
  //next: 下一步操作

  if(to.meta.requestAuth) {//如果需要授权
    if(store.state.user.is_login) {//如果已经登录
      next()//直接放行
    } else {//如果未登录
      next("/user/account/login/")//跳转到登录页面
    }
  } else {//如果不需要授权
    next()//直接放行
  }
})

export default router
