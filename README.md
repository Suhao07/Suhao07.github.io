# RickSu 技术博客

这是 RickSu 的零服务器技术博客。界面基于
[Auroral0810/fishblog_tpl](https://github.com/Auroral0810/fishblog_tpl)
改造，使用 GitHub Pages 免费托管，并通过 Pages CMS 在电脑或手机浏览器中管理
Markdown 文章。

## 当前架构

- `GitHub Pages`：免费托管与 HTTPS。
- `GitHub Actions`：文章变更后自动生成数据并发布网站。
- `Pages CMS`：手机和电脑上的文章编辑界面。
- `content/posts/`：所有文章的 Markdown 源文件。
- `content/site.json`：网站标题、简介、头像和公告。
- `blog-vue/blog/`：Vue 2 博客前台。

网站运行不需要 Java、Maven、Docker、MySQL、Redis、RabbitMQ 或
Elasticsearch。仓库中的 `blog-springboot/`、`blog-vue/admin/` 和服务器部署文件
仅作为原模板归档，不参与构建或发布。

## 首次启用 GitHub Pages

1. 将本仓库的改动推送到 `main` 分支。
2. 打开 GitHub 仓库的 `Settings → Pages`。
3. 在 `Build and deployment → Source` 中选择 `GitHub Actions`。
4. 打开 `Actions`，等待“发布 GitHub Pages”任务完成。
5. 访问 <https://suhao07.github.io/>。

工作流位于 `.github/workflows/deploy-pages.yml`。每次更新文章并提交后，GitHub
都会自动重新构建并发布。

## 手机写文章

1. 在手机浏览器打开 <https://app.pagescms.org/>。
2. 使用 GitHub 账号登录。
3. 首次使用时安装 Pages CMS GitHub App，并只授权
   `Suhao07/Suhao07.github.io` 仓库。
4. 选择仓库和 `main` 分支。
5. 打开“博客文章”，新建或编辑文章。
6. 点击保存；GitHub Actions 会自动发布，通常几分钟内生效。

编辑界面由仓库根目录的 `.pages.yml` 定义。文章字段包括标题、网址、发布日期、
分类、标签、摘要、封面、置顶、发布状态和 Markdown 正文。将“已发布”关闭可保留
草稿而不显示在网站中。

### 文章网址填写规则

`slug` 只使用小写英文字母、数字和连字符，例如：

```text
my-first-post
```

发布后尽量不要修改，否则旧链接会失效。

## 上传图片

在 Pages CMS 的封面或正文编辑器中上传图片，文件会保存到：

```text
blog-vue/blog/public/uploads/
```

文章中记录的公开地址为 `/uploads/文件名`。图片和文章会在同一次提交后自动发布。

## 内容目录约定

Markdown 内容按照用途划分：

```text
content/
├── posts/       # 网站文章以及 Pages CMS 可编辑的草稿
├── drafts/      # 仅在本地维护、不参与构建的草稿
├── archive/     # 不再发布的历史文档，可按年份或主题建立子目录
├── templates/   # 可复制的文章模板
└── site.json    # 网站标题、简介、头像和公告
```

当前构建脚本只扫描 `content/posts/` 第一层的 Markdown 文件。因此需要在线显示的文章
必须直接放在 `content/posts/`，不能再放入它的子目录。网站通过文章的 `category`
和 `tags` 字段进行逻辑分类。

归档与草稿的具体规则分别记录在 `content/archive/README.md` 和
`content/drafts/README.md`。可复制的新文章模板位于
`content/templates/article-template.md`。

## 本地维护文章

在本地新增或修改文章时，只需要维护 `content/` 目录，不需要修改 Vue 源码。建议先
同步远端更新，再开始编辑：

```bash
cd /Users/ricksu/WorkSpace/Personal/website/Suhao07.github.io
git pull --rebase origin main
```

如果 Pages CMS 已经产生了新提交，这一步会先把手机或网页端的修改同步到本地，避免
后续推送时发生冲突。

### 新增文章

在 `content/posts/` 中新建一个 Markdown 文件。文件名建议与文章的 `slug` 保持
一致，例如：

```text
content/posts/my-new-article.md
```

可以复制 `content/templates/article-template.md`，也可以直接使用下面的模板：

```markdown
---
title: "我的新文章"
slug: "my-new-article"
date: "2026-07-29"
updated: "2026-07-29"
category: "技术笔记"
tags: "GitHub, 博客"
summary: "这篇文章的简短介绍。"
cover: "/uploads/my-cover.png"
top: false
published: true
---

# 我的新文章

这里是文章开头。

## 1. 第一章节

章节内容。

### 1.1 子章节

子章节内容。

## 2. 第二章节

章节内容。

## 3. 总结

总结内容。
```

字段说明：

- `slug` 只能使用小写英文字母、数字和连字符，并且不能与其他文章重复。
- `category` 决定文章所属分类；填写新的名称会自动创建新分类。
- `tags` 使用英文或中文逗号分隔多个标签。
- `cover` 是封面图片的公开路径。
- `top: true` 会将文章置顶。
- `published: false` 可以把文章保留为草稿，不在网站中显示。

### 插入章节

文章正文使用 Markdown 标题表示章节：

```markdown
## 1. 主要章节

### 1.1 子章节

#### 1.1.1 更小的章节
```

文章标题通常使用一个 `#`，正文主要章节从 `##` 开始。

### 本地插入图片

将图片复制到：

```text
blog-vue/blog/public/uploads/
```

然后在文章中使用公开路径引用：

```markdown
![图片说明](/uploads/my-image.png)
```

文件名建议使用小写英文、数字和连字符，避免空格和特殊字符。

### 更新手工内容导航

分类页会根据文章的 `category` 自动整理，一般不需要手动维护链接。如果希望新文章
同时出现在 `Tools & Tech` 的手工内容导航中，则编辑
`content/posts/tools-and-tech.md`，加入：

```markdown
### 新章节

- [我的新文章](#/articles/my-new-article) - 文章简介
```

其中链接末尾必须与目标文章的 `slug` 一致。

### 提交并发布

完成编辑后检查、提交并推送：

```bash
git status
git add content blog-vue/blog/public/uploads
git commit -m "content: 新增或更新文章"
git push origin main
```

推送后 GitHub Actions 会自动构建并发布网站，通常 1–3 分钟后生效。本地只编辑文章
时不要求安装 Node.js；Node.js 仅用于本地预览和修改前台界面。

## 自定义域名

买到域名后：

1. 在 GitHub 仓库 `Settings → Pages → Custom domain` 中填写域名。
2. 按 GitHub 提示到域名商添加 DNS 记录。
3. 等待 DNS 生效后启用 `Enforce HTTPS`。
4. 在 Pages CMS 的“网站设置”里把“网站地址”改成正式 HTTPS 域名。

建议同时设置根域名和 `www`，并让其中一个跳转到另一个。不要手工把密码、令牌或
DNS 密钥提交到仓库。

## 本地预览

本地需要 Node.js 20：

```bash
cd blog-vue/blog
npm ci
npm run serve
```

打开 <http://localhost:8081/>。

内容生成命令：

```bash
cd blog-vue/blog
npm run content:build
```

它会读取 `content/posts/*.md` 和 `content/site.json`，生成前台内容索引与
`public/rss.xml`。`npm run serve` 和 `npm run build` 都会自动运行这一步。

## 内容与旧站归档

- 原 MkDocs 内容保存在 `legacy-content/docs/`。
- 4 篇已有文章已经迁移到 `content/posts/`。
- 旧站图片继续由 `blog-vue/blog/public/legacy-assets/` 提供。
- `blog-springboot/` 和 `blog-vue/admin/` 不会被 GitHub Pages 工作流部署。

## 许可

本项目保留上游的 [Apache License 2.0](LICENSE)。
