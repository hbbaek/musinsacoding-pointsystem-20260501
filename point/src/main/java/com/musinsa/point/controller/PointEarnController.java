package com.musinsa.point.controller;

import com.musinsa.point.dto.PointEarnDto;
import com.musinsa.point.service.PointEarnService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PointEarnController {
    private PointEarnService pointEarnService;

    @PostMapping("/api/point/earn")
    public PointEarnDto.EarnResponse earn(@RequestBody @Valid PointEarnDto.EarnRequest request) {
        return pointEarnService.earnPoint(request);
    }

    @PostMapping("/api/point/earn/cancel")
    public ResponseEntity cancel(@RequestBody @Valid PointEarnDto.CancelRequest request) {
        pointEarnService.cancelEarnPoint(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
