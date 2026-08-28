package com.example.resource_booking.dto;

import com.example.resource_booking.model.Resource;

public class ResourceResponse {

    private Long id;
    private String name;
    private String type;
    private String description;

    public ResourceResponse() {}

    public ResourceResponse(Resource resource) {
        if (resource != null) {
            this.id = resource.getId();
            this.name = resource.getName();
            this.type = resource.getType();
            this.description = resource.getDescription();
        }
    }

    public ResourceResponse(Long id, String name, String type, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
