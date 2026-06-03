package com.atsforge.platform.resume;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.atsforge.platform.resume.ResumeDtos.CreateResumeRequest;
import com.atsforge.platform.resume.ResumeDtos.ResumeResponse;
import com.atsforge.platform.resume.ResumeDtos.UpdateResumeRequest;
import com.atsforge.platform.resume.ResumeDtos.VersionResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {
    private final ResumeService service;
    public ResumeController(ResumeService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse create(@Valid @RequestBody CreateResumeRequest request) { return service.create(request); }
    @GetMapping
    public Page<ResumeResponse> list(@RequestParam(required = false) String search, 
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) UUID template,
                                      Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return service.search(search, pageable);
        }
        if (status != null && !status.isBlank()) {
            return service.filterByStatus(status, pageable);
        }
        if (template != null) {
            return service.filterByTemplate(template, pageable);
        }
        return service.list(pageable);
    }
    @GetMapping("/{id}")
    public ResumeResponse get(@PathVariable UUID id) { return service.get(id); }
    @PutMapping("/{id}/autosave")
    public ResumeResponse autosave(@PathVariable UUID id, @Valid @RequestBody UpdateResumeRequest request) {
        return service.autosave(id, request);
    }
    @PostMapping("/{id}/versions")
    public ResumeResponse snapshot(@PathVariable UUID id, @Valid @RequestBody UpdateResumeRequest request) {
        return service.snapshot(id, request);
    }
    @GetMapping("/{id}/versions")
    public List<VersionResponse> history(@PathVariable UUID id) { return service.versions(id); }
    @PostMapping("/{id}/duplicate") @ResponseStatus(HttpStatus.CREATED)
    public ResumeResponse duplicate(@PathVariable UUID id) { return service.duplicate(id); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
