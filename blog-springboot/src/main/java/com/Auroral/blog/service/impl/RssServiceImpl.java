package com.Auroral.blog.service.impl;

import com.Auroral.blog.dto.ArticleDTO;
import com.Auroral.blog.dto.ArticleRecommendDTO;
import com.Auroral.blog.service.ArticleService;
import com.Auroral.blog.service.RssService;
import com.Auroral.blog.utils.HtmlToRssConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RSS服务实现类
 * 
 * @author Auroral
 */
@Service
public class RssServiceImpl implements RssService {

    @Autowired
    private ArticleService articleService;

    @Override
    public ResponseEntity<String> generateRssFeed() throws Exception {
        try {
            // 获取最近的文章列表
            List<ArticleRecommendDTO> recentArticles = articleService.listNewestArticles();
            
            // 获取完整文章数据
            List<ArticleDTO> fullArticles = new ArrayList<>();
            
            for (ArticleRecommendDTO article : recentArticles) {
                // 获取完整文章内容
                ArticleDTO fullArticle = articleService.getArticleById(article.getId());
                
                // 确保创建时间不为空
                if (fullArticle.getCreateTime() == null) {
                    fullArticle.setCreateTime(new Date());
                }
                
                // 确保更新时间不为空
                if (fullArticle.getUpdateTime() == null) {
                    fullArticle.setUpdateTime(fullArticle.getCreateTime());
                }
                
                fullArticles.add(fullArticle);
            }
            
            // 使用HtmlToRssConverter将文章转换为Atom格式 - 注意使用新方法名
            String atomXml = HtmlToRssConverter.convertArticlesToAtom(
                fullArticles,
                "yourblog",
                "https://your-domain.com",
                "Your Name",
                "your-email@example.com"
            );
            
            // 设置HTTP响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/atom+xml; charset=UTF-8");
            
            // 返回带有HTTP头和内容的完整响应
            return new ResponseEntity<>(atomXml, headers, HttpStatus.OK);
        } catch (Exception e) {
            // 记录异常信息
            e.printStackTrace();
            throw e;
        }
    }
} 