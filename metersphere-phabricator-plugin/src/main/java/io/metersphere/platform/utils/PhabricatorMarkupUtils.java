package io.metersphere.platform.utils;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.dto.PhabricatorAttachmentRef;
import io.metersphere.platform.dto.PhabricatorMarkupConversion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for converting between Phabricator Remarkup and Markdown
 */
public class PhabricatorMarkupUtils {

    private PhabricatorClient client;

    public PhabricatorMarkupUtils(PhabricatorClient client) {
        this.client = client;
    }

    /**
     * Convert Phabricator Remarkup to Markdown
     * Uses remarkup.process API to get HTML, then converts to Markdown
     */
    public PhabricatorMarkupConversion convertRemarkupToMarkdown(String remarkup, String context) {
        PhabricatorMarkupConversion conversion = new PhabricatorMarkupConversion();
        conversion.setOriginalRemarkup(remarkup);

        try {
            // Step 1: Use remarkup.process API to get HTML
            String html = client.processRemarkup(remarkup, context);

            // Step 2: Extract attachment references before conversion
            List<PhabricatorAttachmentRef> attachments = extractAttachmentRefs(html);
            conversion.setExtractedAttachments(attachments);

            // Step 3: Convert HTML to Markdown
            String markdown = htmlToMarkdown(html);

            // Step 4: Replace attachment refs with proxy links
            markdown = replaceAttachmentsWithLinks(markdown, attachments);

            conversion.setConvertedMarkdown(markdown);
            conversion.setConversionSuccessful(true);

        } catch (Exception e) {
            conversion.setConversionSuccessful(false);
            conversion.setErrorMessage(e.getMessage());
            // Return original content if conversion fails
            conversion.setConvertedMarkdown(remarkup);
        }

        return conversion;
    }

    /**
     * Convert Markdown to Remarkup (basic conversion)
     */
    public String markdownToRemarkup(String markdown) {
        if (markdown == null) {
            return "";
        }

        String remarkup = markdown;

        // Headers: # Header -> = Header =
        remarkup = remarkup.replaceAll("(?m)^######\\s*(.+)$", "====== $1 ======");
        remarkup = remarkup.replaceAll("(?m)^#####\\s*(.+)$", "===== $1 =====");
        remarkup = remarkup.replaceAll("(?m)^####\\s*(.+)$", "==== $1 ====");
        remarkup = remarkup.replaceAll("(?m)^###\\s*(.+)$", "=== $1 ===");
        remarkup = remarkup.replaceAll("(?m)^##\\s*(.+)$", "== $1 ==");
        remarkup = remarkup.replaceAll("(?m)^#\\s*(.+)$", "= $1 =");

        // Bold: **text** -> **text**
        // Remarkup uses same syntax for bold

        // Italic: *text* -> //text//
        remarkup = remarkup.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "//$1//");

        // Code: `code` -> `code` (same in remarkup)

        // Code blocks: ``` -> ``` (same in remarkup)

        // Links: [text](url) -> [[url|text]]
        remarkup = remarkup.replaceAll("\\[(.+?)\\]\\((.+?)\\)", "[[$2|$1]]");

        // Lists: - item -> * item (basic conversion)
        remarkup = remarkup.replaceAll("(?m)^\\s*-\\s+(.+)$", "* $1");

