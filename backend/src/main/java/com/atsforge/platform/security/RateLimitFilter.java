package com.atsforge.platform.security;

import com.atsforge.platform.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final AppProperties properties;

    public RateLimitFilter(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String client = request.getRemoteAddr();
        String bucket = "rate:api:" + client + ":" + (System.currentTimeMillis() / 60000);
        try {
            Long count = redis.opsForValue().increment(bucket);
            if (count != null && count == 1) {
                redis.expire(bucket, Duration.ofMinutes(2));
            }
            if (count != null && count > properties.limits().apiPerMinute()) {
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Request limit exceeded.\"}");
                return;
            }
        } catch (DataAccessException ignored) {
            // API traffic remains available during Redis failover; edge gateway limits remain the first defense.
        }
        chain.doFilter(request, response);
    }
}

