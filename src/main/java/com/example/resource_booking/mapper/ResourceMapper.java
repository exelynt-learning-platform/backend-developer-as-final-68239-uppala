package com.example.resource_booking.mapper;

import com.example.resource_booking.dto.ResourceRequest;
import com.example.resource_booking.dto.ResourceResponse;
import com.example.resource_booking.model.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(resource);
    }

    public Resource toEntity(ResourceRequest request) {
        return Resource.builder()
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .build();
    }
}
