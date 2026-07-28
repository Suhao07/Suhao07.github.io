package com.Auroral.blog.utils;

import com.Auroral.blog.dto.ArticleDTO;
import com.Auroral.blog.entity.Article;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;
import org.w3c.dom.CDATASection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.Auroral.blog.utils.MarkdownToHtmlConverter.convertMarkdownToHtml;
import com.Auroral.blog.utils.MarkdownUtil;

/**
 * HTML到Atom RSS转换器
 * 将多个文章转换为Atom格式的RSS订阅源
 */
public class HtmlToRssConverter {

    // 用于生成ISO 8601格式的日期
    private static final SimpleDateFormat ISO_8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static {
        ISO_8601_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    /**
     * 将多个文章转换为Atom格式
     */
    public static String convertArticlesToAtom(List<ArticleDTO> articles, String blogTitle, String blogUrl,
                                              String authorName, String authorEmail) throws Exception {
        String updateTime = ISO_8601_FORMAT.format(new Date());

        // 创建XML文档
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // 创建根元素 <feed>
        Element feedElement = doc.createElement("feed");
        feedElement.setAttribute("xmlns", "http://www.w3.org/2005/Atom");
        doc.appendChild(feedElement);

        // 添加博客标题 <title>
        Element titleElement = doc.createElement("title");
        titleElement.setTextContent(blogTitle);
        feedElement.appendChild(titleElement);

        // 添加博客链接 <link>
        Element selfLinkElement = doc.createElement("link");
        selfLinkElement.setAttribute("href", blogUrl + "/atom.xml");
        selfLinkElement.setAttribute("rel", "self");
        feedElement.appendChild(selfLinkElement);

        Element blogLinkElement = doc.createElement("link");
        blogLinkElement.setAttribute("href", blogUrl);
        feedElement.appendChild(blogLinkElement);

        // 添加更新时间 <updated>
        Element updatedElement = doc.createElement("updated");
        updatedElement.setTextContent(updateTime);
        feedElement.appendChild(updatedElement);

        // 添加博客ID <id>
        Element idElement = doc.createElement("id");
        idElement.setTextContent(blogUrl);
        feedElement.appendChild(idElement);

        // 添加作者信息 <author>
        Element authorElement = doc.createElement("author");
        feedElement.appendChild(authorElement);

        Element nameElement = doc.createElement("name");
        nameElement.setTextContent(authorName);
        authorElement.appendChild(nameElement);

        Element emailElement = doc.createElement("email");
        emailElement.setTextContent(authorEmail);
        authorElement.appendChild(emailElement);

        // 添加生成器信息 <generator>
        Element generatorElement = doc.createElement("generator");
        generatorElement.setTextContent("HTML to Atom Converter");
        generatorElement.setAttribute("uri", "https://github.com/your-repo");
        feedElement.appendChild(generatorElement);

        // 为每个文章创建一个entry
        for (ArticleDTO article : articles) {
            // 获取文章摘要，已经存储在数据库中的干净摘要
            String summary = article.getSummary();
            
            // 如果数据库中没有摘要，临时生成一个（虽然这种情况不应该出现）
            if (summary == null || summary.isEmpty()) {
                String content = article.getArticleContent();
                if (content != null && !content.isEmpty()) {
                    String plainText = MarkdownUtil.cleanMarkdown(content);
                    summary = plainText.length() > 200 ? 
                        plainText.substring(0, 200) + "..." : plainText;
                } else {
                    summary = "";
                }
            }
            
            // 添加条目 <entry>
            Element entryElement = doc.createElement("entry");
            feedElement.appendChild(entryElement);

            // 添加条目标题
            Element entryTitleElement = doc.createElement("title");
            entryTitleElement.setTextContent(article.getArticleTitle());
            entryElement.appendChild(entryTitleElement);

            // 添加条目链接
            Element entryLinkElement = doc.createElement("link");
            entryLinkElement.setAttribute("href", blogUrl + "/articles/" + article.getId());
            entryElement.appendChild(entryLinkElement);

            // 添加条目ID
            Element entryIdElement = doc.createElement("id");
            entryIdElement.setTextContent(blogUrl + "/article.html?id=" + article.getId());
            entryElement.appendChild(entryIdElement);

            // 添加发布时间和更新时间
            Element publishedElement = doc.createElement("published");
            Date createDate = article.getCreateTime() != null ? article.getCreateTime() : new Date();
            publishedElement.setTextContent(ISO_8601_FORMAT.format(createDate));
            entryElement.appendChild(publishedElement);

            Element entryUpdatedElement = doc.createElement("updated");
            Date updateDate = article.getUpdateTime() != null ? article.getUpdateTime() : createDate;
            entryUpdatedElement.setTextContent(ISO_8601_FORMAT.format(updateDate));
            entryElement.appendChild(entryUpdatedElement);

            // 添加内容 - 直接使用摘要
            Element contentElement = doc.createElement("content");
            contentElement.setAttribute("type", "text");
            contentElement.setTextContent(summary);
            entryElement.appendChild(contentElement);

            // 添加分类
            Element categoryElement = doc.createElement("category");
            categoryElement.setAttribute("term", "blog");
            categoryElement.setAttribute("scheme", blogUrl + "/categories/blog/");
            entryElement.appendChild(categoryElement);
        }

        // 将XML转换为字符串并返回
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        
        // 添加这一行来去除XML声明
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);

        transformer.transform(source, result);
        return stringWriter.toString();
    }
}
