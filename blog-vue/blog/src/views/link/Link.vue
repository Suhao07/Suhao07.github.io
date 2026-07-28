<template>
  <div>
    <!-- banner -->
    <div class="link-banner banner">
      <h1 class="banner-title">友情链接</h1>
    </div>
    <!-- 链接列表 -->
    <v-card class="blog-container">
      <div class="link-title mb-1">
        <v-icon color="blue">mdi-link-variant</v-icon> 现有链接
      </div>
      <v-row class="link-container">
        <v-col
          class="link-wrapper"
          md="4"
          cols="12"
          v-for="item of friendLinkList"
          :key="item.id"
        >
          <a :href="item.linkAddress" target="_blank">
            <v-avatar size="65" class="link-avatar">
              <img :src="item.linkAvatar" />
            </v-avatar>
            <div style="width:100%;z-index:10;">
              <div class="link-name">{{ item.linkName }}</div>
              <div class="link-intro">{{ item.linkIntro }}</div>
            </div>
          </a>
        </v-col>
      </v-row>
      <!-- 说明 -->
      <div class="link-title mt-4 mb-4">
        <v-icon color="blue">mdi-dots-horizontal-circle</v-icon> 添加友链
      </div>
      <blockquote>
        <div>名称：RickSu 的技术博客</div>
        <div>简介：记录技术、研究与实践</div>
        <div>
          头像：/legacy-assets/static/images/logo.png
        </div>
      </blockquote>
      <blockquote class="mb-10">
        需要交换友链时，请通过 GitHub 联系我。友链信息需包含：名称、介绍、链接和头像。
      </blockquote>
    </v-card>
  </div>
</template>

<script>
export default {
  created() {
    this.listFriendLink();
  },
  data: function() {
    return {
      friendLinkList: []
    };
  },
  methods: {
    listFriendLink() {
      this.axios.get("/api/links").then(({ data }) => {
        this.friendLinkList = data.data;
      });
    }
  }
};
</script>

<style scoped>
blockquote {
  line-height: 2;
  margin: 0;
  font-size: 15px;
  border-left: 0.2rem solid #49b1f5;
  padding: 10px 1rem !important;
  background-color: #ecf7fe;
  border-radius: 4px;
}
.link-banner {
  background: url("@/assets/img/6.jpg") center center /
    cover no-repeat;
  background-color: #49b1f5;
}
.link-title {
  color: #344c67;
  font-size: 21px;
  font-weight: bold;
  line-height: 2;
}
.link-container {
  margin: 10px 10px 0;
}
.link-wrapper {
  position: relative;
  transition: all 0.3s;
  border-radius: 8px;
}
.link-avatar {
  margin-top: 5px;
  margin-left: 10px;
  transition: all 0.5s;
}
@media (max-width: 759px) {
  .link-avatar {
    margin-left: 30px;
  }
}
.link-name {
  text-align: center;
  font-size: 1.25rem;
  font-weight: bold;
  z-index: 1000;
}
.link-intro {
  text-align: center;
  padding: 16px 10px;
  height: 50px;
  font-size: 13px;
  color: #1f2d3d;
  width: 100%;
}
.link-wrapper:hover a {
  color: #fff;
}
.link-wrapper:hover .link-intro {
  color: #fff;
}
.link-wrapper:hover .link-avatar {
  transform: rotate(360deg);
}
.link-wrapper a {
  color: #333;
  text-decoration: none;
  display: flex;
  height: 100%;
  width: 100%;
}
.link-wrapper:hover {
  box-shadow: 0 2px 20px #49b1f5;
}
.link-wrapper:hover:before {
  transform: scale(1);
}
.link-wrapper:before {
  position: absolute;
  border-radius: 8px;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: #49b1f5 !important;
  content: "";
  transition-timing-function: ease-out;
  transition-duration: 0.3s;
  transition-property: transform;
  transform: scale(0);
}

/* 添加深色模式样式 */
.theme--dark .link-title,
[data-theme='dark'] .link-title,
body.theme-dark .link-title {
  color: #81a7c7; /* 更亮的蓝色，适合深色背景 */
}

.theme--dark .link-wrapper a,
[data-theme='dark'] .link-wrapper a,
body.theme-dark .link-wrapper a {
  color: #ffffff; /* 链接颜色改为白色 */
}

.theme--dark .link-name,
[data-theme='dark'] .link-name,
body.theme-dark .link-name {
  color: #49b1f5; /* 确保名称在深色模式下使用亮蓝色 */
}

.theme--dark .link-intro,
[data-theme='dark'] .link-intro,
body.theme-dark .link-intro {
  color: #cccccc; /* 描述文字使用浅灰色 */
}

/* 深色模式下悬停效果 */
.theme--dark .link-wrapper:hover .link-name,
[data-theme='dark'] .link-wrapper:hover .link-name,
body.theme-dark .link-wrapper:hover .link-name,
.theme--dark .link-wrapper:hover .link-intro,
[data-theme='dark'] .link-wrapper:hover .link-intro,
body.theme-dark .link-wrapper:hover .link-intro {
  color: #ffffff; /* 悬停时文字为白色 */
}

/* 修改引用块的深色模式样式 */
.theme--dark blockquote,
[data-theme='dark'] blockquote,
body.theme-dark blockquote {
  background-color: #2c2c2c;
  border-left-color: #49b1f5;
  color: #e0e0e0;
}

/* 确保在深色模式下链接卡片的背景透明，以便:before伪元素的颜色可见 */
.theme--dark .link-wrapper,
[data-theme='dark'] .link-wrapper,
body.theme-dark .link-wrapper {
  background-color: rgba(30, 30, 30, 0.6);
}

/* 在深色模式下调整卡片悬停时的阴影效果 */
.theme--dark .link-wrapper:hover,
[data-theme='dark'] .link-wrapper:hover,
body.theme-dark .link-wrapper:hover {
  box-shadow: 0 2px 20px rgba(73, 177, 245, 0.7);
}
</style>
