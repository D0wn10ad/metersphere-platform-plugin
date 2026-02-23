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
}
