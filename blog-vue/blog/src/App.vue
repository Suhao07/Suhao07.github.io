<template>
  <v-app id="app">
    <!-- 导航栏 -->
    <TopNavBar></TopNavBar>
    <!-- 侧边导航栏 -->
    <SideNavBar></SideNavBar>
    <!-- 内容 -->
    <v-content>
      <router-view :key="$route.fullPath" />
    </v-content>
    <!-- 页脚 -->
    <Footer></Footer>
    <!-- 返回顶部 -->
    <BackTop></BackTop>
    <!-- 搜索模态框 -->
    <searchModel></searchModel>
    <!-- 烟花特效 -->
    <canvas
      class="fireworks"
      style="position:fixed;left:0;top:0;z-index:99999999;pointer-events:none;"
    ></canvas>
    <!-- 全局加载遮罩 -->
    <loading-overlay 
      :visible="$store.state.loading.isLoading"
      :message="$store.state.loading.loadingMessage" 
    />
  </v-app>
</template>

<script>
import TopNavBar from "./components/layout/TopNavBar";
import SideNavBar from "./components/layout/SideNavBar";
import Footer from "./components/layout/Footer";
import BackTop from "./components/BackTop";
import searchModel from "./components/model/SearchModel";
import LoadingOverlay from '@/components/common/LoadingOverlay.vue';
// import MathJaxComponent from './assets/js/MathJaxComponent.vue'
export default {
  name: "App",
  components: {
    TopNavBar,
    SideNavBar,
    Footer,
    BackTop,
    searchModel,
    LoadingOverlay,
    // MathJaxComponent
  },
  mounted() {
    // 加载 anime.js
    const script1 = document.createElement("script");
    script1.src = "/js/anime.min.js";
    script1.onload = () => {
      // 加载 fireworks.js
      const script2 = document.createElement("script");
      script2.src = "/js/fireworks.js";
      document.head.appendChild(script2);
    };
    document.head.appendChild(script1);
  }
};
</script>

<style>
.fireworks {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 99999999;
  pointer-events: none;
}
</style>
