package com.example.resource_booking.controller;

import com.example.resource_booking.model.Resource;
import com.example.resource_booking.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @GetMapping
    public List<Resource> getAllResources() {
        return resourceService.getAllResources();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Resource createResource(@RequestBody Resource resource) {
        return resourceService.createResource(resource);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> updateResource(@PathVariable Long id, @RequestBody Resource resourceDetails) {
        return ResponseEntity.ok(resourceService.updateResource(id, resourceDetails));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
