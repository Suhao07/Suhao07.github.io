<template>
  <el-container>
    <div
      v-if="isMobile && mobileMenuOpen"
      class="mobile-menu-mask"
      @click="mobileMenuOpen = false"
    />
    <!-- 侧边栏 -->
    <el-aside width="auto" :class="{ 'mobile-aside': isMobile }">
      <SideBar
        :mobile="isMobile"
        :mobile-open="mobileMenuOpen"
        @navigate="mobileMenuOpen = false"
      />
    </el-aside>
    <el-container :class="'main-container ' + isHide">
      <!-- 导航栏 -->
      <el-header height="84px" style="padding:0">
        <NavBar
          :key="$route.fullPath"
          :mobile="isMobile"
          @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen"
        />
      </el-header>
      <!-- 内容 -->
      <el-main style="background:#F7F9FB">
        <div class="fade-transform-box">
          <transition name="fade-transform" mode="out-in">
            <router-view :key="$route.fullPath" />
          </transition>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import NavBar from "./components/NavBar";
import SideBar from "./components/SideBar";
export default {
  components: {
    NavBar,
    SideBar
  },
  data() {
    return {
      isMobile: window.innerWidth <= 768,
      mobileMenuOpen: false
    };
  },
  mounted() {
    window.addEventListener("resize", this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
  },
  watch: {
    $route() {
      this.mobileMenuOpen = false;
    }
  },
  computed: {
    isHide() {
      return this.$store.state.collapse ? "hideSideBar" : "";
    }
  },
  methods: {
    handleResize() {
      this.isMobile = window.innerWidth <= 768;
      if (!this.isMobile) {
        this.mobileMenuOpen = false;
      }
    }
  }
};
</script>

<style scoped>
.main-container {
  transition: margin-left 0.45s;
  margin-left: 210px;
  min-height: 100vh;
}
.hideSideBar {
  margin-left: 64px;
}
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.5s ease 0s;
}
.fade-transform-enter {
  opacity: 0;
  transform: translateX(-30px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
.fade-transform-box {
  position: relative;
  top: 0px;
  bottom: 0px;
  width: 100%;
  overflow: hidden;
}

.mobile-menu-mask {
  position: fixed;
  z-index: 1998;
  inset: 0;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(2px);
}

@media (max-width: 768px) {
  .main-container,
  .hideSideBar {
    margin-left: 0;
    width: 100%;
  }

  .mobile-aside {
    width: 0 !important;
    overflow: visible;
  }

  .el-main {
    padding: 8px !important;
  }

  .fade-transform-box {
    overflow: visible;
  }
}
</style>
