<template>
  <div>
    <div class="about-banner banner">
      <h1 class="banner-title">关于我</h1>
    </div>
    <v-card class="blog-container about-card">
      <div class="profile">
        <v-avatar size="112" class="profile-avatar">
          <img src="/legacy-assets/static/images/logo.png" alt="RickSu" />
        </v-avatar>
        <div>
          <p class="eyebrow">TECH · RESEARCH · PRACTICE</p>
          <h2>RickSu</h2>
          <p class="intro">
            这里记录日常技术实践、研究方法和论文写作经验。旧 MkDocs
            站点的有效内容已经迁移到文章、分类与标签体系中。
          </p>
          <a
            href="https://github.com/Suhao07"
            target="_blank"
            rel="noopener noreferrer"
            class="github-link"
          >
            <v-icon size="19">mdi-github</v-icon>
            github.com/Suhao07
          </a>
        </div>
      </div>

      <v-divider class="section-divider" />

      <v-row>
        <v-col v-for="item in topics" :key="item.title" md="4" cols="12">
          <div class="topic-card">
            <v-icon color="#49b1f5" size="28">{{ item.icon }}</v-icon>
            <h3>{{ item.title }}</h3>
            <p>{{ item.description }}</p>
          </div>
        </v-col>
      </v-row>
    </v-card>
  </div>
</template>

<script>
export default {
  name: "About",
  data() {
    return {
      topics: [
        {
          icon: "mdi-tools",
          title: "工具与技术",
          description: "整理开发工具、工程实践与可复用的技术经验。"
        },
        {
          icon: "mdi-file-document-edit-outline",
          title: "论文写作",
          description: "记录 LaTeX、Overleaf 与科研写作方法。"
        },
        {
          icon: "mdi-robot-industrial-outline",
          title: "机器人研究",
          description: "沉淀 IEEE RAL 投稿和机器人方向的研究笔记。"
        }
      ]
    };
  }
};
</script>

<style scoped>
.about-banner {
  background:
    linear-gradient(135deg, rgba(18, 31, 46, 0.58), rgba(73, 177, 245, 0.28)),
    url("@/assets/img/ricksu-background2.png") center center / cover no-repeat;
}

.about-card {
  overflow: hidden;
}

.profile {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 30px;
  align-items: center;
}

.profile-avatar {
  box-shadow: 0 12px 30px rgba(31, 67, 97, 0.18);
}

.eyebrow {
  margin-bottom: 7px;
  color: #49b1f5;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.13em;
}

h2 {
  margin: 0 0 12px;
  color: #25364a;
  font-size: 32px;
}

.intro {
  max-width: 680px;
  margin-bottom: 16px;
  color: #637487;
  font-size: 16px;
  line-height: 1.9;
}

.github-link {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: #344c67;
  font-weight: 600;
}

.section-divider {
  margin: 32px 0 20px;
}

.topic-card {
  height: 100%;
  padding: 24px;
  border: 1px solid rgba(73, 177, 245, 0.16);
  border-radius: 14px;
  background: rgba(73, 177, 245, 0.05);
}

.topic-card h3 {
  margin: 14px 0 8px;
  color: #344c67;
}

.topic-card p {
  margin: 0;
  color: #718093;
  line-height: 1.7;
}

@media (max-width: 759px) {
  .profile {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .profile-avatar {
    margin: 0 auto;
  }

  .github-link {
    justify-content: center;
  }
}
</style>
