package com.Auroral.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.Auroral.blog.dao.*;
import com.Auroral.blog.dto.*;
import com.Auroral.blog.entity.Article;
import com.Auroral.blog.entity.ArticleTag;
import com.Auroral.blog.entity.Category;
import com.Auroral.blog.entity.Tag;
import com.Auroral.blog.service.ArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.Auroral.blog.service.ArticleTagService;
import com.Auroral.blog.utils.BeanCopyUtil;
import com.Auroral.blog.utils.UserUtil;
import com.Auroral.blog.vo.ArticleVO;
import com.Auroral.blog.vo.ConditionVO;
import com.Auroral.blog.vo.DeleteVO;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.Auroral.blog.constant.CommonConst.FALSE;
import static com.Auroral.blog.constant.RedisPrefixConst.*;
import com.Auroral.blog.utils.MarkdownUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Auroral
 * @since 2020-05-18
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleDao, Article> implements ArticleService {
    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private TagDao tagDao;
    @Autowired
    private ArticleTagDao articleTagDao;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    private HttpSession session;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private ArticleTagService articleTagService;

    @Override
    public PageDTO<ArchiveDTO> listArchives(Long current) {
        Page<Article> page = new Page<>(current, 10);
        // 获取分页数据
        Page<Article> articlePage = articleDao.selectPage(page, new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getCreateTime)
                .orderByDesc(Article::getCreateTime)
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getIsDraft, FALSE));
        // 拷贝dto集合
        List<ArchiveDTO> archiveDTOList = BeanCopyUtil.copyList(articlePage.getRecords(), ArchiveDTO.class);
        return new PageDTO<>(archiveDTOList, (int) articlePage.getTotal());
    }

    @Override
    public PageDTO<ArticleBackDTO> listArticleBackDTO(ConditionVO condition) {
        // 转换页码
        condition.setCurrent((condition.getCurrent() - 1) * condition.getSize());
        // 查询文章总量
        Integer count = articleDao.countArticleBacks(condition);
        if (count == 0) {
            return new PageDTO<>();
        }
        // 查询后台文章
        List<ArticleBackDTO> articleBackDTOList = articleDao.listArticleBacks(condition);
        // 查询文章点赞量和浏览量
        Map<String, Integer> viewsCountMap = redisTemplate.boundHashOps(ARTICLE_VIEWS_COUNT).entries();
        Map<String, Integer> likeCountMap = redisTemplate.boundHashOps(ARTICLE_LIKE_COUNT).entries();
        // 封装点赞量和浏览量
        articleBackDTOList.forEach(item -> {
            item.setViewsCount(Objects.requireNonNull(viewsCountMap).get(item.getId().toString()));
            item.setLikeCount(Objects.requireNonNull(likeCountMap).get(item.getId().toString()));
        });
        return new PageDTO<>(articleBackDTOList, count);
    }

    @Override
    public List<ArticleHomeDTO> listArticles(Long current) {
        // 转换页码分页查询文章
        List<ArticleHomeDTO> articleDTOList = articleDao.listArticles((current - 1) * 10);
        return articleDTOList;
    }
    @Override
    public ArticlePreviewListDTO listArticlesByCondition(ConditionVO condition) {
        // 确保current不为null，并且至少为1
        int current = condition.getCurrent() == null ? 1 : condition.getCurrent();

        // 每页显示的文章数量
        int pageSize = 9;

        // 计算偏移量 - 直接设置到condition对象
        condition.setCurrent((current - 1) * pageSize);

        // 设置每页大小
        condition.setSize(pageSize);

        // 搜索条件对应数据
        List<ArticlePreviewDTO> articlePreviewDTOList = articleDao.listArticlesByCondition(condition);

        // 搜索条件对应名(标签或分类名)
        String name;
        if (Objects.nonNull(condition.getCategoryId())) {
            name = categoryDao.selectOne(new LambdaQueryWrapper<Category>()
                            .select(Category::getCategoryName)
                            .eq(Category::getId, condition.getCategoryId()))
                    .getCategoryName();
        } else {
            name = tagDao.selectOne(new LambdaQueryWrapper<Tag>()
                            .select(Tag::getTagName)
                            .eq(Tag::getId, condition.getTagId()))
                    .getTagName();
        }
        return ArticlePreviewListDTO.builder()
                .articlePreviewDTOList(articlePreviewDTOList)
                .name(name)
                .build();
    }

    @Override
    public ArticleDTO getArticleById(Integer articleId) {
        // 更新文章浏览量
        updateArticleViewsCount(articleId);
        // 查询id对应的文章
        ArticleDTO article = articleDao.getArticleById(articleId);
        // 查询上一篇下一篇文章
        Article lastArticle = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover)
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getIsDraft, FALSE)
                .lt(Article::getId, articleId)
                .orderByDesc(Article::getId)
                .last("limit 1"));
        Article nextArticle = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover)
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getIsDraft, FALSE)
                .gt(Article::getId, articleId)
                .orderByAsc(Article::getId)
                .last("limit 1"));
        article.setLastArticle(BeanCopyUtil.copyObject(lastArticle, ArticlePaginationDTO.class));
        article.setNextArticle(BeanCopyUtil.copyObject(nextArticle, ArticlePaginationDTO.class));
        // 查询相关推荐文章
        article.setArticleRecommendList(articleDao.listArticleRecommends(articleId));
        // 封装点赞量和浏览量
        article.setViewsCount((Integer) redisTemplate.boundHashOps(ARTICLE_VIEWS_COUNT).get(articleId.toString()));
        article.setLikeCount((Integer) redisTemplate.boundHashOps(ARTICLE_LIKE_COUNT).get(articleId.toString()));
        return article;
    }

    @Override
    public List<ArticleRecommendDTO> listNewestArticles() {
        // 查询最新文章
        List<Article> articleList = articleDao.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleCover, Article::getCreateTime)
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getIsDraft, FALSE)
                .orderByDesc(Article::getId)
                .last("limit 5"));
        return BeanCopyUtil.copyList(articleList, ArticleRecommendDTO.class);
    }

    /**
     * 更新文章浏览量
     *
     * @param articleId 文章id
     */
    @Async
    public void updateArticleViewsCount(Integer articleId) {
        // 判断是否第一次访问，增加浏览量
        Set<Integer> set = (Set<Integer>) session.getAttribute("articleSet");
        if (Objects.isNull(set)) {
            set = new HashSet<>();
        }
        if (!set.contains(articleId)) {
            set.add(articleId);
            session.setAttribute("articleSet", set);
            // 浏览量+1
            redisTemplate.boundHashOps(ARTICLE_VIEWS_COUNT).increment(articleId.toString(), 1);
        }
    }

    @Override
    public ArticleOptionDTO listArticleOptionDTO() {
        // 查询文章分类选项
        List<Category> categoryList = categoryDao.selectList(new LambdaQueryWrapper<Category>()
                .select(Category::getId, Category::getCategoryName));
        List<CategoryBackDTO> categoryDTOList = BeanCopyUtil.copyList(categoryList, CategoryBackDTO.class);
        // 查询文章标签选项
        List<Tag> tagList = tagDao.selectList(new LambdaQueryWrapper<Tag>()
                .select(Tag::getId, Tag::getTagName));
        List<TagDTO> tagDTOList = BeanCopyUtil.copyList(tagList, TagDTO.class);
        return ArticleOptionDTO.builder()
                .categoryDTOList(categoryDTOList)
                .tagDTOList(tagDTOList)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveArticleLike(Integer articleId) {
        // 查询当前用户点赞过的文章id集合
        Set<Integer> articleLikeSet = (Set<Integer>) redisTemplate.boundHashOps(ARTICLE_USER_LIKE).get(UserUtil.getLoginUser().getUserInfoId().toString());
        // 第一次点赞则创建
        if (CollectionUtils.isEmpty(articleLikeSet)) {
            articleLikeSet = new HashSet<>();
        }
        // 判断是否点赞
        if (articleLikeSet.contains(articleId)) {
            // 点过赞则删除文章id
            articleLikeSet.remove(articleId);
            // 文章点赞量-1
            redisTemplate.boundHashOps(ARTICLE_LIKE_COUNT).increment(articleId.toString(), -1);
        } else {
            // 未点赞则增加文章id
            articleLikeSet.add(articleId);
            // 文章点赞量+1
            redisTemplate.boundHashOps(ARTICLE_LIKE_COUNT).increment(articleId.toString(), 1);
        }
        // 保存点赞记录
        redisTemplate.boundHashOps(ARTICLE_USER_LIKE).put(UserUtil.getLoginUser().getUserInfoId().toString(), articleLikeSet);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOrUpdateArticle(ArticleVO articleVO) {
        // 处理摘要 - 如果没有提供摘要，则从内容中生成
        String summary = articleVO.getSummary();
        if (summary == null || summary.isEmpty()) {
            // 从内容中生成摘要
            String content = articleVO.getArticleContent();
            if (content != null && !content.isEmpty()) {
                // 清理Markdown语法，获取纯文本
                String plainText = MarkdownUtil.cleanMarkdown(content);
                
                // 截取前200个字符
                if (plainText.length() > 200) {
                    summary = plainText.substring(0, 200) + "...";
                } else {
                    summary = plainText;
                }
            } else {
                summary = ""; // 确保摘要不为null
            }
        }
        
        // 保存或修改文章
        Article article = Article.builder()
                .id(articleVO.getId())
                .userId(UserUtil.getLoginUser().getUserInfoId())
                .categoryId(articleVO.getCategoryId())
                .articleCover(articleVO.getArticleCover())
                .articleTitle(articleVO.getArticleTitle())
                .articleContent(articleVO.getArticleContent())
                .summary(summary) // 设置清理后的摘要
                .createTime(Objects.isNull(articleVO.getId()) ? new Date() : null)
                .updateTime(Objects.nonNull(articleVO.getId()) ? new Date() : null)
                .isTop(articleVO.getIsTop())
                .isDraft(articleVO.getIsDraft())
                .build();
        
        articleService.saveOrUpdate(article);
        
        // 编辑文章则删除文章所有标签
        if (Objects.nonNull(articleVO.getId()) && articleVO.getIsDraft().equals(FALSE)) {
            articleTagDao.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleVO.getId()));
        }
        
        // 添加文章标签
        if (!articleVO.getTagIdList().isEmpty()) {
            List<ArticleTag> articleTagList = articleVO.getTagIdList().stream().map(tagId -> ArticleTag.builder()
                    .articleId(article.getId())
                    .tagId(tagId)
                    .build())
                    .collect(Collectors.toList());
            articleTagService.saveBatch(articleTagList);
        }
        
        // 如果不是草稿则同步到 ES
        if (articleVO.getIsDraft().equals(FALSE)) {
            try {
                // 构建 ES 文档对象 - 使用更简单的方式
                ArticleSearchDTO articleSearchDTO = new ArticleSearchDTO();
                articleSearchDTO.setId(article.getId());
                articleSearchDTO.setArticleTitle(article.getArticleTitle() != null ? article.getArticleTitle() : "");
                
                // 只存储前10000个字符，避免过大
                String content = article.getArticleContent();
                if (content != null && content.length() > 10000) {
                    content = content.substring(0, 10000) + "...";
                }
                articleSearchDTO.setArticleContent(content != null ? content : "");
                
                // 存储摘要
                articleSearchDTO.setSummary(article.getSummary() != null ? article.getSummary() : "");
                articleSearchDTO.setIsDelete(FALSE);

                // 使用REST客户端发送请求 - 修正版本
                elasticsearchRestTemplate.execute(client -> {
                    try {
                        // 准备请求 - 使用正确的REST客户端API
                        org.elasticsearch.client.RestClient lowLevelClient = client.getLowLevelClient();
                        Request request = new Request("PUT", "/article/_doc/" + article.getId());
                        
                        // 将对象转换为JSON
                        Map<String, Object> document = new HashMap<>();
                        document.put("id", articleSearchDTO.getId());
                        document.put("articleTitle", articleSearchDTO.getArticleTitle());
                        document.put("articleContent", articleSearchDTO.getArticleContent());
                        document.put("summary", articleSearchDTO.getSummary());
                        document.put("isDelete", articleSearchDTO.getIsDelete());
                        
                        // 将Map转换为JSON
                        ObjectMapper mapper = new ObjectMapper();
                        request.setJsonEntity(mapper.writeValueAsString(document));
                        
                        // 发送请求并检查状态码
                        Response response = lowLevelClient.performRequest(request);
                        int statusCode = response.getStatusLine().getStatusCode();
                        
                        return statusCode >= 200 && statusCode < 300;
                    } catch (IOException e) {
                        log.error("ES同步失败", e);
                        return false;
                    }
                });
            } catch (Exception e) {
                // 记录错误但不影响主流程
                log.error("同步文章到ES失败: " + e.getMessage(), e);
            }
        }
    }
    @Transactional(rollbackFor = Exception.class)
    public void syncArticlesToEs() {
        try {
            // 查询所有非删除、非草稿的文章
            List<Article> articleList = articleDao.selectList(new LambdaQueryWrapper<Article>()
                    .eq(Article::getIsDelete, FALSE)
                    .eq(Article::getIsDraft, FALSE));

            // 转换为 ES DTO 并保存
            if (!articleList.isEmpty()) {
                List<ArticleSearchDTO> articleSearchDTOList = articleList.stream()
                        .map(article -> {
                            ArticleSearchDTO dto = new ArticleSearchDTO();
                            dto.setId(article.getId());
                            dto.setArticleTitle(article.getArticleTitle() != null ? article.getArticleTitle() : "");
                            dto.setArticleContent(article.getArticleContent() != null ? article.getArticleContent() : "");
                            dto.setIsDelete(FALSE);
                            return dto;
                        }).collect(Collectors.toList());

                // 清除现有索引中的所有数据
                elasticsearchRestTemplate.deleteIndex(ArticleSearchDTO.class);
                elasticsearchRestTemplate.createIndex(ArticleSearchDTO.class);
                elasticsearchRestTemplate.putMapping(ArticleSearchDTO.class);
                
                // 保存新数据
                elasticsearchRestTemplate.save(articleSearchDTOList);
            }
        } catch (Exception e) {
            log.error("同步文章到ES失败: " + e.getMessage(), e);
            throw new RuntimeException("同步文章到ES失败", e);
        }
    }

    @Override
    public List<Article> getRecentArticles(int limit) {
        return articleDao.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getIsDraft, FALSE)
                .orderByDesc(Article::getCreateTime)
                .last("limit " + limit));
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateArticleTop(Integer articleId, Integer isTop) {
        // 修改文章置顶状态
        Article article = Article.builder()
                .id(articleId)
                .isTop(isTop)
                .build();
        articleDao.updateById(article);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateArticleDelete(DeleteVO deleteVO) {
        // 修改文章逻辑删除状态
        List<Article> articleList = deleteVO.getIdList().stream().map(id -> Article.builder()
                .id(id)
                .isTop(FALSE)
                .isDelete(deleteVO.getIsDelete())
                .build())
                .collect(Collectors.toList());
        articleService.updateBatchById(articleList);
        // 同步ES中的文章状态
        try {
            for (Integer articleId : deleteVO.getIdList()) {
                // 如果是删除操作
                if (deleteVO.getIsDelete() == 1) {
                    // 方法1：直接从ES中删除
                    elasticsearchRestTemplate.delete(articleId.toString(), ArticleSearchDTO.class);

                    // 或方法2：更新ES中的isDelete状态（取消注释使用此方法）
                /*
                ArticleSearchDTO articleSearchDTO = new ArticleSearchDTO();
                articleSearchDTO.setId(articleId);
                articleSearchDTO.setIsDelete(1);
                elasticsearchRestTemplate.save(articleSearchDTO);
                */
                } else {
                    // 恢复文章，需要重新索引
                    Article article = articleDao.selectById(articleId);
                    if (article != null && article.getIsDraft().equals(FALSE)) {
                        ArticleSearchDTO articleSearchDTO = new ArticleSearchDTO();
                        articleSearchDTO.setId(article.getId());
                        articleSearchDTO.setArticleTitle(article.getArticleTitle());
                        articleSearchDTO.setArticleContent(article.getArticleContent());
                        articleSearchDTO.setIsDelete(FALSE);
                        elasticsearchRestTemplate.save(articleSearchDTO);
                    }
                }
            }
        } catch (Exception e) {
            // 记录错误但不影响主流程
            log.error("同步文章删除状态到ES失败: " + e.getMessage(), e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteArticles(List<Integer> articleIdList) {
        // 删除文章标签关联
        articleTagDao.delete(new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIdList));
        // 删除文章
        articleDao.deleteBatchIds(articleIdList);
        // 从ES中删除文章
        try {
            for (Integer articleId : articleIdList) {
                elasticsearchRestTemplate.delete(articleId.toString(), ArticleSearchDTO.class);
            }
        } catch (Exception e) {
            // 记录错误但不影响主流程
            log.error("从ES删除文章失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ArticleSearchDTO> listArticlesBySearch(ConditionVO condition) {
        return searchArticle(buildQuery(condition));
    }

    @Override
    public ArticleVO getArticleBackById(Integer articleId) {
        // 查询文章信息
        Article article = articleDao.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getArticleTitle, Article::getArticleContent, Article::getArticleCover, Article::getCategoryId, Article::getIsTop, Article::getIsDraft)
                .eq(Article::getId, articleId));
        // 查询文章标签
        List<Integer> tagIdList = articleTagDao.selectList(new LambdaQueryWrapper<ArticleTag>()
                .select(ArticleTag::getTagId)
                .eq(ArticleTag::getArticleId, article.getId()))
                .stream()
                .map(ArticleTag::getTagId).collect(Collectors.toList());
        return ArticleVO.builder()
                .id(article.getId())
                .articleTitle(article.getArticleTitle())
                .articleContent(article.getArticleContent())
                .articleCover(article.getArticleCover())
                .categoryId(article.getCategoryId())
                .isTop(article.getIsTop())
                .tagIdList(tagIdList)
                .isDraft(article.getIsDraft())
                .build();
    }

    /**
     * 搜索文章构造
     *
     * @param condition 条件
     * @return es条件构造器
     */
    private NativeSearchQueryBuilder buildQuery(ConditionVO condition) {
        // 条件构造器
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 始终过滤已删除的文章
        boolQueryBuilder.must(QueryBuilders.termQuery("isDelete", FALSE));

        // 根据关键词搜索文章标题或内容
        if (Objects.nonNull(condition.getKeywords())) {
            boolQueryBuilder.must(
                    QueryBuilders.boolQuery()
                            .should(QueryBuilders.matchQuery("articleTitle", condition.getKeywords()))
                            .should(QueryBuilders.matchQuery("articleContent", condition.getKeywords()))
            );
        }

        // 查询
        nativeSearchQueryBuilder.withQuery(boolQueryBuilder);
        return nativeSearchQueryBuilder;
    }

    /**
     * 文章搜索结果高亮
     *
     * @param nativeSearchQueryBuilder es条件构造器
     * @return 搜索结果
     */
    private List<ArticleSearchDTO> searchArticle(NativeSearchQueryBuilder nativeSearchQueryBuilder) {
        // 添加文章标题高亮
        HighlightBuilder.Field titleField = new HighlightBuilder.Field("articleTitle");
        titleField.preTags("<span style='color:#f47466'>");
        titleField.postTags("</span>");
        // 添加文章内容高亮
        HighlightBuilder.Field contentField = new HighlightBuilder.Field("articleContent");
        contentField.preTags("<span style='color:#f47466'>");
        contentField.postTags("</span>");
        contentField.fragmentSize(200);
        nativeSearchQueryBuilder.withHighlightFields(titleField, contentField);
        // 搜索
        SearchHits<ArticleSearchDTO> search = elasticsearchRestTemplate.search(nativeSearchQueryBuilder.build(), ArticleSearchDTO.class);
        return search.getSearchHits().stream().map(hit -> {
            ArticleSearchDTO article = hit.getContent();
            // 获取文章标题高亮数据
            List<String> titleHighLightList = hit.getHighlightFields().get("articleTitle");
            if (CollectionUtils.isNotEmpty(titleHighLightList)) {
                // 替换标题数据
                article.setArticleTitle(titleHighLightList.get(0));
            }
            // 获取文章内容高亮数据
            List<String> contentHighLightList = hit.getHighlightFields().get("articleContent");
            if (CollectionUtils.isNotEmpty(contentHighLightList)) {
                // 替换内容数据
                article.setArticleContent(contentHighLightList.get(0));
            }
            return article;
        }).collect(Collectors.toList());
    }

}
