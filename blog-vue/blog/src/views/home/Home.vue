<template>
  <div>
    <!-- banner -->
    <div class="home-banner">
      <div class="banner-container">
        <!-- 联系方式 -->
        <h1 class="blog-title animated zoomIn">
          RickSu 的博客
        </h1>
        <!-- 一言 -->
        <div class="blog-intro">
          {{ obj.output }} <span class="typed-cursor">|</span>
        </div>
        <!-- 联系方式 -->
        <div class="blog-contact">
          <a
            target="_blank"
            href="https://github.com/Suhao07"
            aria-label="RickSu 的 GitHub"
            class="ml-5 mr-5 iconfont icongithub"
          />
        </div>
      </div>
      <!-- 向下滚动 -->
      <div class="scroll-down" @click="scrollDown">
        <v-icon color="#fff" class="scroll-down-effects">
          mdi-chevron-down
        </v-icon>
      </div>
    </div>
    <!-- 主页文章 -->
    <v-row class="home-container">
        <!-- 博主信息 -->
        <v-col md="3" cols="12" class="d-md-block d-none">
          <div class="blog-wrapper">
            <v-card class="animated zoomIn blog-card mt-5">
              <div class="author-wrapper site-author">
                <!-- 博主头像 -->
                <v-avatar size="110">
                  <img class="author-avatar" :src="blogInfo.avatar" />
                </v-avatar>
                <div class="author-name">{{ blogInfo.nickname }}</div>
                <div class="author-description">{{ blogInfo.intro }}</div>
              </div>
              <!-- 博客信息 -->
              <div class="site-state">
                <div class="site-state-item">
                  <router-link to="/archives">
                    <div class="state-num">{{ blogInfo.articleCount }}</div>
                    <div class="state-name">日志</div>
                  </router-link>
                </div>
                <div class="site-state-item">
                  <router-link to="/categories">
                    <div class="state-num">{{ blogInfo.categoryCount }}</div>
                    <div class="state-name">分类</div>
                  </router-link>
                </div>
                <div class="site-state-item">
                  <router-link to="/tags">
                    <div class="state-num">{{ blogInfo.tagCount }}</div>
                    <div class="state-name">标签</div>
                  </router-link>
                </div>
              </div>
  
              <!-- 收藏按钮 -->
              <a class="collection-btn" @click="tip = true">
                <v-icon color="#fff" size="18" class="mr-1">mdi-bookmark</v-icon>
                加入书签
              </a>
  
              <!-- 社交媒体链接 - 与Article保持一致 -->
              <div class="site-social">
                <a
                  href="https://github.com/Suhao07"
                  target="_blank"
                  class="social-item"
                >
                  <i class="iconfont icongithub"></i> GitHub
                </a>
              </div>
  
              <!-- CC许可证 -->
              <div class="site-cc">
                <a
                  href="https://creativecommons.org/licenses/by-nc-sa/4.0/"
                  target="_blank"
                  class="license-icon"
                >
                  <img
                    src="https://licensebuttons.net/l/by-nc-sa/4.0/88x31.png"
                    alt="CC BY-NC-SA 4.0"
                  />
                </a>
                <!-- RSS订阅图标 -->
                <a
                  href="/rss.xml"
                  target="_blank"
                  class="rss-icon"
                  title="RSS订阅"
                >
                  <img
                    src="@/assets/img/rss.png"
                    alt="RSS Feed"
                    style="height: 31px;"
                  />
                </a>
              </div>
            </v-card>
            <!-- 网站信息 -->
            <v-card class="blog-card animated zoomIn mt-5">
              <div class="announcement-header">
                <v-icon color="#49b1f5" size="20">mdi-bullhorn</v-icon>
                <span class="announcement-title">公告板</span>
              </div>
              
              <div class="announcement-content">
                <div class="modern-timeline">
                  <div class="timeline-item" v-for="(notice, index) in blogInfo.notices" :key="notice.id">
                    <div class="timeline-dot" :class="{'secondary': index > 0}">
                      <v-icon style="color: #ffffff !important;" size="14">
                        {{ index === 0 ? 'mdi-bell' : 'mdi-information-outline' }}
                      </v-icon>
                    </div>
                    <div class="timeline-card">
                      <div class="timeline-time">
                        <v-icon size="12">mdi-clock-outline</v-icon>
                        {{ formatDate(notice.createTime) }}
                      </div>
                      <div class="timeline-content markdown-body" v-html="renderMarkdown(notice.content)"></div>
                    </div>
                  </div>
                </div>
              </div>
            </v-card>
          </div>
        </v-col>
      <v-col md="9" cols="12">
        <v-card
          class="animated zoomIn article-card"
          style="border-radius: 12px 8px 8px 12px"
          v-for="(item, index) of articleList"
          :key="item.id"
        >
          <!-- 文章封面图 -->
          <div :class="isRight(index)">
            <router-link :to="'/articles/' + item.id">
              <v-img
                class="on-hover"
                width="90%"
                height="90%"
                :src="item.articleCover"
              />
            </router-link>
          </div>
          <!-- 文章信息 -->
          <div class="article-wrapper">
            <div style="line-height:1.4">
              <router-link :to="'/articles/' + item.id">
                {{ item.articleTitle }}
              </router-link>
            </div>
            <div class="article-info">
              <!-- 是否置顶 -->
              <span v-if="item.isTop == 1">
                <span style="color:#ff7242">
                  <i class="iconfont iconzhiding" /> 置顶
                </span>
                <span class="separator">|</span>
              </span>
              <!-- 发表时间 -->
              <v-icon size="14">mdi-calendar-month-outline</v-icon>
              {{ item.createTime | date }}
              <span class="separator">|</span>
              <!-- 文章分类 -->
              <router-link :to="'/categories/' + item.categoryId">
                <v-icon size="14">mdi-inbox-full</v-icon>
                {{ item.categoryName }}
              </router-link>
              <span class="separator">|</span>
              <!-- 文章标签 -->
              <router-link
                style="display:inline-block"
                :to="'/tags/' + tag.id"
                class="mr-1"
                v-for="tag of item.tagDTOList"
                :key="tag.id"
              >
                <v-icon size="14">mdi-tag-multiple</v-icon>{{ tag.tagName }}
              </router-link>
            </div>
            <!-- 文章内容 -->
            <div class="article-content">
              {{ item.articleContent }}
            </div>
          </div>
        </v-card>
        <!-- 无限加载 -->
        <infinite-loading @infinite="infiniteHandler">
          <div slot="no-more" />
        </infinite-loading>
      </v-col>

    </v-row>
    <!-- 提示消息 -->
    <v-snackbar v-model="tip" top color="#49b1f5" :timeout="2000">
      按CTRL+D 键将本页加入书签
    </v-snackbar>
  </div>
