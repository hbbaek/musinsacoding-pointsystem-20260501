package com.musinsa.point.controller;

import com.musinsa.point.dto.PointUseDto;
import com.musinsa.point.service.PointUseService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PointUseController {
    private final PointUseService pointUseService;

    @PostMapping("/api/point/use")
    private PointUseDto.UseResponse use(@RequestBody @Valid PointUseDto.UseRequest request) {
        return pointUseService.usePoint(request);
    }

    @PostMapping("/api/point/use/cancel")
    private ResponseEntity cancel(@RequestBody @Valid PointUseDto.CancelRequest request) {
        pointUseService.cancelUsePoint(request);
        return new ResponseEntity(HttpStatus.OK);
    }
}