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
}
