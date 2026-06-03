package com.atsforge.platform.ai;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.atsforge.platform.ai.AiDtos.AnalysisResponse;
import com.atsforge.platform.ai.AiDtos.AnalyzeRequest;
import com.atsforge.platform.ai.AiDtos.BulletPointRequest;
import com.atsforge.platform.ai.AiDtos.BulletPointResponse;
import com.atsforge.platform.ai.AiDtos.GenerateRequest;
import com.atsforge.platform.ai.AiDtos.GenerationResponse;
import com.atsforge.platform.ai.AiDtos.ImprovementRequest;
import com.atsforge.platform.ai.AiDtos.ImprovementResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AiController {
    private final AiService service;
    public AiController(AiService service) { this.service = service; }

    @PostMapping("/resumes/{resumeId}/analyses") @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisResponse analyze(@PathVariable UUID resumeId, @Valid @RequestBody AnalyzeRequest request) {
        return service.request(resumeId, request);
    }
    @GetMapping("/analyses/{id}")
    public AnalysisResponse status(@PathVariable UUID id) { return service.get(id); }
    @PostMapping("/ai/generate")
    public GenerationResponse generate(@Valid @RequestBody GenerateRequest request) { return service.generate(request); }
    @PostMapping("/resumes/{resumeId}/ai/bullets")
    public BulletPointResponse generateBullets(@PathVariable UUID resumeId, @Valid @RequestBody BulletPointRequest request) {
        return service.generateBulletPoints(resumeId, request);
    }
    @PostMapping("/resumes/{resumeId}/ai/improvements")
    public ImprovementResponse getImprovements(@PathVariable UUID resumeId, @Valid @RequestBody ImprovementRequest request) {
        return service.getImprovementSuggestions(resumeId, request);
    }
}
