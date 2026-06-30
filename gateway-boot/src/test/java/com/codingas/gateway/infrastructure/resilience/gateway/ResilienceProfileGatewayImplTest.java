package com.codingas.gateway.infrastructure.resilience.gateway;

import com.codingas.gateway.domain.resilience.entity.ResilienceMode;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.infrastructure.resilience.gateway.database.dataobject.ResilienceProfileDo;
import com.codingas.gateway.infrastructure.resilience.gateway.database.repository.ResilienceProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResilienceProfileGatewayImpl 单元测试
 *
 * <p>验证 ResilienceProfile 容灾画像的持久化网关行为：findById/findByCode/findAll/save
 * 的 DO↔Entity 转换（含 mode 枚举↔字符串互转）与委派逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilienceProfileGatewayImpl 测试")
class ResilienceProfileGatewayImplTest {

    @Mock
    private ResilienceProfileRepository repository;

    @InjectMocks
    private ResilienceProfileGatewayImpl gateway;

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("存在时返回 ResilienceProfile 实体（含 mode 枚举还原）")
        void findById_existingId_returnsEntity() {
            ResilienceProfileDo doEntity = createTestDo();
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            ResilienceProfile result = gateway.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("RES-STANDARD");
            assertThat(result.getName()).isEqualTo("标准容灾画像");
            assertThat(result.getMode()).isEqualTo(ResilienceMode.STANDARD);
            assertThat(result.isEnableL2ModelDegradation()).isTrue();
            assertThat(result.getDegradationMaxDepth()).isEqualTo(2);
            assertThat(result.getTimeout()).isEqualTo(0);
        }

        @Test
        @DisplayName("不存在时返回 null")
        void findById_nonExistingId_returnsNull() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            ResilienceProfile result = gateway.findById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findByCode 方法测试")
    class FindByCodeTests {

        @Test
        @DisplayName("按编码查找命中返回实体")
        void findByCode_existingCode_returnsEntity() {
            ResilienceProfileDo doEntity = createTestDo();
            when(repository.findByCode("RES-STANDARD")).thenReturn(Optional.of(doEntity));

            ResilienceProfile result = gateway.findByCode("RES-STANDARD");

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("RES-STANDARD");
            assertThat(result.getMode()).isEqualTo(ResilienceMode.STANDARD);
        }

        @Test
        @DisplayName("按编码查找未命中返回 null")
        void findByCode_nonExistingCode_returnsNull() {
            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            ResilienceProfile result = gateway.findByCode("UNKNOWN");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("findAll 方法测试")
    class FindAllTests {

        @Test
        @DisplayName("返回全部 ResilienceProfile（含多档位 mode 还原）")
        void findAll_returnsAll() {
            ResilienceProfileDo d1 = createTestDo();
            ResilienceProfileDo d2 = createTestDo();
            d2.setId(2L);
            d2.setCode("RES-STRICT");
            d2.setName("严格容灾画像");
            d2.setMode("STRICT");
            when(repository.findAll()).thenReturn(List.of(d1, d2));

            List<ResilienceProfile> result = gateway.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ResilienceProfile::getCode)
                    .containsExactly("RES-STANDARD", "RES-STRICT");
            assertThat(result).extracting(ResilienceProfile::getMode)
                    .containsExactly(ResilienceMode.STANDARD, ResilienceMode.STRICT);
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存 ResilienceProfile 并回写转换结果（mode 枚举→字符串）")
        void save_validEntity_returnsSaved() {
            ResilienceProfile entity = createTestEntity();
            ResilienceProfileDo savedDo = createTestDo();
            when(repository.save(any())).thenReturn(savedDo);

            ResilienceProfile result = gateway.save(entity);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getMode()).isEqualTo(ResilienceMode.STANDARD);
            ArgumentCaptor<ResilienceProfileDo> captor = ArgumentCaptor.forClass(ResilienceProfileDo.class);
            verify(repository).save(captor.capture());
            ResilienceProfileDo captured = captor.getValue();
            assertThat(captured.getCode()).isEqualTo("RES-STANDARD");
            assertThat(captured.getMode()).isEqualTo("STANDARD");
            assertThat(captured.isEnableL2ModelDegradation()).isTrue();
            assertThat(captured.getDegradationMaxDepth()).isEqualTo(2);
            assertThat(captured.getTimeout()).isEqualTo(0);
        }

        @Test
        @DisplayName("save 透传审计字段 createdBy/updatedBy")
        void save_preservesAuditFields() {
            ResilienceProfile entity = createTestEntity();
            entity.setCreatedBy(7L);
            entity.setUpdatedBy(7L);
            ResilienceProfileDo savedDo = createTestDo();
            when(repository.save(any())).thenReturn(savedDo);

            gateway.save(entity);

            ArgumentCaptor<ResilienceProfileDo> captor = ArgumentCaptor.forClass(ResilienceProfileDo.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(7L);
            assertThat(captor.getValue().getUpdatedBy()).isEqualTo(7L);
        }
    }

    // ===== Helper methods =====

    private ResilienceProfileDo createTestDo() {
        ResilienceProfileDo d = new ResilienceProfileDo();
        d.setId(1L);
        d.setCode("RES-STANDARD");
        d.setName("标准容灾画像");
        d.setMode("STANDARD");
        d.setEnableL2ModelDegradation(true);
        d.setDegradationMaxDepth(2);
        d.setTimeout(0);
        return d;
    }

    private ResilienceProfile createTestEntity() {
        ResilienceProfile entity = new ResilienceProfile();
        entity.setId(1L);
        entity.setCode("RES-STANDARD");
        entity.setName("标准容灾画像");
        entity.setMode(ResilienceMode.STANDARD);
        entity.setEnableL2ModelDegradation(true);
        entity.setDegradationMaxDepth(2);
        entity.setTimeout(0);
        return entity;
    }
}
