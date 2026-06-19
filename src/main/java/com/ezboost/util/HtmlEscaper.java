package com.ezboost.util;

/** Minimal context-specific encoder for dynamic text rendered in HTML or attributes. */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    public static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        StringBuilder escaped = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            switch (text.charAt(index)) {
                case '&': escaped.append("&amp;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                default: escaped.append(text.charAt(index));
            }
        }
        return escaped.toString();
    }
}
