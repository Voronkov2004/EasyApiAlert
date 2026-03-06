package io.github.voronkov.easyapialert;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@RequiredArgsConstructor
public class RequestMonitor implements HandlerInterceptor {

    private final StatsCollector statsCollector;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startNanos", System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object startObj = request.getAttribute("startNanos");
        if (!(startObj instanceof Long startNanos)) return;

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;

        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = pattern != null ? pattern.toString() : RouteNormalizer.normalize(request.getRequestURI());

        statsCollector.record(request.getMethod(), route, response.getStatus(), durationMs);
    }
}
