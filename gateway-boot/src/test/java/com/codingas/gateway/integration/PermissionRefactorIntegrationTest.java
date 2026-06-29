package com.codingas.gateway.integration;

import com.codingas.gateway.application.proxy.routing.RouterChain;
import com.codingas.gateway.application.proxy.routing.RoutingRequest;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.infrastructure.application.gateway.database.dataobject.ApplicationChannelDo;
import com.codingas.gateway.infrastructure.application.gateway.database.repository.ApplicationChannelRepository;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelEndpointDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ChannelEndpointRepository;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P-r 权限重构集成测试
 *
 * <p>端到端验证权限锚点从 userId 切换为 applicationId 后，数据面权限路由的真实行为。
 * 通过真实 Spring 装配的 {@link RouterChain}（含 {@code PermissionRouter}）+ 真实 H2 持久化的
 * {@link ApplicationChannelRepository}/{@link ChannelRepository}，验证应用-渠道授权过滤的端到端链路。</p>
 *
 * <p>构造方式说明：基类 {@code FullContextIntegrationTestBase} 通过 {@code @MockBean} mock 了
 * {@code RoutingResolver}（导致 {@code ChatDispatchService} 不触发真实路由链），但 <b>未 mock</b>
 * {@link RouterChain}/{@code PermissionRouter}/{@code ApplicationChannelGateway}。
 * 因此本测试直接注入真实 {@link RouterChain} bean，调用 {@code routerChain.filter(instances, request)}
 * 驱动真实 {@code PermissionRouter} 按 {@code applicationId} 过滤；
 * {@code ApplicationChannelGateway}/{@code ChannelGateway} 走真实 H2 查询，
 * 验证 JPA 映射与权限锚点端到端正确性。</p>
 *
 * <p>场景2分歧说明：brief 描述"无 application_id 走 migration-default 软兜底 + 告警日志"
 * 属于<b>迁移层</b>（V52/V53 迁移脚本，迁移正确性已在 Task 1.6 覆盖）。
 * 运行时数据面 {@code PermissionRouter.getPermittedChannelIds} 对 {@code applicationId==null}
 * 直接返回空集（无运行时软兜底）。本测试场景2针对<b>运行时数据面</b>断言返回空集。</p>
 */
@DisplayName("P-r 权限重构集成测试")
class PermissionRefactorIntegrationTest extends FullContextIntegrationTestBase {

    private static final Long APP_A = 1001L;
    private static final Long APP_B = 1002L;
    private static final Long MODEL_M = 5001L;
    private static final Long USER_ID = 1L;

    @Autowired
    private RouterChain routerChain;

    @Autowired
    private ApplicationChannelRepository applicationChannelRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ChannelEndpointRepository channelEndpointRepository;

    private Long ch1Id;
    private Long ch2Id;
    private ModelInstance miCh1;
    private ModelInstance miCh2;

