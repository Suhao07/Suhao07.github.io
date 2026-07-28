import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const appRoot = path.resolve(here, "..");
const repoRoot = path.resolve(appRoot, "..", "..");
const postsDir = path.join(repoRoot, "content", "posts");
const siteFile = path.join(repoRoot, "content", "site.json");
const outputDir = path.join(appRoot, "src", "generated");
const outputFile = path.join(outputDir, "content.json");
const rssFile = path.join(appRoot, "public", "rss.xml");

function parseScalar(raw) {
  const value = raw.trim();
  if (!value) return "";
  if (value === "true") return true;
  if (value === "false") return false;
  if (value === "null") return null;
  if (
    (value.startsWith('"') && value.endsWith('"')) ||
    (value.startsWith("'") && value.endsWith("'"))
  ) {
    if (value.startsWith('"')) return JSON.parse(value);
    return value.slice(1, -1).replace(/''/g, "'");
  }
  return value;
}

function parseMarkdown(filePath) {
  const source = fs.readFileSync(filePath, "utf8").replace(/\r\n/g, "\n");
  const match = source.match(/^---\n([\s\S]*?)\n---\n?([\s\S]*)$/);
  if (!match) {
    throw new Error(`${path.relative(repoRoot, filePath)} 缺少 YAML frontmatter`);
  }

  const attributes = {};
  for (const line of match[1].split("\n")) {
    if (!line.trim() || line.trimStart().startsWith("#")) continue;
    const separator = line.indexOf(":");
    if (separator === -1) continue;
    const key = line.slice(0, separator).trim();
    attributes[key] = parseScalar(line.slice(separator + 1));
  }

  return { attributes, body: match[2].trim() + "\n" };
}

function splitTags(value) {
  return String(value || "")
    .split(/[,，]/)
    .map(tag => tag.trim())
    .filter(Boolean);
}

function dateOnly(value, fieldName) {
  const raw = String(value || "").trim();
  const directMatch = raw.match(/^(\d{4}-\d{2}-\d{2})/);
  if (directMatch) return directMatch[1];

  const parsed = new Date(raw);
  if (!Number.isNaN(parsed.getTime())) {
    return parsed.toISOString().slice(0, 10);
  }
  throw new Error(`${fieldName} 不是合法日期：${raw}`);
}

