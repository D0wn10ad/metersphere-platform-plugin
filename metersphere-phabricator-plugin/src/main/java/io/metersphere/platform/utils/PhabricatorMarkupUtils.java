package io.metersphere.platform.utils;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.plugin.utils.LogUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhabricatorMarkupUtils {

    private final PhabricatorClient client;

    public PhabricatorMarkupUtils(PhabricatorClient client) {
        this.client = client;
    }

    /**
     * Convert Phabricator Remarkup to Markdown
     */
    public String remarkupToMarkdown(String remarkup, String context) {
        if (remarkup == null || remarkup.isBlank()) {
            return remarkup;
        }

        try {
            // Step 1: Call remarkup.process API to get HTML
            String html = client.processRemarkup(remarkup, context);

            // Step 2: Convert HTML to Markdown
            return htmlToMarkdown(html);
        } catch (Exception e) {
            LogUtil.error("Failed to convert remarkup to markdown", e);
            return remarkup; // Return original on failure
        }
    }

    /**
     * Convert HTML to Markdown using JSoup
     */
    private String htmlToMarkdown(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }

        Document doc = Jsoup.parse(html);
        StringBuilder markdown = new StringBuilder();
        processNode(doc.body(), markdown);
        return markdown.toString().trim();
    }

    private void processNode(Element element, StringBuilder markdown) {
        for (Element child : element.children()) {
            String tag = child.tagName().toLowerCase();
            
            switch (tag) {
                case "p":
                    if (markdown.length() > 0 && !markdown.toString().endsWith("\n\n")) {
                        markdown.append("\n\n");
                    }
                    markdown.append(child.text());
                    break;
                case "h1":
                    markdown.append("\n\n# ").append(child.text()).append("\n");
                    break;
                case "h2":
                    markdown.append("\n\n## ").append(child.text()).append("\n");
                    break;
                case "h3":
                    markdown.append("\n\n### ").append(child.text()).append("\n");
                    break;
                case "h4":
                    markdown.append("\n\n#### ").append(child.text()).append("\n");
                    break;
                case "strong", "b":
                    markdown.append("**").append(child.text()).append("**");
                    break;
                case "em", "i":
                    markdown.append("*").append(child.text()).append("*");
                    break;
                case "code":
                    markdown.append("`").append(child.text()).append("`");
                    break;
                case "pre":
                    markdown.append("\n\n```\n").append(child.text()).append("\n```\n");
                    break;
                case "a":
                    markdown.append("[").append(child.text()).append("](").append(child.attr("href")).append(")");
                    break;
                case "img":
                    markdown.append("![").append(child.attr("alt")).append("](").append(child.attr("src")).append(")");
                    break;
                case "ul":
                    processList(child, markdown, false);
                    break;
                case "ol":
                    processList(child, markdown, true);
                    break;
                case "li":
                    markdown.append(child.text());
                    break;
                case "br":
                    markdown.append("\n");
                    break;
                case "blockquote":
                    markdown.append("\n> ").append(child.text()).append("\n");
                    break;
                default:
                    processNode(child, markdown);
                    break;
            }
        }
    }

    private void processList(Element list, StringBuilder markdown, boolean ordered) {
        int index = 1;
        for (Element item : list.children()) {
            if ("li".equals(item.tagName().toLowerCase())) {
                String prefix = ordered ? index + ". " : "- ";
                markdown.append("\n").append(prefix).append(item.text());
                index++;
            }
        }
    }

    /**
     * Extract attachment references like {F123} from content
     */
    public List<String> extractAttachmentRefs(String content) {
        List<String> refs = new ArrayList<>();
        if (content == null) return refs;

        Pattern pattern = Pattern.compile("\\{F(\\d+)\\}");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            refs.add(matcher.group(0));
        }
        return refs;
    }

    /**
     * Convert Markdown to Phabricator Remarkup
     */
    public String markdownToRemarkup(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return markdown;
        }
        
        String remarkup = markdown;
        
        // Headers: # Header -> = Header =
        remarkup = remarkup.replaceAll("(?m)^######\\s+(.+)$", "====== $1 ======");
        remarkup = remarkup.replaceAll("(?m)^#####\\s+(.+)$", "===== $1 =====");
        remarkup = remarkup.replaceAll("(?m)^####\\s+(.+)$", "==== $1 ====");
        remarkup = remarkup.replaceAll("(?m)^###\\s+(.+)$", "=== $1 ===");
        remarkup = remarkup.replaceAll("(?m)^##\\s+(.+)$", "== $1 ==");
        remarkup = remarkup.replaceAll("(?m)^#\\s+(.+)$", "= $1 =");
        
        // Bold: **text** or __text__ -> **text**
        remarkup = remarkup.replaceAll("\\*\\*(.+?)\\*\\*", "**$1**");
        remarkup = remarkup.replaceAll("__(.+?)__", "**$1**");
        
        // Italic: *text* or _text_ -> //text//
        remarkup = remarkup.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "//$1//");
        remarkup = remarkup.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "//$1//");
        
        // Code: `code` -> `code` (remarkup uses backticks too)
        // Links: [text](url) -> [[url|text]]
        remarkup = remarkup.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "[[$2|$1]]");
        
        // Images: ![alt](url) -> (no direct equivalent, keep as link)
        remarkup = remarkup.replaceAll("!\\[(.*?)\\]\\((.+?)\\)", "[[$2|$1]]");
        
        // Lists: - item -> * item
        remarkup = remarkup.replaceAll("(?m)^-\\s+(.+)$", "* $1");
        // Ordered lists: 1. item -> # numeral item
        remarkup = remarkup.replaceAll("(?m)^\\d+\\.\\s+(.+)$", "# $1");
        
        // Blockquotes: > text -> > text
        remarkup = remarkup.replaceAll("(?m)^>\\s+(.+)$", "> $1");
        
        // Code blocks: ```language\ncode\n``` -> `code`
        remarkup = remarkup.replaceAll("```\\w*\\n([\\s\\S]*?)```", "`$1`");
        
        return remarkup;
    }
}
