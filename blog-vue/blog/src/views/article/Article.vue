<template>
  <div>
    <div class="article-banner" :style="articleCover">
      <div class="banner-shade"></div>
      <div class="article-heading">
        <h1>{{ article.articleTitle }}</h1>
        <div class="article-meta">
          <span>
            <v-icon dark small>mdi-calendar-month-outline</v-icon>
            {{ article.createTime | date }}
          </span>
          <span>
            <v-icon dark small>mdi-update</v-icon>
            {{ article.updateTime | date }}
          </span>
          <router-link :to="'/categories/' + article.categoryId">
            <v-icon dark small>mdi-inbox-full</v-icon>
            {{ article.categoryName }}
          </router-link>
          <span>
            <v-icon dark small>mdi-text</v-icon>
            {{ wordCount }} 字 · {{ readTime }}
          </span>
        </div>
      </div>
    </div>

    <v-row class="article-container">
      <v-col md="3" cols="12" class="d-md-block d-none">
        <div class="sidebar-sticky">
          <v-card class="side-card">
            <div class="side-title">
              <v-icon size="19" color="#49b1f5">mdi-format-list-bulleted</v-icon>
              文章目录
            </div>
            <button
              v-for="(item, index) in tocItems"
              :key="`${item.text}-${index}`"
              class="toc-link"
              :style="{ paddingLeft: `${12 + (item.level - 1) * 12}px` }"
              @click="scrollToHeading(index)"
            >
              {{ item.text }}
            </button>
            <div v-if="!tocItems.length" class="empty-toc">本文暂无目录</div>
          </v-card>

          <v-card class="side-card latest-card">
            <div class="side-title">
              <v-icon size="19" color="#49b1f5">mdi-clock-outline</v-icon>
              最新文章
            </div>
            <router-link
              v-for="item in latestArticles"
              :key="item.id"
              :to="'/articles/' + item.id"
              class="latest-item"
            >
              <img :src="item.articleCover" :alt="item.articleTitle" />
              <span>{{ item.articleTitle }}</span>
            </router-link>
          </v-card>
        </div>
      </v-col>

      <v-col md="9" cols="12">
        <v-card class="article-card">
          <article class="article-content markdown-body">
            <Viewer :value="article.articleContent || ''" :plugins="plugins" />
          </article>

          <div class="article-end">— 感谢阅读 —</div>

          <div class="copyright-card">
            <div><strong>作者：</strong>{{ site.nickname || "RickSu" }}</div>
            <div class="copyright-link">
              <strong>链接：</strong>
              <a :href="articleHref">{{ articleHref }}</a>
            </div>
            <div>
              <strong>许可：</strong>
              <a
                href="https://creativecommons.org/licenses/by-nc-sa/4.0/"
                target="_blank"
                rel="noopener noreferrer"
              >
                CC BY-NC-SA 4.0
              </a>
            </div>
          </div>

          <div class="article-footer-row">
            <div class="tag-list">
              <router-link
                v-for="tag in article.tagDTOList"
                :key="tag.id"
                :to="'/tags/' + tag.id"
                class="tag-pill"
              >
                <v-icon dark x-small>mdi-pound</v-icon>{{ tag.tagName }}
              </router-link>
            </div>
            <button class="share-button" @click="copyLink">
              <v-icon dark small>mdi-link-variant</v-icon>
              {{ copied ? "已复制" : "复制链接" }}
            </button>
          </div>

          <div
            v-if="article.lastArticle.id || article.nextArticle.id"
            class="post-navigation"
          >
            <router-link
              v-if="article.lastArticle.id"
              :to="'/articles/' + article.lastArticle.id"
              class="post-navigation-item"
            >
              <span>上一篇</span>
              <strong>{{ article.lastArticle.articleTitle }}</strong>
            </router-link>
            <router-link
              v-if="article.nextArticle.id"
              :to="'/articles/' + article.nextArticle.id"
              class="post-navigation-item next"
            >
              <span>下一篇</span>
              <strong>{{ article.nextArticle.articleTitle }}</strong>
            </router-link>
          </div>

          <section
            v-if="article.articleRecommendList.length"
            class="recommend-section"
          >
            <h2>
              <v-icon color="#49b1f5">mdi-book-open-page-variant</v-icon>
              相关推荐
            </h2>
            <v-row>
              <v-col
                v-for="item in article.articleRecommendList"
                :key="item.id"
                md="4"
                cols="12"
              >
                <router-link
                  :to="'/articles/' + item.id"
                  class="recommend-card"
                >
                  <img :src="item.articleCover" :alt="item.articleTitle" />
                  <div>
                    <strong>{{ item.articleTitle }}</strong>
                    <span>{{ item.createTime | date }}</span>
                  </div>
                </router-link>
              </v-col>
            </v-row>
          </section>
        </v-card>
      </v-col>
    </v-row>

    <v-snackbar v-model="snackbar" top color="#49b1f5" :timeout="1800">
      文章链接已复制
    </v-snackbar>
  </div>