</template>

<script>
import EasyTyper from "easy-typer-js";
import InfiniteLoading from "vue-infinite-loading";

export default {
  components: {
    InfiniteLoading
  },
  created() {
    this.init();
    this.getBlogInfo();
    this.timer = setInterval(this.runTime, 1000);
  },
  data: function() {
    return {
      tip: false,
      time: "",
      obj: {
        output: "",
        isEnd: false,
        speed: 300,
        singleBack: false,
        sleep: 0,
        type: "rollback",
        backSpeed: 40,
        sentencePause: true
      },
      articleList: [],
      blogInfo: {},
      current: 1,
      renderedNotice: ""
    };
  },
  methods: {
    // 初始化
    init() {
      document.title = this.$route.meta.title;
      
      // 一言Api进行打字机循环输出效果 - 添加超时处理
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 5000); // 5秒超时
      
      fetch("https://v1.hitokoto.cn?c=i", { 
        signal: controller.signal 
      })
        .then(res => {
          clearTimeout(timeoutId);
          if (!res.ok) {
            throw new Error('网络响应异常');
          }
          return res.json();
        })
        .then(({ hitokoto }) => {
          this.initTyped(hitokoto);
        })
        .catch(error => {
          
          // 备选的一句话数组
          const defaultQuotes = [
            "欢迎来到我的个人博客，望你在这里能找到值得收藏的知识。",
            "生活不止眼前的苟且，还有诗和远方的田野。",
            "代码是写给人看的，顺便让机器执行一下。",
            "学习不是为了成为别人，而是为了成为更好的自己。"
          ];
          // 随机选择一句话
          const randomQuote = defaultQuotes[Math.floor(Math.random() * defaultQuotes.length)];
          this.initTyped(randomQuote);
        });
    },
    initTyped(input, fn, hooks) {
      const obj = this.obj;
      // eslint-disable-next-line no-unused-vars
      const typed = new EasyTyper(obj, input, fn, hooks);
    },
    scrollDown() {
      window.scrollTo({
        behavior: "smooth",
        top: document.documentElement.clientHeight
      });
    },
    getBlogInfo() {
      this.axios.get("/api/").then(({ data }) => {
        if (data.flag) {
          // 确保 notices 存在，如果不存在则设置为空数组
          this.blogInfo = {
            ...data.data,
            notices: data.data.notices || []
          };
          this.$store.commit("checkBlogInfo", this.blogInfo);
          
          // 调试输出
          
        } else {
          this.$message.error("获取博客信息失败");
        }
      }).catch(error => {
        
        this.$message.error("获取博客信息失败");
      });
    },
    infiniteHandler($state) {
      let md = require("markdown-it")();
      this.axios
        .get("/api/articles", {
          params: {
            current: this.current
          }
        })
        .then(({ data }) => {
          if (data.data.length) {
            // 去除markdown标签
            data.data.forEach(item => {
              item.articleContent = md
                .render(item.articleContent)
                .replace(/<\/?[^>]*>/g, "")
                .replace(/[|]*\n/, "")
                .replace(/&npsp;/gi, "");
            });
            this.articleList.push(...data.data);
            this.current++;
            $state.loaded();
          } else {
            $state.complete();
          }
        });
    },
    getUpdateIcon(type) {
      const icons = {
        'article': 'mdi-file-document-outline',
        'category': 'mdi-folder-outline',
        'tag': 'mdi-tag-outline',
        'system': 'mdi-cog-outline',
        'feature': 'mdi-star-outline'
      };
      return icons[type] || 'mdi-information-outline';
    },
    formatDate(date) {
      if (!date) return '';
      const d = new Date(date);
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    renderMarkdown(content) {
      if (!content) return '';
      let md = require("markdown-it")();
      return md.render(content);
    }
  },
  computed: {
    isRight() {
      return function(index) {
        if (index % 2 == 0) {
          return "article-cover left-radius";
        }
        return "article-cover right-radius";
      };
    }
  }
};
</script>

