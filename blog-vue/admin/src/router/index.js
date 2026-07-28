import Vue from "vue";
import VueRouter from "vue-router";
import Layout from "@/layout/index.vue";

Vue.use(VueRouter);

const routes = [
  {
    path: "/login",
    name: "登录",
    hidden: true,
    component: () => import("../views/login/Login.vue")
  },
  {
    path: "/favorites",
    component: Layout,
    redirect: "/favorites/list",
    name: "FavoriteManage",
    meta: {
      title: "收藏管理",
      icon: "el-icon-star-on"
    },
    children: [
      {
        path: "list",
        name: "FavoriteList",
        component: () => import("../views/favorite/FavoriteList"),
        meta: { title: "收藏网站" }
      },
      {
        path: "category",
        name: "FavoriteCategory",
        component: () => import("../views/favorite/FavoriteCategory"),
        meta: { title: "分类管理" }
      }
    ]
  }
];

const createRouter = () =>
  new VueRouter({
    mode: "history",
    routes: routes
  });

const router = createRouter();

export function resetRouter() {
  const newRouter = createRouter();
  router.matcher = newRouter.matcher;
}

export default router;