</template>

<script>
import { Viewer } from "@bytemd/vue";
import "bytemd/dist/index.css";
import gfm from "@bytemd/plugin-gfm";
import gemoji from "@bytemd/plugin-gemoji";
import highlight from "@bytemd/plugin-highlight";
import math from "@bytemd/plugin-math";
import mediumZoom from "@bytemd/plugin-medium-zoom";
import mermaid from "@bytemd/plugin-mermaid";
import "katex/dist/katex.css";
import "github-markdown-css/github-markdown.css";

const emptyArticle = () => ({
  id: "",
  articleTitle: "文章加载中",
  articleContent: "",
  articleCover: "",
  createTime: "",
  updateTime: "",
  categoryId: 0,
  categoryName: "",
  tagDTOList: [],
  lastArticle: { id: 0, articleTitle: "", articleCover: "" },
  nextArticle: { id: 0, articleTitle: "", articleCover: "" },
  articleRecommendList: []
});

export default {
  name: "Article",
  components: { Viewer },
  data() {
    return {
      article: emptyArticle(),
      latestArticles: [],
      site: {},
      copied: false,
      snackbar: false,
      plugins: [
        gfm(),
        gemoji(),
        highlight(),
        math({ katexOptions: { throwOnError: false } }),
        mermaid({ theme: "default" }),
        mediumZoom()
      ]
    };
  },
  computed: {
    articleCover() {
      const cover =
        this.article.articleCover ||
        require("@/assets/img/ricksu-background2.png");
      return {
        backgroundImage: `linear-gradient(135deg, rgba(19, 31, 45, .2), rgba(20, 44, 73, .48)), url("${cover}")`
      };
    },
    articleHref() {
      return window.location.href;
    },
    wordCount() {
      const text = String(this.article.articleContent || "")
        .replace(/```[\s\S]*?```/g, "")
        .replace(/[#>*_`|~\[\]()!-]/g, " ")
        .trim();
      const chinese = (text.match(/[\u3400-\u9fff]/g) || []).length;
      const words = (text.match(/[a-zA-Z0-9]+/g) || []).length;
      return chinese + words;
    },
    readTime() {
      return `${Math.max(1, Math.ceil(this.wordCount / 400))} 分钟`;
    },
    tocItems() {
      return String(this.article.articleContent || "")
        .split("\n")
        .map(line => {
          const match = line.match(/^(#{1,4})\s+(.+)$/);
          return match
            ? {
                level: match[1].length,
                text: match[2].replace(/[*_`]/g, "").trim()
              }
            : null;
        })
        .filter(Boolean);
    }
  },
  watch: {
    "$route.params.articleId"() {
      this.loadArticle();
    }
  },
  created() {
    this.loadArticle();
    this.loadSidebar();
  },
  methods: {
    async loadArticle() {
      this.article = emptyArticle();
      const { data } = await this.axios.get(
        `/api/articles/${this.$route.params.articleId}`
      );
      if (!data.flag || !data.data) {
        this.$router.replace("/");
        return;
      }
      this.article = data.data;
      document.title = `${this.article.articleTitle} - RickSu`;
    },
    async loadSidebar() {
      const [{ data: newest }, { data: blogInfo }] = await Promise.all([
        this.axios.get("/api/articles/newest"),
        this.axios.get("/api/")
      ]);
      this.latestArticles = newest.data || [];
      this.site = blogInfo.data || {};
      this.$store.commit("checkBlogInfo", this.site);
    },
    scrollToHeading(index) {
      this.$nextTick(() => {
        const headings = this.$el.querySelectorAll(
          ".article-content h1, .article-content h2, .article-content h3, .article-content h4"
        );
        if (!headings[index]) return;
        const top =
          headings[index].getBoundingClientRect().top +
          window.pageYOffset -
          82;
        window.scrollTo({ top, behavior: "smooth" });
      });
    },
    async copyLink() {
      try {
        await navigator.clipboard.writeText(window.location.href);
      } catch (error) {
        const input = document.createElement("textarea");
        input.value = window.location.href;
        document.body.appendChild(input);
        input.select();
        document.execCommand("copy");
        input.remove();
      }
      this.copied = true;
      this.snackbar = true;
      window.setTimeout(() => {
        this.copied = false;
      }, 1800);
    }
  }
};
</script>

<style scoped>
.article-banner {
  position: relative;
  height: 420px;
  margin-top: -60px;
  padding-top: 60px;
  box-sizing: border-box;
  background-position: center;
  background-size: cover;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.article-heading {
  position: relative;
  z-index: 1;
  width: min(920px, calc(100% - 40px));
  text-align: center;
  text-shadow: 0 2px 16px rgba(0, 0, 0, 0.38);
}

.article-heading h1 {
  margin-bottom: 18px;
  font-size: clamp(30px, 5vw, 46px);
  line-height: 1.25;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px 18px;
  font-size: 14px;
}

.article-meta span,
.article-meta a {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  color: #fff;
}

.article-container {
  max-width: 1180px;
  margin: -44px auto 48px;
  padding: 0 18px;
  position: relative;
  z-index: 2;
}

.sidebar-sticky {
  position: sticky;
  top: 78px;
}

.side-card,
.article-card {
  border-radius: 14px !important;
  box-shadow: 0 10px 34px rgba(31, 50, 70, 0.1) !important;
}

.side-card {
  padding: 18px;
}

.side-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #344c67;
  font-weight: 700;
}

.toc-link {
  display: block;
  width: 100%;
  padding-top: 6px;
  padding-bottom: 6px;
  border-radius: 7px;
  color: #617386;
  font-size: 13px;
  text-align: left;
  transition: 0.2s ease;
}

.toc-link:hover {
  color: #49b1f5;
  background: rgba(73, 177, 245, 0.08);
}

.empty-toc {
  color: #9aa8b5;
  font-size: 13px;
}

.latest-card {
  margin-top: 18px;
}

.latest-item {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 7px 0;
  color: #55687b;
  font-size: 13px;
  line-height: 1.4;
}

.latest-item img {
  width: 58px;
  height: 42px;
  border-radius: 7px;
  object-fit: cover;
}

.article-card {
  padding: clamp(22px, 5vw, 54px);
}

.article-content {
  min-height: 220px;
}

.article-content ::v-deep .markdown-body {
  color: inherit;
  background: transparent;
}

.article-content ::v-deep .katex,
.article-content ::v-deep .katex * {
  color: #2f3d4a !important;
}

.article-content ::v-deep img {
  max-width: 100%;
  border-radius: 10px;
}

.article-content ::v-deep pre {
  overflow-x: auto;
  border-radius: 8px;
  background: #282c34 !important;
}

.article-content ::v-deep pre code,
.article-content ::v-deep pre code.hljs {
  display: block;
  padding: 0;
  color: #abb2bf !important;
  background: transparent !important;
  white-space: pre;
}

.article-end {
  margin: 44px 0 24px;
  color: #91a0ad;
  text-align: center;
  letter-spacing: 0.15em;
}

.copyright-card {
  padding: 18px 20px;
  border-left: 4px solid #49b1f5;
  border-radius: 7px;
  background: rgba(73, 177, 245, 0.08);
  color: #617386;
  line-height: 1.9;
}

.copyright-link {
  overflow-wrap: anywhere;
}

.article-footer-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  margin-top: 24px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-pill,
.share-button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 7px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #49b1f5, #8e8cd8);
  color: #fff !important;
  font-size: 13px;
}

.share-button {
  white-space: nowrap;
}

.post-navigation {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 34px;
}

.post-navigation-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 98px;
  padding: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #eef8ff, #f2f1ff);
  color: #344c67;
}

.post-navigation-item.next {
  text-align: right;
}

.post-navigation-item span {
  color: #8b9aa8;
  font-size: 12px;
}

.recommend-section {
  margin-top: 38px;
}

.recommend-section h2 {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
  color: #344c67;
  font-size: 20px;
}

.recommend-card {
  display: block;
  height: 100%;
  overflow: hidden;
  border: 1px solid rgba(73, 177, 245, 0.15);
  border-radius: 11px;
  color: #344c67;
  transition: 0.2s ease;
}

.recommend-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 9px 22px rgba(31, 50, 70, 0.1);
}

.recommend-card img {
  width: 100%;
  height: 120px;
  object-fit: cover;
}

.recommend-card div {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 12px;
}

.recommend-card span {
  color: #91a0ad;
  font-size: 12px;
}

@media (max-width: 759px) {
  .article-banner {
    height: 340px;
  }

  .article-heading {
    margin-top: 24px;
  }

  .article-meta {
    font-size: 12px;
  }

  .article-container {
    margin-top: -24px;
    padding: 0 10px;
  }

  .article-card {
    padding: 22px 17px;
    border-radius: 12px !important;
  }

  .article-footer-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .post-navigation {
    grid-template-columns: 1fr;
  }

  .post-navigation-item.next {
    text-align: left;
  }
}

body.theme-dark .side-card,
body.theme-dark .article-card {
  background: #1e1e1e !important;
}

body.theme-dark .side-title,
body.theme-dark .recommend-section h2,
body.theme-dark .post-navigation-item,
body.theme-dark .recommend-card {
  color: #f1f5f9;
}

body.theme-dark .toc-link,
body.theme-dark .latest-item,
body.theme-dark .copyright-card {
  color: #c5ced8;
}

body.theme-dark .copyright-card,
body.theme-dark .post-navigation-item {
  background: rgba(73, 177, 245, 0.1);
}

body.theme-dark .article-content ::v-deep .katex,
body.theme-dark .article-content ::v-deep .katex *,
[data-theme="dark"] .article-content ::v-deep .katex,
[data-theme="dark"] .article-content ::v-deep .katex * {
  color: #f1f5f9 !important;
}

/* 阅读页的深色模式：组件自身的浅色配色不再覆盖主题。 */
body.theme-dark .article-card,
[data-theme="dark"] .article-card,
body.theme-dark .side-card,
[data-theme="dark"] .side-card {
  background: #1c222b !important;
  color: #e6edf3 !important;
}

body.theme-dark .article-content,
[data-theme="dark"] .article-content,
body.theme-dark .article-content ::v-deep .markdown-body,
[data-theme="dark"] .article-content ::v-deep .markdown-body {
  background: transparent !important;
  color: #e6edf3 !important;
}

body.theme-dark .article-content ::v-deep h1,
body.theme-dark .article-content ::v-deep h2,
body.theme-dark .article-content ::v-deep h3,
body.theme-dark .article-content ::v-deep h4,
[data-theme="dark"] .article-content ::v-deep h1,
[data-theme="dark"] .article-content ::v-deep h2,
[data-theme="dark"] .article-content ::v-deep h3,
[data-theme="dark"] .article-content ::v-deep h4 {
  color: #f8fafc !important;
}

body.theme-dark .side-title,
body.theme-dark .toc-link,
body.theme-dark .latest-item,
body.theme-dark .copyright-card,
[data-theme="dark"] .side-title,
[data-theme="dark"] .toc-link,
[data-theme="dark"] .latest-item,
[data-theme="dark"] .copyright-card {
  color: #cbd5e1 !important;
}

body.theme-dark .post-navigation-item,
body.theme-dark .recommend-card,
[data-theme="dark"] .post-navigation-item,
[data-theme="dark"] .recommend-card {
  border-color: rgba(125, 211, 252, 0.28);
  background: #263241;
  color: #e6edf3 !important;
}

body.theme-dark .post-navigation-item span,
body.theme-dark .recommend-card span,
body.theme-dark .article-end,
[data-theme="dark"] .post-navigation-item span,
[data-theme="dark"] .recommend-card span,
[data-theme="dark"] .article-end {
  color: #94a3b8 !important;
}

body.theme-dark .article-content ::v-deep a,
[data-theme="dark"] .article-content ::v-deep a {
  color: #7dd3fc !important;
}
</style>
