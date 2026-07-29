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

cd /Users/ricksu/WorkSpace/Personal/website/Suhao07.github.io

git pull --rebase origin main

## 编辑文章或网站配置
```bash
git status
git add .
git commit -m "content: 更新文章"
git push origin main
```
## 许可

本项目保留上游的 [Apache License 2.0](LICENSE)。
