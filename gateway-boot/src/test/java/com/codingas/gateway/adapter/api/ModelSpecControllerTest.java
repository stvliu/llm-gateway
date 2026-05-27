package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.modelspec.ModelSpecService;
import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelSpecController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ModelSpecControllerTest {

    @Mock
    private ModelSpecService modelSpecService;

    @InjectMocks
    private ModelSpecController controller;

    private static final Long SPEC_ID = 1L;

    private ModelSpecResponse createResponse(Long id) {
        ModelSpecResponse response = new ModelSpecResponse();
        response.setId(id);
        response.setProviderModelId("gpt-4o");
        response.setDisplayName("GPT-4o");
        response.setModelFamily("gpt-4");
        response.setContextWindow(128000);
        response.setMaxOutputTokens(4096);
        response.setCapabilities(Map.of("vision", true));
        response.setModalities(List.of("text", "image"));
        response.setState("ACTIVE");
        response.setPriority(1);
        response.setWeight(10);
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }

    @Test
    void create_success() {
        ModelSpecCreateRequest request = new ModelSpecCreateRequest();
        request.setProviderModelId("gpt-4o");

        ModelSpecResponse expected = createResponse(SPEC_ID);
        when(modelSpecService.create(any(ModelSpecCreateRequest.class))).thenReturn(expected);

        ResponseEntity<ModelSpecResponse> result = controller.create(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getId()).isEqualTo(SPEC_ID);
    }

    @Test
    void getById_success() {
        ModelSpecResponse expected = createResponse(SPEC_ID);
        when(modelSpecService.getById(SPEC_ID)).thenReturn(expected);

        ResponseEntity<ModelSpecResponse> result = controller.getById(SPEC_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getProviderModelId()).isEqualTo("gpt-4o");
    }

    @Test
    void query_success() {
        ModelSpecQueryRequest request = new ModelSpecQueryRequest();

        when(modelSpecService.query(any(ModelSpecQueryRequest.class)))
                .thenReturn(List.of(createResponse(SPEC_ID)));

        ResponseEntity<List<ModelSpecResponse>> result = controller.query(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void update_success() {
        ModelSpecUpdateRequest request = new ModelSpecUpdateRequest();
        request.setDisplayName("GPT-4o Updated");

        ModelSpecResponse expected = createResponse(SPEC_ID);
        expected.setDisplayName("GPT-4o Updated");
        when(modelSpecService.update(eq(SPEC_ID), any(ModelSpecUpdateRequest.class))).thenReturn(expected);

        ResponseEntity<ModelSpecResponse> result = controller.update(SPEC_ID, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getDisplayName()).isEqualTo("GPT-4o Updated");
    }

    @Test
    void delete_success() {
        ResponseEntity<Void> result = controller.delete(SPEC_ID);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(modelSpecService).delete(SPEC_ID);
    }

    @Test
    void setEnabled_true() {
        ModelSpecResponse expected = createResponse(SPEC_ID);
        when(modelSpecService.setEnabled(SPEC_ID, true)).thenReturn(expected);

        ResponseEntity<ModelSpecResponse> result = controller.setEnabled(SPEC_ID, true);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        verify(modelSpecService).setEnabled(SPEC_ID, true);
    }

    @Test
    void setEnabled_false() {
        ModelSpecResponse expected = createResponse(SPEC_ID);
        expected.setState("INACTIVE");
        when(modelSpecService.setEnabled(SPEC_ID, false)).thenReturn(expected);

        ResponseEntity<ModelSpecResponse> result = controller.setEnabled(SPEC_ID, false);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getState()).isEqualTo("INACTIVE");
        verify(modelSpecService).setEnabled(SPEC_ID, false);
    }
}
