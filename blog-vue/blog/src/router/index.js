import Vue from "vue";
import VueRouter from "vue-router";
import store from '../store';

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    component: () => import('../views/home/Home.vue'),
    meta: {
      title: "RickSu 的博客"
    }
  },
  {
    path: "/articles/:articleId",
    component: () => import('../views/article/Article.vue')
  },
  {
    path: "/archives",
    component: () => import('../views/archive/Archive.vue'),
    meta: {
      title: "归档"
    }
  },
  {
    path: "/tags",
    component: () => import('../views/tag/Tag.vue'),
    meta: {
      title: "标签"
    }
  },
  {
    path: "/categories",
    component: () => import('../views/category/Category.vue'),
    meta: {
      title: "分类"
    }
  },
  {
    path: "/categories/:categoryId",
    name: "CategoryArticles",
    component: () => import('../components/ArticleList.vue')
  },
  {
    path: "/links",
    component: () => import('../views/link/Link.vue'),
    meta: {
      title: "友链列表"
    }
  },
  {
    path: "/about",
    component: () => import('../views/about/About.vue'),
    meta: {
      title: "关于我"
    }
  },
  {
    path: "/tags/:tagId",
    name: "TagArticles",
    component: () => import('../components/ArticleList.vue')
  },
  {
    path: "*",
    redirect: "/"
  }
];

const router = new VueRouter({
  mode: 'hash',
  base: '/',
  routes
});

// 添加全局前置守卫
router.beforeEach((to, from, next) => {
  // 显示加载遮罩
  store.dispatch('showLoading', '页面加载中...');
  next();
});

// 添加全局后置钩子
router.afterEach(() => {
  // 延迟一点隐藏加载遮罩，让页面有时间渲染
  setTimeout(() => {
    store.dispatch('hideLoading');
  }, 300);
});

export default router;
