package com.example.resource_booking.controller;

import com.example.resource_booking.dto.ResourceRequest;
import com.example.resource_booking.dto.ResourceResponse;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ResourceResponse>> getAllResources() {
        List<ResourceResponse> responses = resourceService.getAllResources().stream()
                .map(ResourceResponse::new)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(new ResourceResponse(resourceService.getResourceById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(new ResourceResponse(resourceService.createResource(resource)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResourceResponse> updateResource(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        Resource resourceDetails = Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(new ResourceResponse(resourceService.updateResource(id, resourceDetails)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
