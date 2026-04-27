package com.ecommerce.aggregator.controller;

import com.ecommerce.aggregator.dto.DashboardResponse;
import com.ecommerce.aggregator.dto.SessionData;
import com.ecommerce.aggregator.service.DashboardAggregatorService;
import com.ecommerce.aggregator.service.SessionService;
import com.ecommerce.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/aggregate")
@RequiredArgsConstructor
public class AggregatorController {

    private final DashboardAggregatorService dashboardService;
    private final SessionService sessionService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(@RequestParam Long userId) {
        DashboardResponse response = dashboardService.aggregate(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/session/{userId}")
    public ResponseEntity<Void> createSession(
            @PathVariable Long userId,
            @RequestParam String username,
            @RequestParam(defaultValue = "USER") String role) {
        SessionData session = SessionData.builder()
            .userId(userId).username(username).role(role)
            .lastLogin(Instant.now()).build();
        sessionService.save(userId, session);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session/{userId}")
    public ResponseEntity<ApiResponse<SessionData>> getSession(@PathVariable Long userId) {
        Optional<SessionData> session = sessionService.get(userId);
        return session.map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
            .orElse(ResponseEntity.notFound().<ApiResponse<SessionData>>build());
    }

    @DeleteMapping("/session/{userId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long userId) {
        sessionService.invalidate(userId);
        return ResponseEntity.noContent().build();
    }
}
