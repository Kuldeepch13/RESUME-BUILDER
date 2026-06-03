package com.atsforge.platform.export;

import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atsforge.platform.storage.ObjectStorageService;

@Service
public class ExportJobProcessor {
    private final ExportJobRepository jobs;
    private final ResumeRenderer renderer;
    private final ObjectStorageService storage;

    public ExportJobProcessor(ExportJobRepository jobs, ResumeRenderer renderer, ObjectStorageService storage) {
        this.jobs = jobs;
        this.renderer = renderer;
        this.storage = storage;
    }

    @Async
    @Transactional
    public void process(UUID jobId, byte[] bytes, String userId, String resumeId, String format) {
        ExportJobEntity job = jobs.findById(jobId).orElseThrow();
        try {
            String key = "exports/" + userId + "/" + resumeId + "/" + jobId + "." + format;
            ExportService.Format exportFormat = ExportService.Format.valueOf(format.toUpperCase());
            storage.put(key, exportFormat.contentType, bytes);
            job.complete(key);
        } catch (Exception ex) {
            job.fail(ex.getMessage() == null ? "Export failed." : ex.getMessage());
        }
    }
}
