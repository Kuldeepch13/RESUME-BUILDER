package com.atsforge.platform.export;

import com.atsforge.platform.resume.ResumeEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class ResumeRenderer {
    private final ObjectMapper mapper;
    public ResumeRenderer(ObjectMapper mapper) { this.mapper = mapper; }

    public String html(ResumeEntity resume) {
        JsonNode sections = read(resume.getSectionsJson());
        StringBuilder body = new StringBuilder();
        sections.forEach(section -> {
            if (section.path("visible").asBoolean(true)) {
                body.append("<section><h2>").append(escape(section.path("title").asText())).append("</h2><p>")
                        .append(escape(contentText(section.path("content")))).append("</p></section>");
            }
        });
        String color = escape(read(resume.getThemeJson()).path("primaryColor").asText("#115e59"));
        return """
                <!doctype html><html><head><meta charset="UTF-8"/>
                <style>@page{size:A4;margin:18mm}body{font:10.5pt Arial,sans-serif;color:#172334;line-height:1.45}
                h1{font-size:27pt;margin:0 0 14px;color:%s}h2{font-size:12pt;text-transform:uppercase;letter-spacing:.08em;
                border-bottom:1px solid #d7dee5;padding-bottom:5px;margin-top:18px;color:%s}p{white-space:pre-wrap}</style></head>
                <body><h1>%s</h1>%s</body></html>
                """.formatted(color, color, escape(resume.getTitle()), body);
    }

    public String text(ResumeEntity resume) {
        List<String> output = new ArrayList<>();
        output.add(resume.getTitle().toUpperCase());
        read(resume.getSectionsJson()).forEach(section -> {
            if (section.path("visible").asBoolean(true)) {
                output.add("\n" + section.path("title").asText().toUpperCase());
                output.add(contentText(section.path("content")));
            }
        });
        return String.join("\n", output);
    }

    private String contentText(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            node.forEach(value -> values.add("- " + contentText(value)));
            return String.join("\n", values);
        }
        if (node.isObject()) {
            List<String> values = new ArrayList<>();
            node.fields().forEachRemaining(field -> values.add(field.getKey() + ": " + contentText(field.getValue())));
            return String.join(" | ", values);
        }
        return node.asText("");
    }
    private JsonNode read(String raw) {
        try { return mapper.readTree(raw); } catch (Exception ex) { throw new IllegalStateException("Unable to render resume.", ex); }
    }
    private String escape(String text) { return HtmlUtils.htmlEscape(text); }
}

