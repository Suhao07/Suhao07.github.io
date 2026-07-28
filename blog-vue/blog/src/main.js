import Vue from 'vue'  
import App from "./App.vue";
import router from "./router";
import store from "./store";
import vuetify from "./plugins/vuetify";

Vue.prototype.config = config;
Vue.config.productionTip = false;

// 然后再导入其他样式和插件
import 'animate.css';
import "./assets/css/index.css";
import "./assets/css/iconfont.css";
import "./assets/css/markdown.css";
import config from "./assets/js/config";
import axios from "axios";
import VueAxios from "vue-axios";
import { installStaticApi } from "./services/staticApi";
import moment from "moment";
import InfiniteLoading from "vue-infinite-loading";
import "highlight.js/styles/atom-one-dark.css";
import Toast from "./components/toast/index";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import "@fortawesome/fontawesome-free/css/all.css";
import "./assets/css/katex-custom.css";
import './assets/css/markdown-plugins.css'

// 然后再导入其他插件
installStaticApi(axios);
Vue.use(vuetify);
Vue.use(InfiniteLoading);
Vue.use(VueAxios, axios);
Vue.use(Toast);

Vue.filter("date", value => {
  return moment(value).format("YYYY-MM-DD");
});

Vue.filter("hour", function(value) {
  return moment(value).format("HH:mm:ss");
});

Vue.filter("num", function(value) {
  if (value >= 1000) {
    return (value / 1000).toFixed(1) + "k";
  }
  return value;
});

router.beforeEach((to, from, next) => {
  NProgress.start();
  if (to.meta.title) {
    document.title = to.meta.title;
  }
  next();
});

router.afterEach(() => {
  window.scrollTo({
    top: 0,
    behavior: "instant"
  });
  NProgress.done();
});

axios.interceptors.response.use(
  function(response) {
    switch (response.data.code) {
      case 50000:
        Vue.prototype.$toast({ type: "error", message: "系统异常" });
        break;
      // 添加其他状态码处理
    }
    return response;
  },
  function(error) {
    // 添加错误处理
    if (error.response) {
      switch (error.response.status) {
        case 404:
          Vue.prototype.$toast({ type: "error", message: "请求的资源不存在" });
          break;
        case 500:
          Vue.prototype.$toast({ type: "error", message: "服务器内部错误" });
          break;
        default:
          Vue.prototype.$toast({ type: "error", message: "网络错误" });
      }
    } else {
      Vue.prototype.$toast({ type: "error", message: "网络连接失败" });
    }
    return Promise.reject(error);
  }
);

// 添加全局错误处理
Vue.config.errorHandler = function() {
  
}

// 注册全局组件
Vue.component('infinite-loading', InfiniteLoading)

new Vue({
  router,
  store,
  vuetify,
  render: h => h(App)
}).$mount("#app");