<style lang="stylus">
.typed-cursor
  opacity: 1
  animation: blink 0.7s infinite
@keyframes blink
  0%
    opacity: 1
  50%
    opacity: 0
  100%
    opacity: 1
</style>

<style scoped>
.home-banner {
  position: absolute;
  top: -60px;
  left: 0;
  right: 0;
  height: 100vh;
  background: url("@/assets/img/ricksu-background.png") center
    center / cover no-repeat;
  background-color: #49b1f5;
  background-attachment: fixed;
  text-align: center;
  color: #fff !important;
  animation: header-effect 1s;
}
.banner-container {
  margin-top: 43vh;
  line-height: 1.5;
  color: #eee;
}
.blog-contact a {
  color: #fff !important;
}
.card-info-social {
  line-height: 40px;
  text-align: center;
  font-size: 1.5rem;
  margin: 6px 0 -6px;
}
.left-radius {
  border-radius: 8px 0 0 8px !important;
  order: 0;
}
.right-radius {
  border-radius: 0 8px 8px 0 !important;
  order: 1;
}
.article-wrapper {
  font-size: 14px;
}
@media (min-width: 760px) {
  .blog-title {
    font-size: 2.5rem;
  }
  .blog-intro {
    font-size: 1.5rem;
  }
  .blog-contact {
    display: none;
  }
  .home-container {
    max-width: 1200px;
    margin: calc(100vh - 48px) auto 28px auto;
    padding: 0 5px;
  }
  .article-card {
    display: flex;
    align-items: center;
    height: 280px;
    width: 100%;
    margin-top: 20px;
  }
  .article-cover {
    overflow: hidden;
    height: 100%;
    width: 45%;
  }
  .on-hover {
    transition: all 0.6s;
  }
  .article-card:hover .on-hover {
    transform: scale(1.1);
  }
  .article-wrapper {
    padding: 0 2.5rem;
    width: 55%;
  }
  .article-wrapper a {
    font-size: 1.5rem;
    transition: all 0.3s;
  }
}
@media (max-width: 759px) {
  .blog-title {
    font-size: 26px;
  }
  .blog-contact {
    font-size: 1.25rem;
    line-height: 2;
  }
  .home-container {
    width: 100%;
    margin: calc(100vh - 66px) auto 0 auto;
  }
  .article-card {
    margin-top: 1rem;
  }
  .article-cover {
    border-radius: 8px 8px 0 0 !important;
    height: 230px !important;
    width: 100%;
  }
  .article-cover div {
    border-radius: 8px 8px 0 0 !important;
  }
  .article-wrapper {
    padding: 1.25rem 1.25rem 1.875rem;
  }
  .article-wrapper a {
    font-size: 1.25rem;
    transition: all 0.3s;
  }
}
.scroll-down {
  cursor: pointer;
  position: absolute;
  bottom: 0;
  width: 100%;
}
.scroll-down i {
  font-size: 2rem;
}
.article-wrapper a:hover {
  color: #8e8cd8;
}
.article-info {
  font-size: 95%;
  color: #858585;
  line-height: 2;
  margin: 0.375rem 0;
}
.article-info a {
  font-size: 95%;
  color: #858585 !important;
}
.article-content {
  line-height: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
.blog-wrapper {
  position: sticky;
  top: 10px;
}
.blog-card {
  line-height: 2;
  padding: 1.25rem 1.5rem;
}
.author-wrapper {
  text-align: center;
}
.blog-info-wrapper {
  display: flex;
  justify-self: center;
  padding: 0.875rem 0;
}
.blog-info-data {
  flex: 1;
  text-align: center;
}
.blog-info-data a {
  text-decoration: none;
}
.collection-btn {
  text-align: center;
  z-index: 1;
  font-size: 14px;
  position: relative;
  display: block;
  background-color: #49b1f5;
  color: #fff !important;
  height: 32px;
  line-height: 32px;
  transition-duration: 1s;
  transition-property: color;
}
.collection-btn:before {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: -1;
  background: #ff7242;
  content: "";
  transition-timing-function: ease-out;
  transition-duration: 0.5s;
  transition-property: transform;
  transform: scaleX(0);
  transform-origin: 0 50%;
}
.collection-btn:hover:before {
  transition-timing-function: cubic-bezier(0.45, 1.64, 0.47, 0.66);
  transform: scaleX(1);
}
.author-avatar {
  transition: all 0.5s;
}
.author-avatar:hover {
  transform: rotate(360deg);
}
.web-info {
  padding: 0.25rem;
  font-size: 0.875rem;
}
.scroll-down-effects {
  color: #eee !important;
  text-align: center;
  text-shadow: 0.1rem 0.1rem 0.2rem rgba(0, 0, 0, 0.15);
  line-height: 1.5;
  display: inline-block;
  text-rendering: auto;
  -webkit-font-smoothing: antialiased;
  animation: scroll-down-effect 1.5s infinite;
}
@keyframes scroll-down-effect {
  0% {
    top: 0;
    opacity: 0.4;
    filter: alpha(opacity=40);
  }
  50% {
    top: -16px;
    opacity: 1;
    filter: none;
  }
  100% {
    top: 0;
    opacity: 0.4;
    filter: alpha(opacity=40);
  }
}
.big i {
  color: #f00;
  animation: big 0.8s linear infinite;
}
@keyframes big {
  0%,
  100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
}

/* 站点概览样式 - 从Article组件复制 */
.site-author {
  text-align: center;
  margin-bottom: 20px;
}

.author-avatar {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  border: 2px solid rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
  object-fit: cover;
}

.author-avatar:hover {
  transform: rotate(360deg);
  transition: transform 1s ease;
}

.author-name {
  font-size: 18px;
  font-weight: 500;
  margin: 15px 0 5px;
  color: #333;
}

.author-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
}

