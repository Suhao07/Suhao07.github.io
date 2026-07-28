package com.Auroral.blog.service.impl;

import com.Auroral.blog.service.NoticeService;
import com.Auroral.blog.vo.NoticeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.Auroral.blog.constant.CommonConst;
import com.Auroral.blog.dao.*;
import com.Auroral.blog.dto.*;
import com.Auroral.blog.entity.Article;
import com.Auroral.blog.entity.UserInfo;
import com.Auroral.blog.service.BlogInfoService;
import com.Auroral.blog.service.UniqueViewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.Auroral.blog.constant.CommonConst.FALSE;
import static com.Auroral.blog.constant.RedisPrefixConst.*;

/**
 * 博客信息服务实现
 * @author Auroral
 * @since 2020-05-18
 */
@Service
public class BlogInfoServiceImpl implements BlogInfoService {
    @Autowired
    private UserInfoDao userInfoDao;
    @Autowired
    private ArticleDao articleDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private TagDao tagDao;
    @Autowired
    private MessageDao messageDao;
    @Autowired
    private UniqueViewService uniqueViewService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private NoticeService noticeService;

    @Override
    public BlogHomeInfoDTO getBlogInfo() {
        // 查询博主信息
        UserInfo userInfo = userInfoDao.selectOne(new LambdaQueryWrapper<UserInfo>()
                .select(UserInfo::getAvatar, UserInfo::getNickname, UserInfo::getIntro)
                .eq(UserInfo::getId, CommonConst.BLOGGER_ID));
        // 查询文章数量
        Integer articleCount = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDraft, FALSE)
                .eq(Article::getIsDelete, FALSE));
        // 查询分类数量
        Integer categoryCount = categoryDao.selectCount(null);
        // 查询标签数量
        Integer tagCount = tagDao.selectCount(null);
        // 查询访问量
        Object viewsValue = redisTemplate.boundValueOps(BLOG_VIEWS_COUNT).get();
        String viewsCount = Objects.nonNull(viewsValue) ? viewsValue.toString() : "0";
        // 查询公告（兼容旧代码）
        Object value = redisTemplate.boundValueOps(NOTICE).get();
        String notice = Objects.nonNull(value) ? value.toString() : "记录技术、研究与实践";

        // 获取最新的5条公告
        List<NoticeDTO> notices = noticeService.listLatestNotices(5);

        // 封装数据
        return BlogHomeInfoDTO.builder()
                .nickname(userInfo.getNickname())
                .avatar(userInfo.getAvatar())
                .intro(userInfo.getIntro())
                .articleCount(articleCount)
                .categoryCount(categoryCount)
                .tagCount(tagCount)
                .notice(notice)  // 保留旧字段，兼容旧代码
                .notices(notices)  // 新增字段，支持多条公告
                .viewsCount(viewsCount)
                .build();
    }

    @Override
    public BlogBackInfoDTO getBlogBackInfo() {
        // 查询访问量
        Object backViewsValue = redisTemplate.boundValueOps(BLOG_VIEWS_COUNT).get();
        Integer viewsCount = backViewsValue instanceof Number
                ? ((Number) backViewsValue).intValue()
                : 0;
        // 查询留言量
        Integer messageCount = messageDao.selectCount(null);
        // 查询用户量
        Integer userCount = userInfoDao.selectCount(null);
        // 查询文章量
        Integer articleCount = articleDao.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getIsDelete, FALSE)
                .eq(Article::getIsDraft, FALSE));
        // 查询一周用户量
        List<UniqueViewDTO> uniqueViewList = uniqueViewService.listUniqueViews();
        // 查询分类数据
        List<CategoryDTO> categoryDTOList = categoryDao.listCategoryDTO();
        // 查询redis访问量前五的文章
        Map<String, Integer> articleViewsMap = redisTemplate.boundHashOps(ARTICLE_VIEWS_COUNT).entries();
        // 将文章进行倒序排序
        List<Integer> articleIdList = Objects.requireNonNull(articleViewsMap).entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .map(item -> Integer.valueOf(item.getKey()))
                .collect(Collectors.toList());
        // 提取前五篇文章
        int index = Math.min(articleIdList.size(), 5);
        articleIdList = articleIdList.subList(0, index);
        // 文章为空直接返回
        if (articleIdList.isEmpty()) {
            return BlogBackInfoDTO.builder()
                    .viewsCount(viewsCount)
                    .messageCount(messageCount)
                    .userCount(userCount)
                    .articleCount(articleCount)
                    .categoryDTOList(categoryDTOList)
                    .uniqueViewDTOList(uniqueViewList)
                    .build();
        }
        // 查询文章标题
        List<Article> articleList = articleDao.listArticleRank(articleIdList);
        // 封装浏览量
        List<ArticleRankDTO> articleRankDTOList = articleList.stream().map(article -> ArticleRankDTO.builder()
                        .articleTitle(article.getArticleTitle())
                        .viewsCount(articleViewsMap.get(article.getId().toString()))
                        .build())
                .collect(Collectors.toList());
        return BlogBackInfoDTO.builder()
                .viewsCount(viewsCount)
                .messageCount(messageCount)
                .userCount(userCount)
                .articleCount(articleCount)
                .categoryDTOList(categoryDTOList)
                .uniqueViewDTOList(uniqueViewList)
                .articleRankDTOList(articleRankDTOList)
                .build();
    }

    @Override
    public String getAbout() {
        Object value = redisTemplate.boundValueOps(ABOUT).get();
        return Objects.nonNull(value) ? value.toString() : "";
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAbout(String aboutContent) {
        redisTemplate.boundValueOps(ABOUT).set(aboutContent);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateNotice(String notice) {
        redisTemplate.boundValueOps(NOTICE).set(notice);

//         如果启用了新的公告系统，可以添加以下代码
         NoticeVO noticeVO = new NoticeVO();
         noticeVO.setContent(notice);
         noticeService.saveOrUpdateNotice(noticeVO);
    }

    @Override
    public String getNotice() {
        Object value = redisTemplate.boundValueOps(NOTICE).get();
        return Objects.nonNull(value) ? value.toString() : "发布你的第一篇公告吧";
    }
}
