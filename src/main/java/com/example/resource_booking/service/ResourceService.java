package com.example.resource_booking.service;

import com.example.resource_booking.exception.ResourceNotFoundException;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {
    
    @Autowired
    private ResourceRepository resourceRepository;

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    public Resource createResource(Resource resource) {
        return resourceRepository.save(resource);
    }

    public Resource updateResource(Long id, Resource resourceDetails) {
        Resource resource = getResourceById(id);
        resource.setName(resourceDetails.getName());
        resource.setType(resourceDetails.getType());
        resource.setDescription(resourceDetails.getDescription());
        return resourceRepository.save(resource);
    }

    public void deleteResource(Long id) {
        Resource resource = getResourceById(id);
        resourceRepository.delete(resource);
    }
}
