<template>
  <el-scrollbar style="height:100%;overflow-x: hidden;">
    <el-menu
      :class="[
        'side-nav-bar',
        { 'mobile-side-nav': mobile, 'mobile-side-nav-open': mobileOpen }
      ]"
      router
      :collapse="mobile ? false : this.$store.state.collapse"
      :default-active="this.$route.path"
      background-color="#304156"
      text-color="#BFCBD9"
      active-text-color="#409EFF"
      @select="$emit('navigate')"
    >
      <template v-for="route of this.$store.state.userMenuList">
        <!-- 二级菜单 -->
        <template v-if="route.name && route.children && !route.hidden">
          <el-submenu :key="route.path" :index="route.path">
            <!-- 二级菜单标题 -->
            <template v-slot:title>
              <i :class="route.icon" />
              <span>{{ route.name }}</span>
            </template>
            <!-- 二级菜单选项 -->
            <template v-for="(item, index) of route.children">
              <el-menu-item v-if="!item.hidden" :key="index" :index="item.path">
                <i :class="item.icon" />
                <span slot="title">{{ item.name }}</span>
              </el-menu-item>
            </template>
          </el-submenu>
        </template>
        <!-- 一级菜单 -->
        <template v-else-if="!route.hidden">
          <el-menu-item :index="route.path" :key="route.path">
            <i :class="route.children[0].icon" />
            <span slot="title">{{ route.children[0].name }}</span>
          </el-menu-item>
        </template>
      </template>
    </el-menu>
  </el-scrollbar>
</template>

<script>
export default {
  props: {
    mobile: {
      type: Boolean,
      default: false
    },
    mobileOpen: {
      type: Boolean,
      default: false
    }
  }
};
</script>

<style scoped>
.side-nav-bar:not(.el-menu--collapse) {
  width: 210px;
}
.side-nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
}
.side-nav-bar i {
  margin-right: 1rem;
}

.mobile-side-nav {
  z-index: 1999;
  width: min(82vw, 280px) !important;
  transform: translateX(-102%);
  box-shadow: 12px 0 32px rgba(15, 23, 42, 0.25);
  transition: transform 0.25s ease;
}

.mobile-side-nav-open {
  transform: translateX(0);
}
</style>
