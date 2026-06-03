package com.atsforge.platform.analytics;

import com.atsforge.platform.auth.TokenHashingService;
import com.atsforge.platform.common.NotFoundException;
import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.export.ResumeRenderer;
import com.atsforge.platform.resume.ResumeEntity;
import com.atsforge.platform.resume.ResumeService;
import com.atsforge.platform.security.SecurityUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShareService {
    private final ResumeShareRepository shares;
    private final ResumeEventRepository events;
    private final ResumeService resumes;
    private final ResumeRenderer renderer;
    private final TokenHashingService tokens;
    private final AppProperties properties;

    public ShareService(ResumeShareRepository shares, ResumeEventRepository events, ResumeService resumes,
                        ResumeRenderer renderer, TokenHashingService tokens, AppProperties properties) {
        this.shares = shares; this.events = events; this.resumes = resumes; this.renderer = renderer;
        this.tokens = tokens; this.properties = properties;
    }

    @Transactional
    public ShareResponse create(UUID resumeId, Instant expiresAt) {
        ResumeEntity resume = resumes.ownedEntity(resumeId);
        resume.publish();
        ResumeShareEntity share = shares.save(new ResumeShareEntity(resume, tokens.newToken(), expiresAt));
        return response(share);
    }

    @Transactional
    public void disable(UUID shareId) {
        ResumeShareEntity share = shares.findByIdAndResumeOwnerId(shareId, SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Share link not found."));
        share.disable();
    }

    @Transactional
    public String view(String publicToken, HttpServletRequest request) {
        ResumeShareEntity share = publicShare(publicToken);
        events.save(new ResumeEventEntity(share, "VIEW", visitor(request), request.getHeader("Referer"), request.getHeader("User-Agent")));
        return renderer.html(share.getResume());
    }

    @Transactional
    public void recruiterClick(String publicToken, HttpServletRequest request) {
        ResumeShareEntity share = publicShare(publicToken);
        events.save(new ResumeEventEntity(share, "RECRUITER_CLICK", visitor(request), request.getHeader("Referer"), request.getHeader("User-Agent")));
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(UUID resumeId) {
        resumes.ownedEntity(resumeId);
        return new AnalyticsResponse(events.countByResumeIdAndEventType(resumeId, "VIEW"),
                events.countByResumeIdAndEventType(resumeId, "RECRUITER_CLICK"));
    }

    @Transactional(readOnly = true)
    public byte[] qr(UUID shareId) {
        ResumeShareEntity share = shares.findByIdAndResumeOwnerId(shareId, SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Share link not found."));
        try {
            BitMatrix matrix = new QRCodeWriter().encode(response(share).url(), BarcodeFormat.QR_CODE, 320, 320);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate QR code.", ex);
        }
    }

    private ResumeShareEntity publicShare(String token) {
        ResumeShareEntity share = shares.findByPublicToken(token).orElseThrow(() -> new NotFoundException("Shared resume not found."));
        if (!share.usable()) throw new NotFoundException("Shared resume not found.");
        return share;
    }
    private String visitor(HttpServletRequest request) { return tokens.hash(request.getRemoteAddr()); }
    private ShareResponse response(ResumeShareEntity entity) {
        return new ShareResponse(entity.getId(), properties.publicUrl() + "/r/" + entity.getPublicToken(), entity.getExpiresAt());
    }
    public record ShareResponse(UUID id, String url, Instant expiresAt) {}
    public record AnalyticsResponse(long views, long recruiterClicks) {}
}

