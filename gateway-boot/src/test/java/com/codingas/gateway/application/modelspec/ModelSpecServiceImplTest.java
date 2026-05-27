package com.codingas.gateway.application.modelspec;

import com.codingas.gateway.application.modelspec.dto.ModelSpecCreateRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecQueryRequest;
import com.codingas.gateway.application.modelspec.dto.ModelSpecResponse;
import com.codingas.gateway.application.modelspec.dto.ModelSpecUpdateRequest;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelSpec;
import com.codingas.gateway.domain.supply.enums.ModelSpecState;
import com.codingas.gateway.domain.supply.gateway.ModelSpecGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * ModelSpecServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ModelSpecServiceImplTest {

    @Mock
    private ModelSpecGateway modelSpecGateway;

    @InjectMocks
    private ModelSpecServiceImpl service;

    private static final Long SPEC_ID = 1L;

    @Test
    void create_success() {
        ModelSpec saved = createSampleSpec();
        when(modelSpecGateway.save(any(ModelSpec.class))).thenReturn(saved);

        ModelSpecCreateRequest request = new ModelSpecCreateRequest();
        request.setProviderModelId("gpt-4o");
        request.setDisplayName("GPT-4o");
        request.setModelFamily("gpt-4");
        request.setContextWindow(128000);
        request.setMaxOutputTokens(4096);
        request.setCapabilities(Map.of("vision", true));
        request.setModalities(List.of("text", "image"));
        request.setPriority(1);
        request.setWeight(10);

        ModelSpecResponse response = service.create(request);

        assertNotNull(response);
        assertEquals(SPEC_ID, response.getId());
        assertEquals("gpt-4o", response.getProviderModelId());
        assertEquals("ACTIVE", response.getState());
        verify(modelSpecGateway).save(argThat(spec ->
                spec.getState() == ModelSpecState.ACTIVE
                        && "gpt-4o".equals(spec.getProviderModelId())
        ));
    }

    @Test
    void getById_success() {
        ModelSpec spec = createSampleSpec();
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.of(spec));

        ModelSpecResponse response = service.getById(SPEC_ID);

        assertNotNull(response);
        assertEquals(SPEC_ID, response.getId());
        assertEquals("gpt-4o", response.getProviderModelId());
    }

    @Test
    void getById_notFound() {
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(SPEC_ID));
    }

    @Test
    void query_all() {
        ModelSpec spec = createSampleSpec();
        when(modelSpecGateway.findAll()).thenReturn(List.of(spec));

        ModelSpecQueryRequest request = new ModelSpecQueryRequest();

        List<ModelSpecResponse> responses = service.query(request);

        assertEquals(1, responses.size());
    }

    @Test
    void query_filterByState() {
        ModelSpec activeSpec = createSampleSpec();
        ModelSpec inactiveSpec = createSampleSpec();
        inactiveSpec.setId(2L);
        inactiveSpec.setState(ModelSpecState.INACTIVE);

        when(modelSpecGateway.findAll()).thenReturn(List.of(activeSpec, inactiveSpec));

        ModelSpecQueryRequest request = new ModelSpecQueryRequest();
        request.setState("ACTIVE");

        List<ModelSpecResponse> responses = service.query(request);

        assertEquals(1, responses.size());
        assertEquals("ACTIVE", responses.get(0).getState());
    }

    @Test
    void query_filterByKeyword() {
        ModelSpec spec1 = createSampleSpec();
        ModelSpec spec2 = createSampleSpec();
        spec2.setId(2L);
        spec2.setProviderModelId("claude-3");
        spec2.setDisplayName("Claude 3");

        when(modelSpecGateway.findAll()).thenReturn(List.of(spec1, spec2));

        ModelSpecQueryRequest request = new ModelSpecQueryRequest();
        request.setKeyword("gpt");

        List<ModelSpecResponse> responses = service.query(request);

        assertEquals(1, responses.size());
        assertEquals("gpt-4o", responses.get(0).getProviderModelId());
    }

    @Test
    void update_success() {
        ModelSpec spec = createSampleSpec();
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.of(spec));
        when(modelSpecGateway.save(any(ModelSpec.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelSpecUpdateRequest request = new ModelSpecUpdateRequest();
        request.setDisplayName("GPT-4o Updated");
        request.setContextWindow(256000);
        request.setPriority(5);

        ModelSpecResponse response = service.update(SPEC_ID, request);

        assertNotNull(response);
        assertEquals("GPT-4o Updated", response.getDisplayName());
        assertEquals(256000, response.getContextWindow());
        assertEquals(5, response.getPriority());
        verify(modelSpecGateway).save(argThat(s ->
                "GPT-4o Updated".equals(s.getDisplayName())
                        && s.getContextWindow() == 256000
        ));
    }

    @Test
    void update_notFound() {
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.empty());

        ModelSpecUpdateRequest request = new ModelSpecUpdateRequest();
        request.setDisplayName("Updated");

        assertThrows(ResourceNotFoundException.class, () -> service.update(SPEC_ID, request));
    }

    @Test
    void update_state() {
        ModelSpec spec = createSampleSpec();
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.of(spec));
        when(modelSpecGateway.save(any(ModelSpec.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelSpecUpdateRequest request = new ModelSpecUpdateRequest();
        request.setState("INACTIVE");

        ModelSpecResponse response = service.update(SPEC_ID, request);

        assertEquals("INACTIVE", response.getState());
    }

    @Test
    void delete_success() {
        ModelSpec spec = createSampleSpec();
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.of(spec));

        service.delete(SPEC_ID);

        verify(modelSpecGateway).delete(spec);
    }

    @Test
    void delete_notFound() {
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(SPEC_ID));
    }

    @Test
    void setEnabled_true() {
        ModelSpec spec = createSampleSpec();
        spec.setState(ModelSpecState.INACTIVE);
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.of(spec));
        when(modelSpecGateway.save(any(ModelSpec.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelSpecResponse response = service.setEnabled(SPEC_ID, true);

        assertEquals("ACTIVE", response.getState());
    }

    @Test
    void setEnabled_false() {
        ModelSpec spec = createSampleSpec();
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.of(spec));
        when(modelSpecGateway.save(any(ModelSpec.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelSpecResponse response = service.setEnabled(SPEC_ID, false);

        assertEquals("INACTIVE", response.getState());
    }

    @Test
    void setEnabled_notFound() {
        when(modelSpecGateway.findById(SPEC_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.setEnabled(SPEC_ID, true));
    }

    private ModelSpec createSampleSpec() {
        ModelSpec spec = new ModelSpec();
        spec.setId(SPEC_ID);
        spec.setProviderModelId("gpt-4o");
        spec.setDisplayName("GPT-4o");
        spec.setModelFamily("gpt-4");
        spec.setContextWindow(128000);
        spec.setMaxInputTokens(128000);
        spec.setMaxOutputTokens(4096);
        spec.setCapabilities(Map.of("vision", true));
        spec.setModalities(List.of("text", "image"));
        spec.setState(ModelSpecState.ACTIVE);
        spec.setPriority(1);
        spec.setWeight(10);
        return spec;
    }
}
