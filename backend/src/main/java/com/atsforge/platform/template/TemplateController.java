package com.atsforge.platform.template;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {
    private final TemplateRepository templates;

    public TemplateController(TemplateRepository templates) {
        this.templates = templates;
    }

    @GetMapping
    @Cacheable("templates")
    public List<TemplateResponse> list() {
        return templates.findAllByActiveTrueOrderByPremiumAscNameAsc().stream().map(TemplateResponse::from).toList();
    }

    public record TemplateResponse(String code, String name, String category, String description, String thumbnailUrl, boolean premium) {
        static TemplateResponse from(TemplateEntity template) {
            return new TemplateResponse(template.getCode(), template.getName(), template.getCategory(),
                    template.getDescription(), template.getThumbnailUrl(), template.isPremium());
        }
    }
}

