package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelService")
class ModelServiceTest {

    @Mock
    private ModelGateway modelGateway;

    @InjectMocks
    private ModelService modelService;

    private Model activeModel;

    @BeforeEach
    void setUp() {
        activeModel = new Model();
        activeModel.setId(1L);
        activeModel.setModelCode("openai/gpt-4o");
        activeModel.setStatus(Model.ModelStatus.ACTIVE);
    }

    @Test
    @DisplayName("delete 应将模型状态设置为 INACTIVE")
    void delete_setsStatusToInactive() {
        when(modelGateway.findById(1L)).thenReturn(Optional.of(activeModel));
        when(modelGateway.save(any(Model.class))).thenReturn(activeModel);

        modelService.delete(1L);

        verify(modelGateway).save(any(Model.class));
        assertThat(activeModel.getStatus()).isEqualTo(Model.ModelStatus.INACTIVE);
    }

    @Test
    @DisplayName("delete 不存在的模型应抛出异常")
    void delete_notFound_throwsException() {
        when(modelGateway.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modelService.delete(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Model not found");
    }
}
