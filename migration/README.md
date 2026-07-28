# 旧博客内容迁移

`legacy-content/docs/` 完整保留了旧 MkDocs 站点的文章和资源，便于审计与继续迁移。对外可访问的图片复制到 `blog-vue/blog/public/legacy-assets/`。

数据库初始化顺序：

1. `blog_mysql8.sql`：上游 FishBlog 表结构与基础权限数据。
2. `10-ricksu-bootstrap.sql`：清除上游演示内容、替换个人资料。
3. `20-legacy-articles.sql`：导入旧站 4 篇有效文章并修正图片和站内链接。

重新生成第 3 个文件：

```bash
python3 scripts/import_legacy_markdown.py
```

Docker Compose 首次创建 MySQL 数据卷时会按上述顺序自动执行。已有数据库不会自动重放初始化文件；请先备份，再手工执行相应 SQL。
