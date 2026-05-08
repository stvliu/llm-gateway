package com.codingas.gateway.infrastructure.template.gateway;

import com.codingas.gateway.domain.template.entity.MarketStatus;
import com.codingas.gateway.domain.template.entity.ProviderTemplate;
import com.codingas.gateway.domain.template.entity.TemplateType;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateDo;
import com.codingas.gateway.infrastructure.template.database.ProviderTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProviderTemplateGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderTemplateGatewayImpl 测试")
class ProviderTemplateGatewayImplTest {

    @Mock
    private ProviderTemplateRepository repository;

    @InjectMocks
    private ProviderTemplateGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存模板成功")
        void save_validEntity_returnsSaved() {
            // given
            ProviderTemplate entity = createTestEntity();
            ProviderTemplateDo savedDo = createTestDo();
            savedDo.setId(1L);

            when(repository.save(any())).thenReturn(savedDo);

            // when
            ProviderTemplate result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTemplateCode()).isEqualTo("openai");
            verify(repository).save(any());
        }

        @Test
        @DisplayName("保存带 ID 的模板成功")
        void save_entityWithId_returnsSaved() {
            // given
            ProviderTemplate entity = createTestEntity();
            entity.setId(1L);
            ProviderTemplateDo savedDo = createTestDo();
            savedDo.setId(1L);

            when(repository.save(any())).thenReturn(savedDo);

            // when
            ProviderTemplate result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("findById 方法测试")
    class FindByIdTests {

        @Test
        @DisplayName("找到模板返回实体")
        void findById_existingId_returnsEntity() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            doEntity.setId(1L);
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));

            // when
            Optional<ProviderTemplate> result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
            assertThat(result.get().getTemplateCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("未找到返回空")
        void findById_nonExistingId_returnsEmpty() {
            // given
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // when
            Optional<ProviderTemplate> result = gateway.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTemplateCode 方法测试")
    class FindByTemplateCodeTests {

        @Test
        @DisplayName("通过编码找到模板")
        void findByTemplateCode_existingCode_returnsEntity() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            when(repository.findByTemplateCode("openai")).thenReturn(Optional.of(doEntity));

            // when
            Optional<ProviderTemplate> result = gateway.findByTemplateCode("openai");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getTemplateCode()).isEqualTo("openai");
        }

        @Test
        @DisplayName("未找到返回空")
        void findByTemplateCode_nonExistingCode_returnsEmpty() {
            // given
            when(repository.findByTemplateCode("unknown")).thenReturn(Optional.empty());

            // when
            Optional<ProviderTemplate> result = gateway.findByTemplateCode("unknown");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOfficialTemplates 方法测试")
    class FindOfficialTemplatesTests {

        @Test
        @DisplayName("查询所有官方模板")
        void findOfficialTemplates_returnsList() {
            // given
            ProviderTemplateDo do1 = createTestDo();
            do1.setId(1L);
            do1.setTemplateType(ProviderTemplateDo.TemplateType.OFFICIAL);

            ProviderTemplateDo do2 = createTestDo();
            do2.setId(2L);
            do2.setTemplateCode("anthropic");
            do2.setTemplateName("Anthropic");
            do2.setTemplateType(ProviderTemplateDo.TemplateType.OFFICIAL);

            when(repository.findOfficialTemplates()).thenReturn(List.of(do1, do2));

            // when
            List<ProviderTemplate> result = gateway.findOfficialTemplates();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTemplateType()).isEqualTo(TemplateType.OFFICIAL);
            assertThat(result.get(1).getTemplateCode()).isEqualTo("anthropic");
        }

        @Test
        @DisplayName("无官方模板返回空列表")
        void findOfficialTemplates_emptyList() {
            // given
            when(repository.findOfficialTemplates()).thenReturn(List.of());

            // when
            List<ProviderTemplate> result = gateway.findOfficialTemplates();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findMarketTemplates 方法测试")
    class FindMarketTemplatesTests {

        @Test
        @DisplayName("分页查询市场模板")
        void findMarketTemplates_returnsPage() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            doEntity.setMarketStatus(ProviderTemplateDo.MarketStatus.PUBLISHED);
            Page<ProviderTemplateDo> page = new PageImpl<>(List.of(doEntity));
            when(repository.findMarketTemplates(any())).thenReturn(page);

            // when
            Page<ProviderTemplate> result = gateway.findMarketTemplates(PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getMarketStatus()).isEqualTo(MarketStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("findByConditions 方法测试")
    class FindByConditionsTests {

        @Test
        @DisplayName("动态条件查询")
        void findByConditions_returnsPage() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            Page<ProviderTemplateDo> page = new PageImpl<>(List.of(doEntity));
            when(repository.findByConditions(any(), any(), any(), any(), any())).thenReturn(page);

            // when
            Page<ProviderTemplate> result = gateway.findByConditions(
                TemplateType.OFFICIAL, "OPENAI", "test", MarketStatus.PRIVATE, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("空条件查询")
        void findByConditions_nullConditions_returnsPage() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            Page<ProviderTemplateDo> page = new PageImpl<>(List.of(doEntity));
            when(repository.findByConditions(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(page);

            // when
            Page<ProviderTemplate> result = gateway.findByConditions(
                null, null, null, null, PageRequest.of(0, 10)
            );

            // then
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByAuthorId 方法测试")
    class FindByAuthorIdTests {

        @Test
        @DisplayName("查询作者的模板列表")
        void findByAuthorId_returnsList() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            doEntity.setAuthorId(1L);
            when(repository.findByAuthorId(1L)).thenReturn(List.of(doEntity));

            // when
            List<ProviderTemplate> result = gateway.findByAuthorId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAuthorId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("existsByTemplateCode 方法测试")
    class ExistsByTemplateCodeTests {

        @Test
        @DisplayName("模板编码存在")
        void existsByTemplateCode_existing_returnsTrue() {
            // given
            when(repository.existsByTemplateCode("openai")).thenReturn(true);

            // when
            boolean result = gateway.existsByTemplateCode("openai");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("模板编码不存在")
        void existsByTemplateCode_nonExisting_returnsFalse() {
            // given
            when(repository.existsByTemplateCode("unknown")).thenReturn(false);

            // when
            boolean result = gateway.existsByTemplateCode("unknown");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteById 方法测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("软删除成功")
        void deleteById_callsSoftDelete() {
            // given
            doNothing().when(repository).softDelete(anyLong(), any());

            // when
            gateway.deleteById(1L);

            // then
            verify(repository).softDelete(eq(1L), any());
        }
    }

    @Nested
    @DisplayName("updateMarketStatus 方法测试")
    class UpdateMarketStatusTests {

        @Test
        @DisplayName("更新市场状态成功")
        void updateMarketStatus_callsRepository() {
            // given
            doNothing().when(repository).updateMarketStatus(anyLong(), any());

            // when
            gateway.updateMarketStatus(1L, MarketStatus.PUBLISHED);

            // then
            verify(repository).updateMarketStatus(1L, ProviderTemplateDo.MarketStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("incrementDownloadCount 方法测试")
    class IncrementDownloadCountTests {

        @Test
        @DisplayName("增加使用次数成功")
        void incrementDownloadCount_callsRepository() {
            // given
            doNothing().when(repository).incrementDownloadCount(anyLong());

            // when
            gateway.incrementDownloadCount(1L);

            // then
            verify(repository).incrementDownloadCount(1L);
        }
    }

    @Nested
    @DisplayName("DO 与 Entity 转换测试")
    class ConversionTests {

        @Test
        @DisplayName("DO 转 Entity 正确转换所有字段")
        void toEntity_allFields() {
            // given
            ProviderTemplateDo doEntity = createTestDo();
            doEntity.setId(1L);
            doEntity.setAuthorId(100L);
            doEntity.setAuthorName("testuser");
            doEntity.setDescription("Test description");
            doEntity.setIconUrl("https://example.com/icon.png");
            doEntity.setTags(List.of("gpt", "chat"));

            // when
            Optional<ProviderTemplate> result = gateway.findById(1L);

            // manually trigger conversion by mocking repository
            when(repository.findById(1L)).thenReturn(Optional.of(doEntity));
            result = gateway.findById(1L);

            // then
            assertThat(result).isPresent();
            ProviderTemplate entity = result.get();
            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getTemplateCode()).isEqualTo("openai");
            assertThat(entity.getTemplateName()).isEqualTo("OpenAI");
            assertThat(entity.getTemplateType()).isEqualTo(TemplateType.OFFICIAL);
            assertThat(entity.getProviderType()).isEqualTo("OPENAI");
            assertThat(entity.getAuthorId()).isEqualTo(100L);
            assertThat(entity.getAuthorName()).isEqualTo("testuser");
            assertThat(entity.getMarketStatus()).isEqualTo(MarketStatus.PRIVATE);
            assertThat(entity.getDescription()).isEqualTo("Test description");
            assertThat(entity.getIconUrl()).isEqualTo("https://example.com/icon.png");
            assertThat(entity.getTags()).containsExactly("gpt", "chat");
        }

        @Test
        @DisplayName("Entity 转 DO 正确转换枚举")
        void toDo_enumConversion() {
            // given
            ProviderTemplate entity = createTestEntity();
            entity.setTemplateType(TemplateType.USER);
            entity.setMarketStatus(MarketStatus.PUBLISHED);
            entity.setStatus(ProviderTemplate.TemplateStatus.DISABLED);

            ProviderTemplateDo savedDo = createTestDo();
            savedDo.setTemplateType(ProviderTemplateDo.TemplateType.USER);
            savedDo.setMarketStatus(ProviderTemplateDo.MarketStatus.PUBLISHED);
            savedDo.setStatus(ProviderTemplateDo.TemplateStatus.DISABLED);

            when(repository.save(any())).thenReturn(savedDo);

            // when
            ProviderTemplate result = gateway.save(entity);

            // then
            assertThat(result.getTemplateType()).isEqualTo(TemplateType.USER);
            assertThat(result.getMarketStatus()).isEqualTo(MarketStatus.PUBLISHED);
            assertThat(result.getStatus()).isEqualTo(ProviderTemplate.TemplateStatus.DISABLED);
        }
    }

    // Helper methods
    private ProviderTemplate createTestEntity() {
        ProviderTemplate entity = new ProviderTemplate();
        entity.setTemplateCode("openai");
        entity.setTemplateName("OpenAI");
        entity.setTemplateType(TemplateType.OFFICIAL);
        entity.setProviderType("OPENAI");
        entity.setProviderConfig(Map.of("base_url", "https://api.openai.com"));
        entity.setModelsConfig(List.of(Map.of("provider_model_id", "gpt-4o")));
        entity.setMarketStatus(MarketStatus.PRIVATE);
        entity.setStatus(ProviderTemplate.TemplateStatus.ACTIVE);
        entity.setDownloadCount(0);
        return entity;
    }

    private ProviderTemplateDo createTestDo() {
        ProviderTemplateDo doEntity = new ProviderTemplateDo();
        doEntity.setTemplateCode("openai");
        doEntity.setTemplateName("OpenAI");
        doEntity.setTemplateType(ProviderTemplateDo.TemplateType.OFFICIAL);
        doEntity.setProviderType("OPENAI");
        doEntity.setProviderConfig(Map.of("base_url", "https://api.openai.com"));
        doEntity.setModelsConfig(List.of(Map.of("provider_model_id", "gpt-4o")));
        doEntity.setMarketStatus(ProviderTemplateDo.MarketStatus.PRIVATE);
        doEntity.setStatus(ProviderTemplateDo.TemplateStatus.ACTIVE);
        doEntity.setDownloadCount(0);
        return doEntity;
    }
}
