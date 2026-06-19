package com.ezboost.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlEscaperTest {

    @Test
    void escapesTextAndAttributeDelimiters() {
        assertEquals("&lt;script&gt;&amp;&quot;&#39;", HtmlEscaper.escape("<script>&\"'"));
        assertEquals("", HtmlEscaper.escape(null));
    }
}