.site-state {
  display: flex;
  margin: 10px 0 20px;
  width: 100%;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}

.site-state-item {
  flex: 1;
  text-align: center;
  padding: 0 10px;
}

.site-state-item:not(:last-child) {
  border-right: 1px solid #eee;
}

.state-num {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.state-name {
  font-size: 13px;
  color: #999;
}

.site-social {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  margin: 15px 0;
}

.social-item {
  width: 50%;
  display: flex;
  align-items: center;
  padding: 5px 10px;
  color: #555;
  transition: color 0.3s;
}

.social-item:hover {
  color: #ff7e79;
}

.social-item i {
  margin-right: 5px;
  font-size: 16px;
}

/* 从Article复制的其他样式 */
.site-cc {
  margin: 15px 0;
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.site-cc a {
  display: inline-block;
  transition: transform 0.3s ease;
}

.site-cc a:hover {
  transform: translateY(-2px);
}

.site-cc img {
  vertical-align: middle;
}

/* 深色模式适配 */
.theme--dark .site-cc img,
[data-theme='dark'] .site-cc img,
body.theme-dark .site-cc img {
  filter: brightness(0.9);
}

.site-friends {
  width: 100%;
  margin-top: 15px;
  text-align: center;
}

.friends-title {
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
  margin-bottom: 10px;
  font-size: 14px;
  color: #666;
}

.friends-title i {
  margin-right: 5px;
}

.friends-links {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.friend-link {
  margin: 5px 0;
  padding: 5px 10px;
  color: #555;
  transition: color 0.3s;
}

.friend-link:hover {
  color: #ff7e79;
}

.emoji {
  margin: 0 3px;
}

/* 移除之前的简化版本样式 */
.site-social.simplified {
  display: none;
}

/* 公告板样式 - 现代时间线 */
.announcement-header {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  margin-bottom: 15px;
}

.announcement-title {
  font-size: 16px;
  font-weight: 500;
  margin-left: 10px;
  color: #49b1f5;
}

.announcement-content {
  padding: 0 5px;
}

/* 现代时间线样式 */
.modern-timeline {
  position: relative;
  padding: 5px 0;
}

.modern-timeline:before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 16px;
  width: 1px;
  background: #e0e0e0;
}

.timeline-item {
  position: relative;
  padding-left: 40px;
  margin-bottom: 25px;
}

.timeline-item:last-child {
  margin-bottom: 0;
}

.timeline-dot {
  position: absolute;
  left: 0;
  top: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #49b1f5;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  box-shadow: 0 2px 4px rgba(73,177,245,0.3);
}

.timeline-dot.secondary {
  background-color: #ff7242;
  box-shadow: 0 2px 4px rgba(255,114,66,0.3);
}

.timeline-card {
  position: relative;
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
  border-left: 3px solid #49b1f5;
  transition: transform 0.3s;
}

.timeline-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 10px rgba(0,0,0,0.08);
}

.timeline-time {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #888;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px dashed #eee;
}

.timeline-time i {
  margin-right: 4px;
}

.timeline-content {
  color: #444;
}

.update-link {
  color: #444;
  transition: color 0.3s;
  font-weight: 500;
}

.update-link:hover {
  color: #49b1f5;
}

/* Markdown 样式优化 */
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
}

