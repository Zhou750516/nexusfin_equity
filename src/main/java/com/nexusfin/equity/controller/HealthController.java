package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.response.HealthStatusResponse;
import com.nexusfin.equity.dto.response.Result;
import java.time.Instant;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/equity")
public class HealthController {

    @GetMapping("/health")
    public Result<HealthStatusResponse> health() {
        return Result.success(new HealthStatusResponse(
                "nexusfin-equity",
                "UP",
                Instant.now().toString()
        ));
    }
}
