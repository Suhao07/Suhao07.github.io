package com.Auroral.blog.utils;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.options.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownToHtmlConverter {

    // 定义一些常用emoji的映射
    private static final Map<String, String> EMOJI_MAP = new HashMap<>();
    static {
        // 常用emoji
        EMOJI_MAP.put(":+1:", "👍");
        EMOJI_MAP.put(":-1:", "👎");
        EMOJI_MAP.put(":smile:", "😄");
        EMOJI_MAP.put(":laughing:", "😆");
        EMOJI_MAP.put(":blush:", "😊");
        EMOJI_MAP.put(":smiley:", "😃");
        EMOJI_MAP.put(":relaxed:", "☺️");
        EMOJI_MAP.put(":family_man_man_boy_boy:", "👨‍👨‍👦‍👦");
        EMOJI_MAP.put(":wales:", "🏴󠁧󠁢󠁷󠁬󠁳󠁿");
        EMOJI_MAP.put(":scotland:", "🏴󠁧󠁢󠁳󠁣󠁴󠁿");
        EMOJI_MAP.put(":england:", "🏴󠁧󠁢󠁥󠁮󠁧󠁿");
    }

    // 存储数学公式和对应的占位符
    private static final Map<String, String> MATH_PLACEHOLDERS = new HashMap<>();

//    public static void main(String[] args) {
//        // 直接使用指定文件路径
//        String markdownFile = "/Users/auroral/Desktop/213/example-markdown.md";
//        String outputFile = "/Users/auroral/Desktop/213/example-markdown.html";
//
//        try {
//            convertMarkdownToHtml(markdownFile, outputFile);
//            System.out.println("转换成功！生成文件: " + outputFile);
//        } catch (IOException e) {
//            System.err.println("转换失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    public static String convertMarkdownToHtml(String markdownContent) throws IOException {
        // 提取YAML前言（frontmatter）
        Map<String, String> frontMatter = new LinkedHashMap<>();
        markdownContent = extractFrontMatter(markdownContent, frontMatter);

        // 预处理：替换LaTeX数学公式为占位符，避免被flexmark处理
        markdownContent = preprocessMathFormulas(markdownContent);

        // 配置解析选项
        MutableDataSet options = new MutableDataSet();
        // 添加扩展支持表格、删除线和注脚
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                FootnoteExtension.create()
        ));

        // 创建解析器
        Parser parser = Parser.builder(options).build();
        Document document = parser.parse(markdownContent);

        // 创建HTML渲染器
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();

        // 查找所有mermaid代码块
        List<FencedCodeBlock> mermaidBlocks = findMermaidCodeBlocks(document);

        // 生成HTML
        String htmlContent = generateHtmlFile( document, renderer, mermaidBlocks, frontMatter);
        return htmlContent;
    }

    /**
     * 提取YAML前言（frontmatter）
     */
    private static String extractFrontMatter(String content, Map<String, String> frontMatter) {
        if (content.startsWith("---")) {
            int endIndex = content.indexOf("---", 3);
            if (endIndex != -1) {
                String yamlContent = content.substring(3, endIndex).trim();

                // 解析每一行
                for (String line : yamlContent.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("#")) continue; // 跳过注释行

                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        frontMatter.put(key, value);
                    }
                }

                // 返回剩余的内容（不包括frontmatter）
                return content.substring(endIndex + 3).trim();
            }
        }
        return content;
    }

    /**
     * 预处理LaTeX数学公式为占位符
     */
    private static String preprocessMathFormulas(String content) {
        MATH_PLACEHOLDERS.clear();

        // 处理行内公式 $...$
        Pattern inlinePattern = Pattern.compile("\\$([^\\$\\n]+?)\\$");
        Matcher inlineMatcher = inlinePattern.matcher(content);
        StringBuffer sb = new StringBuffer();

        while (inlineMatcher.find()) {
            String formula = inlineMatcher.group(0);
            String placeholder = "INLINE_MATH_" + UUID.randomUUID().toString();
            MATH_PLACEHOLDERS.put(placeholder, formula);
            inlineMatcher.appendReplacement(sb, placeholder);
        }
        inlineMatcher.appendTail(sb);
        content = sb.toString();

        // 处理块级公式 $$...$$
        Pattern blockPattern = Pattern.compile("\\$\\$(.*?)\\$\\$", Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(content);
        sb = new StringBuffer();

        while (blockMatcher.find()) {
            String formula = blockMatcher.group(0);
            String placeholder = "BLOCK_MATH_" + UUID.randomUUID().toString();
            MATH_PLACEHOLDERS.put(placeholder, formula);
            blockMatcher.appendReplacement(sb, placeholder);
        }
        blockMatcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 后处理：将占位符替换回真实的LaTeX数学公式
     */
    private static String postprocessMathFormulas(String content) {
        for (Map.Entry<String, String> entry : MATH_PLACEHOLDERS.entrySet()) {
            String placeholder = entry.getKey();
            String formula = entry.getValue();

            // 对于行内公式，使用data-display=false
            if (placeholder.startsWith("INLINE_MATH_")) {
                content = content.replace(placeholder, "<span class=\"katex-formula\" data-display=\"false\">" + formula + "</span>");
            }
            // 对于块级公式，使用data-display=true
            else if (placeholder.startsWith("BLOCK_MATH_")) {
                content = content.replace(placeholder, "<div class=\"katex-formula\" data-display=\"true\">" + formula + "</div>");
            }
        }
        return content;
    }

    private static List<FencedCodeBlock> findMermaidCodeBlocks(Document document) {
        final List<FencedCodeBlock> mermaidBlocks = new ArrayList<>();

        NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(FencedCodeBlock.class, codeBlock -> {
                    BasedSequence info = codeBlock.getInfo();
                    if (info != null && info.toString().trim().equalsIgnoreCase("mermaid")) {
                        mermaidBlocks.add(codeBlock);
                    }
                })
        );

        visitor.visit(document);
        return mermaidBlocks;
    }

    private static String generateHtmlFile(Document document, HtmlRenderer renderer,
                                         List<FencedCodeBlock> mermaidBlocks, Map<String, String> frontMatter) {
        // 将Markdown转换为HTML
        String htmlContent = renderer.render(document);

        // 处理mermaid代码块，确保它们正确渲染
        for (FencedCodeBlock mermaidBlock : mermaidBlocks) {
            String mermaidHtml = "<div class=\"mermaid\">\n" + mermaidBlock.getContentChars().normalizeEOL() + "\n</div>";

            // 在HTML内容中查找并替换对应的pre代码块
            Pattern pattern = Pattern.compile("<pre><code class=\"language-mermaid\">(.*?)</code></pre>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(htmlContent);

            if (matcher.find()) {
                String codeContent = matcher.group(1);
                htmlContent = htmlContent.replace(matcher.group(0), mermaidHtml);
            }
        }

        // 处理Emoji表情符号
        for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
            htmlContent = htmlContent.replace(entry.getKey(), entry.getValue());
        }

        // 后处理：将数学公式占位符替换回原始LaTeX
        htmlContent = postprocessMathFormulas(htmlContent);

        // 构建完整的HTML文档字符串
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<!DOCTYPE html>\n");
        htmlBuilder.append("<html>\n");
        htmlBuilder.append("<head>\n");
        htmlBuilder.append("    <meta charset=\"UTF-8\">\n");
        htmlBuilder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");

        // 使用frontmatter的title作为页面标题
        String title = frontMatter.getOrDefault("title", "Markdown文档");
        htmlBuilder.append("    <title>").append(title).append("</title>\n");

        // 前言（frontmatter）元数据
        if (!frontMatter.isEmpty()) {
            for (Map.Entry<String, String> entry : frontMatter.entrySet()) {
                htmlBuilder.append("    <meta name=\"").append(entry.getKey()).append("\" content=\"").append(entry.getValue()).append("\">\n");
            }
        }

        htmlBuilder.append("    <!-- Mermaid -->\n");
        htmlBuilder.append("    <script src=\"https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js\"></script>\n");
        htmlBuilder.append("    <!-- KaTeX -->\n");
        htmlBuilder.append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/katex.min.css\">\n");
        htmlBuilder.append("    <script src=\"https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/katex.min.js\"></script>\n");
        htmlBuilder.append("    <script src=\"https://cdn.jsdelivr.net/npm/katex@0.16.0/dist/contrib/auto-render.min.js\"></script>\n");
        htmlBuilder.append("    <!-- PrismJS 代码高亮 -->\n");
        htmlBuilder.append("    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/prismjs@1.25.0/themes/prism.css\">\n");
        htmlBuilder.append("    <script src=\"https://cdn.jsdelivr.net/npm/prismjs@1.25.0/prism.min.js\"></script>\n");
        htmlBuilder.append("    <script>\n");
        htmlBuilder.append("        document.addEventListener('DOMContentLoaded', function () {\n");
        htmlBuilder.append("            // 初始化Mermaid\n");
        htmlBuilder.append("            mermaid.initialize({ startOnLoad: true });\n");
        htmlBuilder.append("            \n");
        htmlBuilder.append("            // 处理数学公式\n");
        htmlBuilder.append("            document.querySelectorAll('.katex-formula').forEach(function(element) {\n");
        htmlBuilder.append("                const isDisplay = element.getAttribute('data-display') === 'true';\n");
        htmlBuilder.append("                const formula = element.textContent;\n");
        htmlBuilder.append("                try {\n");
        htmlBuilder.append("                    katex.render(\n");
        htmlBuilder.append("                        isDisplay ? formula.slice(2, -2) : formula.slice(1, -1),\n");
        htmlBuilder.append("                        element,\n");
        htmlBuilder.append("                        {\n");
        htmlBuilder.append("                            displayMode: isDisplay,\n");
        htmlBuilder.append("                            throwOnError: false\n");
        htmlBuilder.append("                        }\n");
        htmlBuilder.append("                    );\n");
        htmlBuilder.append("                } catch (e) {\n");
        htmlBuilder.append("                    console.error('KaTeX渲染错误:', e);\n");
        htmlBuilder.append("                    element.textContent = formula;\n");
        htmlBuilder.append("                }\n");
        htmlBuilder.append("            });\n");
        htmlBuilder.append("        });\n");
        htmlBuilder.append("    </script>\n");
        htmlBuilder.append("    <style>\n");
        htmlBuilder.append("        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; padding: 20px; max-width: 900px; margin: 0 auto; }\n");
        htmlBuilder.append("        pre { background-color: #f5f5f5; padding: 10px; border-radius: 5px; overflow-x: auto; }\n");
        htmlBuilder.append("        code { background-color: #f5f5f5; padding: 2px 4px; border-radius: 3px; }\n");
        htmlBuilder.append("        table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n");
        htmlBuilder.append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        htmlBuilder.append("        th { background-color: #f2f2f2; }\n");
        htmlBuilder.append("        tr:nth-child(even) { background-color: #f9f9f9; }\n");
        htmlBuilder.append("        .mermaid { margin: 20px 0; text-align: center; }\n");
        htmlBuilder.append("        img { max-width: 100%; height: auto; }\n");
        htmlBuilder.append("        .katex-formula[data-display='true'] { margin: 1em 0; text-align: center; font-size: 1.2em; }\n");
        htmlBuilder.append("        .footnotes { margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; font-size: 0.9em; }\n");
        htmlBuilder.append("        .footnotes ol { padding-left: 20px; }\n");
        htmlBuilder.append("        .footnotes li { margin-bottom: 10px; }\n");
        htmlBuilder.append("        .footnote-backref { text-decoration: none; }\n");
        htmlBuilder.append("        .frontmatter { background-color: #f9f9f9; padding: 15px; margin-bottom: 20px; border-radius: 5px; border-left: 5px solid #ccc; }\n");
        htmlBuilder.append("    </style>\n");
        htmlBuilder.append("</head>\n");
        htmlBuilder.append("<body>\n");
        htmlBuilder.append("    <div class=\"container\">\n");

        // 显示前言（frontmatter）
        if (!frontMatter.isEmpty()) {
            htmlBuilder.append("        <div class=\"frontmatter\">\n");
            for (Map.Entry<String, String> entry : frontMatter.entrySet()) {
                htmlBuilder.append("            <div><strong>").append(entry.getKey()).append(":</strong> ").append(entry.getValue()).append("</div>\n");
            }
            htmlBuilder.append("        </div>\n");
        }

        // 插入转换后的HTML内容
        htmlBuilder.append(htmlContent);

        htmlBuilder.append("    </div>\n");
        htmlBuilder.append("</body>\n");
        htmlBuilder.append("</html>\n");
        
        return htmlBuilder.toString();
    }

    /**
     * 将Markdown内容转换为HTML
     *
     * @param markdownContent Markdown格式的内容
     * @return HTML格式的内容
     */
    public static String convertMarkdownContentToHtml(String markdownContent) {
        if (markdownContent == null || markdownContent.isEmpty()) {
            return "";
        }

        try {
            // 提取YAML前言（frontmatter）
            Map<String, String> frontMatter = new LinkedHashMap<>();
            markdownContent = extractFrontMatter(markdownContent, frontMatter);

            // 预处理：替换LaTeX数学公式为占位符
            markdownContent = preprocessMathFormulas(markdownContent);

            // 配置解析选项
            MutableDataSet options = new MutableDataSet();
            options.set(Parser.EXTENSIONS, Arrays.asList(
                    TablesExtension.create(),
                    StrikethroughExtension.create(),
                    FootnoteExtension.create()
            ));

            // 创建解析器
            Parser parser = Parser.builder(options).build();
            Document document = parser.parse(markdownContent);

            // 创建HTML渲染器
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            // 将Markdown转换为HTML
            String htmlContent = renderer.render(document);

            // 处理Emoji表情符号
            for (Map.Entry<String, String> entry : EMOJI_MAP.entrySet()) {
                htmlContent = htmlContent.replace(entry.getKey(), entry.getValue());
            }

            // 后处理：将数学公式占位符替换回原始LaTeX
            htmlContent = postprocessMathFormulas(htmlContent);

            return htmlContent;
        } catch (Exception e) {
            // 转换失败时返回原始内容
            return markdownContent;
        }
    }
}
