package com.Auroral.blog.service;

import org.springframework.http.ResponseEntity;

/**
 * RSS服务接口
 * 
 * @author Auroral
 */
public interface RssService {
    
    /**
     * 生成RSS Feed
     * 
     * @return 包含RSS Feed的HTTP响应
     * @throws Exception 生成异常
     */
    ResponseEntity<String> generateRssFeed() throws Exception;
}
