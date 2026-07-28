package com.Auroral.blog.controller;

import com.Auroral.blog.service.RssService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RssController {

    @Autowired
    private RssService rssService;

    @GetMapping(value = "/rss")
    public ResponseEntity<String> generateRssFeed() throws Exception {
        // 调用服务层生成RSS feed并直接返回结果
        return rssService.generateRssFeed();
    }
}