        return remarkup;
    }

    /**
     * Convert HTML to Markdown
     */
    private String htmlToMarkdown(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        Document doc = Jsoup.parse(html);
        StringBuilder markdown = new StringBuilder();

        // Process body content
        Element body = doc.body();
        for (Element element : body.children()) {
            processElement(element, markdown, 0);
        }

        return markdown.toString().trim();
    }

    private void processElement(Element element, StringBuilder markdown, int depth) {
        String tagName = element.tagName().toLowerCase();

        switch (tagName) {
            case "p":
                if (markdown.length() > 0) {
                    markdown.append("\n\n");
                }
                markdown.append(element.text());
                break;

            case "h1":
                markdown.append("\n\n# ").append(element.text()).append("\n");
                break;

            case "h2":
                markdown.append("\n\n## ").append(element.text()).append("\n");
                break;

            case "h3":
                markdown.append("\n\n### ").append(element.text()).append("\n");
                break;

            case "h4":
                markdown.append("\n\n#### ").append(element.text()).append("\n");
                break;

            case "h5":
                markdown.append("\n\n##### ").append(element.text()).append("\n");
                break;

            case "h6":
                markdown.append("\n\n###### ").append(element.text()).append("\n");
                break;

            case "strong":
            case "b":
                markdown.append("**").append(element.text()).append("**");
                break;

            case "em":
            case "i":
                markdown.append("*").append(element.text()).append("*");
                break;

            case "code":
                markdown.append("`").append(element.text()).append("`");
                break;

            case "pre":
                markdown.append("\n\n```\n").append(element.text()).append("\n```\n");
                break;

            case "a":
                String href = element.attr("href");
                String text = element.text();
                markdown.append("[").append(text).append("](").append(href).append(")");
                break;

            case "img":
                String src = element.attr("src");
                String alt = element.attr("alt");
                markdown.append("![").append(alt).append("](").append(src).append(")");
                break;

            case "ul":
                for (Element li : element.select("li")) {
                    markdown.append("\n* ").append(li.text());
                }
                markdown.append("\n");
                break;

            case "ol":
                int counter = 1;
                for (Element li : element.select("li")) {
                    markdown.append("\n").append(counter++).append(". ").append(li.text());
                }
                markdown.append("\n");
                break;

            case "br":
                markdown.append("\n");
                break;

            case "div":
            case "span":
                // Process children
                for (Element child : element.children()) {
                    processElement(child, markdown, depth);
                }
                break;

            default:
                // For unknown tags, just append text content
                markdown.append(element.text());
        }
    }

    /**
     * Extract attachment/file references from content
     */
    private List<PhabricatorAttachmentRef> extractAttachmentRefs(String content) {
        List<PhabricatorAttachmentRef> attachments = new ArrayList<>();

        // Pattern for {F123} style file references
        Pattern filePattern = Pattern.compile("\\{F(\\d+)\\}");
        Matcher matcher = filePattern.matcher(content);

        while (matcher.find()) {
            PhabricatorAttachmentRef ref = new PhabricatorAttachmentRef();
            ref.setFilePHID("PHID-FILE-" + matcher.group(1));
            ref.setOriginalRef(matcher.group(0));
            ref.setFileName("attachment-" + matcher.group(1));
            attachments.add(ref);
        }

        // Pattern for <img> tags with file references
        Pattern imgPattern = Pattern.compile("<img[^>]+src=\"([^\"]+)\"[^>]*>");
        Matcher imgMatcher = imgPattern.matcher(content);

        while (imgMatcher.find()) {
            String src = imgMatcher.group(1);
            if (src.contains("/file/data/")) {
                PhabricatorAttachmentRef ref = new PhabricatorAttachmentRef();
                ref.setOriginalRef(imgMatcher.group(0));
                // Extract PHID from URL
                String[] parts = src.split("/");
                if (parts.length > 0) {
                    ref.setFilePHID(parts[parts.length - 1]);
                }
                ref.setFileName("image-" + System.currentTimeMillis());
                attachments.add(ref);
            }
        }

        return attachments;
    }

    /**
     * Replace attachment references with proxy links
     */
    private String replaceAttachmentsWithLinks(String content, List<PhabricatorAttachmentRef> attachments) {
        String result = content;

        for (PhabricatorAttachmentRef attachment : attachments) {
            String proxyLink = attachment.generateProxyLink(client.toString());
            result = result.replace(attachment.getOriginalRef(), proxyLink);
        }

        return result;
    }

    /**
     * Extract PHID references from remarkup
     * e.g., {T123}, {D456}, etc.
     */
    public List<String> extractPhidRefs(String content) {
        List<String> refs = new ArrayList<>();

        // Pattern for {T123}, {D456}, etc.
        Pattern pattern = Pattern.compile("\\{([A-Z])(\\d+)\\}");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            refs.add(matcher.group(0));
        }

        return refs;
    }
}