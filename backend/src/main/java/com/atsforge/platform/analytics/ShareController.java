package com.atsforge.platform.analytics;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ShareController {
    private final ShareService service;
    public ShareController(ShareService service) { this.service = service; }

    @PostMapping("/resumes/{id}/shares")
    public ShareService.ShareResponse share(@PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiresAt) {
        return service.create(id, expiresAt);
    }
    @DeleteMapping("/shares/{id}")
    public void disable(@PathVariable UUID id) { service.disable(id); }
    @GetMapping("/resumes/{id}/analytics")
    public ShareService.AnalyticsResponse analytics(@PathVariable UUID id) { return service.analytics(id); }
    @GetMapping(value = "/shares/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] qr(@PathVariable UUID id) { return service.qr(id); }

    @GetMapping(value = "/public/resumes/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> publicResume(@PathVariable String token, HttpServletRequest request) {
        return ResponseEntity.ok(service.view(token, request));
    }
    @PostMapping("/public/resumes/{token}/clicks")
    public void click(@PathVariable String token, HttpServletRequest request) { service.recruiterClick(token, request); }
}
