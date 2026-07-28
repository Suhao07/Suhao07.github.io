import content from "../generated/content.json";

function success(data) {
  return { flag: true, code: 20000, message: "success", data };
}

function paginate(items, params = {}) {
  const current = Math.max(1, Number(params.current) || 1);
  const size = Math.max(1, Number(params.size) || 10);
  const start = (current - 1) * size;
  return items.slice(start, start + size);
}

function preview(article) {
  return {
    ...article,
    articleContent: article.summary || article.articleContent
  };
}

function staticResponse(config) {
  const requestUrl = new URL(config.url, window.location.origin);
  const pathname = requestUrl.pathname.replace(/\/+$/, "") || "/";
  const params = { ...Object.fromEntries(requestUrl.searchParams), ...config.params };

  if (pathname === "/api") {
    return success(content.site);
  }

  if (pathname === "/api/articles") {
    return success(paginate(content.articles.map(preview), params));
  }

  if (pathname === "/api/articles/newest") {
    return success(content.articles.slice(0, 5).map(preview));
  }

  if (pathname === "/api/articles/archives") {
    return success({
      recordList: paginate(content.articles, params).map(article => ({
        id: article.id,
        articleTitle: article.articleTitle,
        createTime: article.createTime
      })),
      count: content.articles.length
    });
  }

  if (pathname === "/api/articles/search") {
    const keywords = String(params.keywords || "").trim().toLocaleLowerCase();
    if (!keywords) return success([]);
    return success(
      content.articles
        .filter(article =>
          [
            article.articleTitle,
            article.articleContent,
            article.categoryName,
            ...article.tagDTOList.map(tag => tag.tagName)
          ]
            .join(" ")
            .toLocaleLowerCase()
            .includes(keywords)
        )
        .map(preview)
    );
  }

  const articleMatch = pathname.match(/^\/api\/articles\/([^/]+)$/);
  if (articleMatch) {
    const id = decodeURIComponent(articleMatch[1]);
    const article = content.articleDetails.find(item => item.id === id);
    return article
      ? success(article)
      : { flag: false, code: 40400, message: "文章不存在", data: null };
  }

  if (pathname === "/api/categories") {
    return success({
      recordList: content.categories,
      count: content.categories.length
    });
  }

  const categoryMatch = pathname.match(/^\/api\/categories\/(\d+)$/);
  if (categoryMatch) {
    const id = Number(categoryMatch[1]);
    const category = content.categories.find(item => item.id === id);
    const articles = content.articles
      .filter(article => article.categoryId === id)
      .map(preview);
    return success({
      name: category ? category.categoryName : "分类",
      articlePreviewDTOList: paginate(articles, params)
    });
  }

  if (pathname === "/api/tags") {
    return success({ recordList: content.tags, count: content.tags.length });
  }

  const tagMatch = pathname.match(/^\/api\/tags\/(\d+)$/);
  if (tagMatch) {
    const id = Number(tagMatch[1]);
    const tag = content.tags.find(item => item.id === id);
    const articles = content.articles
      .filter(article => article.tagDTOList.some(item => item.id === id))
      .map(preview);
    return success({
      name: tag ? tag.tagName : "标签",
      articlePreviewDTOList: paginate(articles, params)
    });
  }

  if (pathname === "/api/links") {
    return success(content.links);
  }

  if (pathname === "/api/comments" || pathname === "/api/messages") {
    return success({ recordList: [], count: 0 });
  }

  return { flag: false, code: 40400, message: "静态资源不存在", data: null };
}

export function installStaticApi(axios) {
  axios.interceptors.request.use(config => {
    if (!String(config.url || "").startsWith("/api")) return config;

    config.adapter = async requestConfig => ({
      data: staticResponse(requestConfig),
      status: 200,
      statusText: "OK",
      headers: {},
      config: requestConfig,
      request: null
    });
    return config;
  });
}
