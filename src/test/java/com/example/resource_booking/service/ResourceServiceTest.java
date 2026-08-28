package com.example.resource_booking.service;

import com.example.resource_booking.exception.ResourceNotFoundException;
import com.example.resource_booking.model.Resource;
import com.example.resource_booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    private Resource resource;

    @BeforeEach
    void setUp() {
        resource = Resource.builder()
                .id(1L)
                .name("Conference Room A")
                .type("Room")
                .description("Large conference room")
                .build();
    }

    @Test
    void testGetAllResources() {
        when(resourceRepository.findAll()).thenReturn(List.of(resource));
        List<Resource> resources = resourceService.getAllResources();
        assertEquals(1, resources.size());
        assertEquals("Conference Room A", resources.get(0).getName());
        verify(resourceRepository, times(1)).findAll();
    }

    @Test
    void testGetResourceById_Success() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        Resource found = resourceService.getResourceById(1L);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetResourceById_NotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResourceById(99L));
    }

    @Test
    void testCreateResource() {
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);
        Resource created = resourceService.createResource(resource);
        assertNotNull(created);
        assertEquals("Conference Room A", created.getName());
    }

    @Test
    void testUpdateResource() {
        Resource updatedInfo = Resource.builder()
                .name("Conference Room B")
                .type("Room")
                .description("Updated description")
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        Resource result = resourceService.updateResource(1L, updatedInfo);
        assertNotNull(result);
        assertEquals("Conference Room B", resource.getName());
        assertEquals("Updated description", resource.getDescription());
    }

    @Test
    void testDeleteResource() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        doNothing().when(resourceRepository).delete(resource);

        assertDoesNotThrow(() -> resourceService.deleteResource(1L));
        verify(resourceRepository, times(1)).delete(resource);
    }
}
