package com.atsforge.platform.export;

import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resumes")
public class ExportController {
    private final ExportService service;
    public ExportController(ExportService service) { this.service = service; }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable UUID id, @RequestParam(defaultValue = "pdf") String format) {
        ExportService.ExportedFile file = service.export(id, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .body(file.bytes());
    }
}
