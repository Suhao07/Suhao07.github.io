package com.Auroral.blog.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown工具类
 * 
 * @author Auroral
 */
public class MarkdownUtil {

    /**
     * 清理Markdown语法标记，获取纯文本
     * 
     * @param markdown Markdown格式的文本
     * @return 纯文本
     */
    public static String cleanMarkdown(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }

        String text = markdown;

        // 移除代码块
        text = text.replaceAll("```[\\s\\S]*?```", "");

        // 移除行内代码
        text = text.replaceAll("`[^`]*`", "");

        // 移除图片
        text = text.replaceAll("!\\[.*?\\]\\(.*?\\)", "");

        // 移除链接，保留链接文本
        text = text.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");

        // 移除标题标记
        text = text.replaceAll("^#{1,6}\\s+", "");

        // 移除强调标记（加粗、斜体）
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
        text = text.replaceAll("__(.*?)__", "$1");
        text = text.replaceAll("\\*(.*?)\\*", "$1");
        text = text.replaceAll("_(.*?)_", "$1");

        // 移除引用
        text = text.replaceAll("^>\\s+", "");

        // 移除水平线
        text = text.replaceAll("^-{3,}|^\\*{3,}|^_{3,}", "");

        // 移除HTML标签
        text = text.replaceAll("<[^>]*>", "");

        // 移除列表标记
        text = text.replaceAll("^[\\*\\-+]\\s+", "");
        text = text.replaceAll("^\\d+\\.\\s+", "");

        // 移除表格
        text = text.replaceAll("\\|.*\\|", "");
        text = text.replaceAll("^[-:| ]+$", "");

        // 移除mermaid图表
        Pattern mermaidPattern = Pattern.compile("(?s)```mermaid.*?```");
        text = mermaidPattern.matcher(text).replaceAll("");

        // 压缩空白字符
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
} 