# 历史文档归档

这个目录用于保存不再对外发布、但仍希望保留在 Git 仓库中的 Markdown 文档。

可以按照年份或主题继续建立子目录，例如：

```text
content/archive/
├── 2024/
├── 2025/
└── paper-writing/
```

归档规则：

- 只有确认不再需要线上访问的文章才从 `content/posts/` 移入这里。
- 文件移入本目录后不会参与网站构建，也不会出现在 Pages CMS 的“博客文章”中。
- 归档会使原来的 `#/articles/<slug>` 链接失效。
- 如果文章仍要在线显示，请继续放在 `content/posts/`，并使用 `category` 和 `tags`
  整理。
- 如果只是暂时隐藏，并且以后还要通过 Pages CMS 编辑，请把文件留在
  `content/posts/`，设置 `published: false`。

恢复归档文章时，将文件移回 `content/posts/`，检查 `slug` 没有与现有文章重复，
更新日期和发布状态后再提交。
