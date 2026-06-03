package com.atsforge.platform.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "templates")
public class TemplateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 80)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 30)
    private String category;
    @Column(nullable = false, length = 300)
    private String description;
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    @Column(name = "html_layout", nullable = false, columnDefinition = "TEXT")
    private String htmlLayout;
    @Column(name = "css_styles", nullable = false, columnDefinition = "TEXT")
    private String cssStyles;
    @Column(nullable = false)
    private boolean premium;
    @Column(nullable = false)
    private boolean active;

    protected TemplateEntity() {}
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getHtmlLayout() { return htmlLayout; }
    public String getCssStyles() { return cssStyles; }
    public boolean isPremium() { return premium; }
    public boolean isActive() { return active; }
}
