package com.kafein.discovery.controller;

import com.kafein.discovery.dto.request.ClassifyRequest;
import com.kafein.discovery.dto.response.ClassifyResponse;
import com.kafein.discovery.service.ClassifyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ClassifyController {

    private final ClassifyService classifyService;

    @PostMapping("/classify")
    public ResponseEntity<ClassifyResponse> classifyColumn(@Valid @RequestBody ClassifyRequest request) {
        log.info("Received classification request for column: {}", request.columnId());

        try {
            ClassifyResponse response = classifyService.classifyColumn(
                request.columnId(), request.sampleCount()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Classification validation error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Classification failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
