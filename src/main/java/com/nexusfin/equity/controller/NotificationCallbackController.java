package com.nexusfin.equity.controller;

import com.nexusfin.equity.dto.request.ExerciseCallbackRequest;
import com.nexusfin.equity.dto.request.GrantForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RepaymentForwardCallbackRequest;
import com.nexusfin.equity.dto.request.RefundCallbackRequest;
import com.nexusfin.equity.dto.response.Result;
import com.nexusfin.equity.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/callbacks")
public class NotificationCallbackController {

    private final NotificationService notificationService;

    public NotificationCallbackController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/exercise-equity")
    public Result<Void> handleExercise(@Valid @RequestBody ExerciseCallbackRequest request) {
        notificationService.handleExercise(request);
        return Result.success(null);
    }

    @PostMapping("/refund")
    public Result<Void> handleRefund(@Valid @RequestBody RefundCallbackRequest request) {
        notificationService.handleRefund(request);
        return Result.success(null);
    }

    @PostMapping("/grant/forward")
    public Result<Void> handleGrant(@Valid @RequestBody GrantForwardCallbackRequest request) {
        notificationService.handleGrant(request);
        return Result.success(null);
    }

    @PostMapping("/repayment/forward")
    public Result<Void> handleRepayment(@Valid @RequestBody RepaymentForwardCallbackRequest request) {
        notificationService.handleRepayment(request);
        return Result.success(null);
    }
}