function plainText(markdown) {
  return markdown
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/!\[[^\]]*\]\([^)]+\)/g, " ")
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/<[^>]+>/g, " ")
    .replace(/[#>*_`|~-]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function xml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

const site = JSON.parse(fs.readFileSync(siteFile, "utf8"));
const fileNames = fs
  .readdirSync(postsDir)
  .filter(name => name.endsWith(".md"))
  .sort();

const parsedPosts = fileNames.map(fileName => {
  const filePath = path.join(postsDir, fileName);
  const { attributes, body } = parseMarkdown(filePath);
  const required = ["title", "slug", "date", "category"];
  for (const key of required) {
    if (!attributes[key]) {
      throw new Error(`${path.relative(repoRoot, filePath)} 缺少 ${key}`);
    }
  }
  if (!/^[a-z0-9-]+$/.test(attributes.slug)) {
    throw new Error(`${attributes.slug} 不是合法 slug`);
  }

  return {
    ...attributes,
    body,
    date: dateOnly(attributes.date, `${attributes.slug}.date`),
    updated: attributes.updated
      ? dateOnly(attributes.updated, `${attributes.slug}.updated`)
      : dateOnly(attributes.date, `${attributes.slug}.date`),
    tags: splitTags(attributes.tags),
    published: attributes.published !== false,
    top: attributes.top === true,
    cover:
      attributes.cover || "/legacy-assets/assets/images/background2.png",
    summary:
      attributes.summary || plainText(body).slice(0, 180) || "技术与研究笔记"
  };
});

const duplicateSlugs = parsedPosts
  .map(post => post.slug)
  .filter((slug, index, all) => all.indexOf(slug) !== index);
if (duplicateSlugs.length) {
  throw new Error(`文章网址重复：${[...new Set(duplicateSlugs)].join(", ")}`);
}

const publishedPosts = parsedPosts
  .filter(post => post.published)
  .sort((a, b) => {
    if (a.top !== b.top) return Number(b.top) - Number(a.top);
    return (
      new Date(b.date).getTime() - new Date(a.date).getTime() ||
      a.title.localeCompare(b.title, "zh-CN")
    );
  });

const categoryNames = [
  ...new Set(publishedPosts.map(post => post.category))
].sort((a, b) => a.localeCompare(b, "zh-CN"));
const tagNames = [...new Set(publishedPosts.flatMap(post => post.tags))].sort(
  (a, b) => a.localeCompare(b, "zh-CN")
);

const categories = categoryNames.map((categoryName, index) => ({
  id: index + 1,
  categoryName,
  articleCount: publishedPosts.filter(post => post.category === categoryName)
    .length
}));
const tags = tagNames.map((tagName, index) => ({
  id: index + 1,
  tagName,
  articleCount: publishedPosts.filter(post => post.tags.includes(tagName))
    .length
}));

const articles = publishedPosts.map(post => {
  const category = categories.find(
    item => item.categoryName === post.category
  );
  return {
    id: post.slug,
    slug: post.slug,
    articleTitle: post.title,
    articleContent: post.body,
    articleCover: post.cover,
    summary: post.summary,
    createTime: `${post.date}T00:00:00`,
    updateTime: `${post.updated || post.date}T00:00:00`,
    categoryId: category.id,
    categoryName: category.categoryName,
    tagDTOList: post.tags.map(tagName => {
      const tag = tags.find(item => item.tagName === tagName);
      return { id: tag.id, tagName: tag.tagName };
    }),
    isTop: post.top ? 1 : 0,
    viewsCount: 0,
    likeCount: 0
  };
});

const articleDetails = articles.map((article, index) => {
  const related = articles
    .filter(candidate => {
      if (candidate.id === article.id) return false;
      const articleTagIds = new Set(article.tagDTOList.map(tag => tag.id));
      return (
        candidate.categoryId === article.categoryId ||
        candidate.tagDTOList.some(tag => articleTagIds.has(tag.id))
      );
    })
    .slice(0, 3)
    .map(candidate => ({
      id: candidate.id,
      articleTitle: candidate.articleTitle,
      articleCover: candidate.articleCover,
      createTime: candidate.createTime
    }));

  const compact = candidate =>
    candidate
      ? {
          id: candidate.id,
          articleTitle: candidate.articleTitle,
          articleCover: candidate.articleCover
        }
      : { id: 0, articleTitle: "", articleCover: "" };

  return {
    ...article,
    lastArticle: compact(articles[index + 1]),
    nextArticle: compact(articles[index - 1]),
    articleRecommendList: related
  };
});

const content = {
  site: {
    ...site,
    startDate: dateOnly(site.startDate || "2025-03-26", "site.startDate"),
    articleCount: articles.length,
    categoryCount: categories.length,
    tagCount: tags.length,
    viewsCount: 0,
    websiteName: site.title,
    notices: site.announcement
      ? [
          {
            id: 1,
            content: site.announcement,
            createTime: `${dateOnly(
              site.startDate || "2025-03-26",
              "site.startDate"
            )}T00:00:00`
          }
        ]
      : []
  },
  articles,
  articleDetails,
  categories,
  tags,
  links: site.github
    ? [
        {
          id: 1,
          linkName: "GitHub",
          linkAvatar: site.avatar,
          linkAddress: site.github,
          linkIntro: `${site.nickname} 的代码与项目`
        }
      ]
    : []
};

fs.mkdirSync(outputDir, { recursive: true });
fs.writeFileSync(outputFile, `${JSON.stringify(content, null, 2)}\n`);

const baseUrl = String(site.url || "https://suhao07.github.io").replace(
  /\/$/,
  ""
);
const rssItems = articles
  .map(
    article => `    <item>
      <title>${xml(article.articleTitle)}</title>
      <link>${xml(`${baseUrl}/#/articles/${article.id}`)}</link>
      <guid>${xml(`${baseUrl}/#/articles/${article.id}`)}</guid>
      <pubDate>${new Date(article.createTime).toUTCString()}</pubDate>
      <description>${xml(article.summary)}</description>
    </item>`
  )
  .join("\n");
const rss = `<?xml version="1.0" encoding="UTF-8" ?>
<rss version="2.0">
  <channel>
    <title>${xml(site.title)}</title>
    <link>${xml(baseUrl)}</link>
    <description>${xml(site.intro)}</description>
${rssItems}
  </channel>
</rss>
`;
fs.writeFileSync(rssFile, rss);

process.stdout.write(`已生成 ${articles.length} 篇文章的静态内容索引\n`);
