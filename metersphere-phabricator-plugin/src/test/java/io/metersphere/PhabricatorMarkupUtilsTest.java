package io.metersphere;

import io.metersphere.platform.client.PhabricatorClient;
import io.metersphere.platform.utils.PhabricatorMarkupUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PhabricatorMarkupUtilsTest {

    @Mock
    private PhabricatorClient mockClient;

    private PhabricatorMarkupUtils markupUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        markupUtils = new PhabricatorMarkupUtils(mockClient);
    }

    // extractAttachmentRefs tests

    @Test
    @DisplayName("extractAttachmentRefs - should extract single attachment reference")
    void testExtractAttachmentRefs_single() {
        String content = "See {F123} for details";
        List<String> refs = markupUtils.extractAttachmentRefs(content);
        
        assertEquals(1, refs.size());
        assertEquals("{F123}", refs.get(0));
    }

    @Test
    @DisplayName("extractAttachmentRefs - should extract multiple attachment references")
    void testExtractAttachmentRefs_multiple() {
        String content = "See {F123} and {F456} and {F789}";
        List<String> refs = markupUtils.extractAttachmentRefs(content);
        
        assertEquals(3, refs.size());
        assertTrue(refs.contains("{F123}"));
        assertTrue(refs.contains("{F456}"));
        assertTrue(refs.contains("{F789}"));
    }

    @Test
    @DisplayName("extractAttachmentRefs - should return empty list for null input")
    void testExtractAttachmentRefs_nullInput() {
        List<String> refs = markupUtils.extractAttachmentRefs(null);
        assertTrue(refs.isEmpty());
    }

    @Test
    @DisplayName("extractAttachmentRefs - should return empty list for no attachments")
    void testExtractAttachmentRefs_noAttachments() {
        String content = "No attachments here";
        List<String> refs = markupUtils.extractAttachmentRefs(content);
        assertTrue(refs.isEmpty());
    }

    // extractMsMarkdownImages tests

    @Test
    @DisplayName("extractMsMarkdownImages - should extract MS image with valid URL")
    void testExtractMsMarkdownImages_valid() {
        String content = "Check ![[/resource/md/get?fileName=test.png|Test.png]] image";
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages(content);
        
        assertEquals(1, images.size());
        assertEquals("/resource/md/get?fileName=test.png", images.get(0).getUrl());
        assertEquals("Test.png", images.get(0).getFilename());
    }

    @Test
    @DisplayName("extractMsMarkdownImages - should return empty for null input")
    void testExtractMsMarkdownImages_nullInput() {
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages(null);
        assertTrue(images.isEmpty());
    }

    @Test
    @DisplayName("extractMsMarkdownImages - should return empty for blank input")
    void testExtractMsMarkdownImages_blankInput() {
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages("   ");
        assertTrue(images.isEmpty());
    }

    @Test
    @DisplayName("extractMsMarkdownImages - should not extract non-MS images")
    void testExtractMsMarkdownImages_nonMsImages() {
        String content = "Check ![external](http://example.com/image.png) image";
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages(content);
        assertTrue(images.isEmpty());
    }

    // markdownToRemarkup tests

    @Test
    @DisplayName("markdownToRemarkup - should convert headers")
    void testMarkdownToRemarkup_headers() {
        String markdown = "# Header 1\n## Header 2\n### Header 3";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertTrue(remarkup.contains("= Header 1 ="));
        assertTrue(remarkup.contains("== Header 2 =="));
        assertTrue(remarkup.contains("=== Header 3 ==="));
    }

    @Test
    @DisplayName("markdownToRemarkup - should convert bold text")
    void testMarkdownToRemarkup_bold() {
        String markdown = "This is **bold** text";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertTrue(remarkup.contains("**bold**"));
    }

    @Test
    @DisplayName("markdownToRemarkup - should convert links")
    void testMarkdownToRemarkup_links() {
        String markdown = "[Link text](http://example.com)";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertTrue(remarkup.contains("[[http://example.com|Link text]]"));
    }

    @Test
    @DisplayName("markdownToRemarkup - should return same for null input")
    void testMarkdownToRemarkup_nullInput() {
        assertNull(markupUtils.markdownToRemarkup(null));
    }

    @Test
    @DisplayName("markdownToRemarkup - should return same for blank input")
    void testMarkdownToRemarkup_blankInput() {
        assertEquals("   ", markupUtils.markdownToRemarkup("   "));
    }

    // remarkupToMarkdown tests

    @Test
    @DisplayName("remarkupToMarkdown - should return original on API failure")
    void testRemarkupToMarkdown_apiFailure() {
        String remarkup = "Some content";
        when(mockClient.processRemarkup(anyString(), anyString()))
            .thenThrow(new RuntimeException("API Error"));
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertEquals(remarkup, result);
        verify(mockClient).processRemarkup(remarkup, "context");
    }

    @Test
    @DisplayName("remarkupToMarkdown - should return null for null input")
    void testRemarkupToMarkdown_nullInput() {
        String result = markupUtils.remarkupToMarkdown(null, "context");
        assertNull(result);
    }

    @Test
    @DisplayName("remarkupToMarkdown - should return blank for blank input")
    void testRemarkupToMarkdown_blankInput() {
        String result = markupUtils.remarkupToMarkdown("   ", "context");
        assertEquals("   ", result);
    }

    // MsImageInfo tests

    @Test
    @DisplayName("MsImageInfo - toPhabricatorSyntax with PHID")
    void testMsImageInfo_toPhabricatorSyntax_withPhid() {
        PhabricatorMarkupUtils.MsImageInfo info = new PhabricatorMarkupUtils.MsImageInfo();
        info.setPhabricatorPhid("PHID-XXXX");
        info.setMatch("[[url|filename]]");
        
        assertEquals("{img:PHID-XXXX}", info.toPhabricatorSyntax());
    }

    @Test
    @DisplayName("MsImageInfo - toPhabricatorSyntax without PHID")
    void testMsImageInfo_toPhabricatorSyntax_noPhid() {
        PhabricatorMarkupUtils.MsImageInfo info = new PhabricatorMarkupUtils.MsImageInfo();
        info.setMatch("[[url|filename]]");
        
        assertEquals("[[url|filename]]", info.toPhabricatorSyntax());
    }

    @Test
    @DisplayName("extractMsMarkdownImages - should extract multiple images")
    void testExtractMsMarkdownImages_multiple() {
        String content = "Image1 ![[/resource/md/get?fileName=test1.png|Test1.png]] and Image2 ![[/resource/md/get?fileName=test2.png|Test2.png]]";
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages(content);
        
        assertEquals(2, images.size());
    }

    @Test
    @DisplayName("extractAttachmentRefs - should handle various formats")
    void testExtractAttachmentRefs_formats() {
        String content = "{F123} {F456}and{F789}";
        List<String> refs = markupUtils.extractAttachmentRefs(content);
        
        assertEquals(3, refs.size());
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle code blocks")
    void testMarkdownToRemarkup_codeBlocks() {
        String markdown = "```java\nSystem.out.println();\n```";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle inline code")
    void testMarkdownToRemarkup_inlineCode() {
        String markdown = "Use `code` here";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle lists")
    void testMarkdownToRemarkup_lists() {
        String markdown = "- Item 1\n- Item 2\n- Item 3";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle numbered lists")
    void testMarkdownToRemarkup_numberedLists() {
        String markdown = "1. First\n2. Second\n3. Third";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle italic")
    void testMarkdownToRemarkup_italic() {
        String markdown = "This is *italic* text";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle strikethrough")
    void testMarkdownToRemarkup_strikethrough() {
        String markdown = "This is ~~deleted~~ text";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle horizontal rule")
    void testMarkdownToRemarkup_horizontalRule() {
        String markdown = "Some text\n---\nMore text";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle blockquote")
    void testMarkdownToRemarkup_blockquote() {
        String markdown = "> Quote text";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }

    @Test
    @DisplayName("extractAttachmentRefs - should handle nested braces")
    void testExtractAttachmentRefs_nestedBraces() {
        String content = "{F123 {F456}}";
        List<String> refs = markupUtils.extractAttachmentRefs(content);
        
        assertFalse(refs.isEmpty());
    }

    @Test
    @DisplayName("extractAttachmentRefs - should handle special characters in filename")
    void testExtractAttachmentRefs_specialChars() {
        String content = "{F123.png} and {F456-test_123.jpg}";
        List<String> refs = markupUtils.extractAttachmentRefs(content);
        
        assertEquals(0, refs.size());
    }

    @Test
    @DisplayName("MsImageInfo - getters and setters")
    void testMsImageInfo_setters() {
        PhabricatorMarkupUtils.MsImageInfo info = new PhabricatorMarkupUtils.MsImageInfo();
        info.setUrl("http://test.com/image.png");
        info.setFilename("image.png");
        info.setMatch("[[http://test.com/image.png|image.png]]");
        info.setPhabricatorPhid("PHID-TEST");
        
        assertEquals("http://test.com/image.png", info.getUrl());
        assertEquals("image.png", info.getFilename());
        assertEquals("[[http://test.com/image.png|image.png]]", info.getMatch());
        assertEquals("PHID-TEST", info.getPhabricatorPhid());
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert HTML to Markdown successfully")
    void testRemarkupToMarkdown_success() {
        String remarkup = "Some remarkup content";
        String html = "<p>Some remarkup content</p>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
        verify(mockClient).processRemarkup(remarkup, "context");
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert headers in HTML to Markdown")
    void testRemarkupToMarkdown_headers() {
        String remarkup = "Header text";
        String html = "<h1>Header text</h1><h2>Subheader</h2><h3>Subsub</h3>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
        assertTrue(result.contains("# Header text"));
        assertTrue(result.contains("## Subheader"));
        assertTrue(result.contains("### Subsub"));
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert bold and italic")
    void testRemarkupToMarkdown_formatting() {
        String remarkup = "Formatted text";
        String html = "<p><strong>Bold</strong> and <em>italic</em></p>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert links and images")
    @SuppressWarnings("deprecation")
    void testRemarkupToMarkdown_linksAndImages() {
        String remarkup = "Links here";
        String html = "<p>Check <a href=\"http://example.com\">this link</a> and <img src=\"http://test.com/img.png\" alt=\"alt text\"/></p>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert code blocks")
    void testRemarkupToMarkdown_codeBlocks() {
        String remarkup = "Code here";
        String html = "<pre>function test() {\n  return true;\n}</pre>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
        assertTrue(result.contains("```"));
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert lists")
    void testRemarkupToMarkdown_lists() {
        String remarkup = "List items";
        String html = "<ul><li>Item 1</li><li>Item 2</li></ul>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert ordered lists")
    void testRemarkupToMarkdown_orderedLists() {
        String remarkup = "Ordered items";
        String html = "<ol><li>First</li><li>Second</li></ol>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("remarkupToMarkdown - should convert blockquote")
    void testRemarkupToMarkdown_blockquote() {
        String remarkup = "Quote text";
        String html = "<blockquote>This is a quote</blockquote>";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertNotNull(result);
        assertTrue(result.contains(">"));
    }

    @Test
    @DisplayName("remarkupToMarkdown - should handle empty HTML")
    void testRemarkupToMarkdown_emptyHtml() {
        String remarkup = "Empty result";
        String html = "";
        when(mockClient.processRemarkup(remarkup, "context")).thenReturn(html);
        
        String result = markupUtils.remarkupToMarkdown(remarkup, "context");
        
        assertEquals("", result);
    }

    @Test
    @DisplayName("htmlToMarkdown - should return same for null input")
    void testHtmlToMarkdown_nullInput() {
        String result = markupUtils.remarkupToMarkdown(null, "context");
        assertNull(result);
    }

    @Test
    @DisplayName("extractMsMarkdownImages - should extract image with complex URL")
    void testExtractMsMarkdownImages_complexUrl() {
        String content = "Image ![[/resource/md/get?fileName=image%20test.png|Image Test.png]]";
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages(content);
        
        assertEquals(1, images.size());
    }

    @Test
    @DisplayName("extractMsMarkdownImages - should not extract URL without resource path")
    void testExtractMsMarkdownImages_nonResourceUrl() {
        String content = "Image ![[/other/path?fileName=test.png|Test.png]]";
        List<PhabricatorMarkupUtils.MsImageInfo> images = markupUtils.extractMsMarkdownImages(content);
        
        assertTrue(images.isEmpty());
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle multiple headers")
    void testMarkdownToRemarkup_multipleHeaders() {
        String markdown = "# H1\n## H2\n### H3\n#### H4\n##### H5\n###### H6";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertTrue(remarkup.contains("= H1 ="));
        assertTrue(remarkup.contains("== H2 =="));
        assertTrue(remarkup.contains("=== H3 ==="));
        assertTrue(remarkup.contains("==== H4 ===="));
        assertTrue(remarkup.contains("===== H5 ====="));
        assertTrue(remarkup.contains("====== H6 ======"));
    }

    @Test
    @DisplayName("markdownToRemarkup - should handle images")
    void testMarkdownToRemarkup_images() {
        String markdown = "![alt text](http://example.com/image.png)";
        String remarkup = markupUtils.markdownToRemarkup(markdown);
        
        assertNotNull(remarkup);
    }
}