    @BeforeEach
    void setupPermissionData() {
        // 清理上一测试方法残留（H2 内存库跨方法持久）
        applicationChannelRepository.deleteAll();
        channelEndpointRepository.deleteAll();
        channelRepository.deleteAll();

        // ch1 + ch2 均为 ACTIVE，都挂模型 M
        ChannelDo ch1 = new ChannelDo();
        ch1.setProviderId(1L);
        ch1.setName("ch1");
        ch1.setState(ChannelState.ACTIVE.name());
        ch1.setTimeout(30);
        ch1Id = channelRepository.save(ch1).getId();

        ChannelDo ch2 = new ChannelDo();
        ch2.setProviderId(1L);
        ch2.setName("ch2");
        ch2.setState(ChannelState.ACTIVE.name());
        ch2.setTimeout(30);
        ch2Id = channelRepository.save(ch2).getId();

        // 为 ch1/ch2 各建一个 OPENAI 端点，供真实 HealthRouter 派生 endpointId（新鲜熔断器视为可用）
        ChannelEndpointDo ep1 = new ChannelEndpointDo();
        ep1.setChannelId(ch1Id);
        ep1.setProtocol(Protocol.OPENAI);
        ep1.setEndpointUrl("https://ch1.example.com/v1");
        channelEndpointRepository.save(ep1);

        ChannelEndpointDo ep2 = new ChannelEndpointDo();
        ep2.setChannelId(ch2Id);
        ep2.setProtocol(Protocol.OPENAI);
        ep2.setEndpointUrl("https://ch2.example.com/v1");
        channelEndpointRepository.save(ep2);

        // 应用 A 授权 ch1；应用 B 授权 ch2（权限锚点切换的核心数据）
        ApplicationChannelDo appACh1 = new ApplicationChannelDo();
        appACh1.setApplicationId(APP_A);
        appACh1.setChannelId(ch1Id);
        applicationChannelRepository.save(appACh1);

        ApplicationChannelDo appBCh2 = new ApplicationChannelDo();
        appBCh2.setApplicationId(APP_B);
        appBCh2.setChannelId(ch2Id);
        applicationChannelRepository.save(appBCh2);

        // 构造模型 M 的候选实例（ch1 + ch2 都挂 M），内存对象直接传入 RouterChain
        miCh1 = new ModelInstance();
        miCh1.setId(1L);
        miCh1.setChannelId(ch1Id);
        miCh1.setModelId(MODEL_M);
        miCh1.setState(ModelInstance.State.ACTIVE);
        miCh1.setPriority(100);
        miCh1.setWeight(100);

        miCh2 = new ModelInstance();
        miCh2.setId(2L);
        miCh2.setChannelId(ch2Id);
        miCh2.setModelId(MODEL_M);
        miCh2.setState(ModelInstance.State.ACTIVE);
        miCh2.setPriority(100);
        miCh2.setWeight(100);
    }

    @Test
    @DisplayName("场景1：应用 A 授权 ch1，请求模型 M（ch1+ch2 都挂 M），只能路由到 ch1")
    void appA_routesOnlyToCh1() {
        RoutingRequest request = new RoutingRequest(MODEL_M, APP_A, USER_ID, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = routerChain.filter(List.of(miCh1, miCh2), request);

        // 权限锚点 applicationId=APP_A → ApplicationChannel 查询仅返回 ch1 → ch2 被过滤
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(mi -> mi.getChannelId().equals(ch1Id));
        assertThat(result).noneMatch(mi -> mi.getChannelId().equals(ch2Id));
    }

    @Test
    @DisplayName("场景2：无 application_id 的 Key（applicationId=null）→ 运行时数据面返回空集（无软兜底）")
    void noApplicationId_returnsEmpty() {
        // 运行时数据面 PermissionRouter 对 applicationId==null 直接返回空集。
        // brief 场景2所述"migration-default 软兜底"属迁移层（V52/V53），本测试针对运行时数据面断言。
        RoutingRequest request = new RoutingRequest(MODEL_M, null, USER_ID, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = routerChain.filter(List.of(miCh1, miCh2), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("场景3：ADMIN Key 数据面不跳过，仍按 ApplicationChannel 过滤（D9 无特权旁路）")
    void admin_doesNotSkipFiltering() {
        // role=ADMIN + applicationId=APP_A：PermissionRouter 不因 ADMIN 跳过，仍按 ApplicationChannel 过滤
        RoutingRequest request = new RoutingRequest(MODEL_M, APP_A, USER_ID, "ADMIN", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = routerChain.filter(List.of(miCh1, miCh2), request);

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(mi -> mi.getChannelId().equals(ch1Id));
        // 关键断言：ADMIN 不能旁路到未授权的 ch2
        assertThat(result).noneMatch(mi -> mi.getChannelId().equals(ch2Id));
    }

    @Test
    @DisplayName("场景4：应用 B 授权 ch2，请求模型 M，只能路由到 ch2（对照组，验证锚点切换对称性）")
    void appB_routesOnlyToCh2() {
        RoutingRequest request = new RoutingRequest(MODEL_M, APP_B, USER_ID, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        List<ModelInstance> result = routerChain.filter(List.of(miCh1, miCh2), request);

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(mi -> mi.getChannelId().equals(ch2Id));
        assertThat(result).noneMatch(mi -> mi.getChannelId().equals(ch1Id));
    }
}