.markdown-body h1, 
.markdown-body h2, 
.markdown-body h3 {
  font-weight: 500;
  margin-top: 12px;
  margin-bottom: 8px;
  color: #333;
}

.markdown-body p {
  margin-bottom: 10px;
}

.markdown-body ul {
  padding-left: 20px;
}

.markdown-body code {
  background-color: rgba(73,177,245,0.1);
  border-radius: 3px;
  padding: 2px 4px;
  font-family: Consolas, Monaco, 'Andale Mono', monospace;
  color: #49b1f5;
}

.markdown-body pre {
  background-color: #f8f9fb;
  border-radius: 4px;
  padding: 12px;
  overflow: auto;
  border-left: 2px solid #49b1f5;
}

/* 确保时间线图标始终为白色 */
.timeline-dot .v-icon {
  color: #ffffff !important;
}

/* 确保Markdown内容在深色模式下可见 */
.theme-dark .markdown-body,
[data-theme='dark'] .markdown-body {
  color: rgba(255,255,255,0.9) !important;
}

.theme-dark .timeline-content,
[data-theme='dark'] .timeline-content {
  color: rgba(255,255,255,0.9) !important;
}

/* 确保链接在深色模式下可见 */
.theme-dark .update-link,
[data-theme='dark'] .update-link {
  color: rgba(255,255,255,0.9) !important;
}

.theme-dark .update-link:hover,
[data-theme='dark'] .update-link:hover {
  color: #49b1f5 !important;
}

/* 时间线卡片在深色模式下的背景色 */
.theme-dark .timeline-card,
[data-theme='dark'] .timeline-card {
  background-color: rgba(255,255,255,0.08);
  box-shadow: 0 1px 6px rgba(0,0,0,0.2);
}

/* 时间线分隔线在深色模式下的颜色 */
.theme-dark .modern-timeline:before,
[data-theme='dark'] .modern-timeline:before {
  background: rgba(255,255,255,0.2);
}

/* 时间显示在深色模式下的颜色 */
.theme-dark .timeline-time,
[data-theme='dark'] .timeline-time {
  color: rgba(255,255,255,0.7);
  border-bottom: 1px dashed rgba(255,255,255,0.1);
}
</style>
