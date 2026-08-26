/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.proxy.experience;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.ModelInstanceRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ModelExperienceManager 单元测试
 *
 * <p>覆盖模型列表查询与流式聊天体验两条主链路：
 * <ul>
 *   <li>{@link #getModelsByProviderId}：空渠道/空实例/空模型/过滤与映射分支</li>
 *   <li>{@link #chatStream}：无效请求、临时配置（OpenAI/Anthropic）、已保存配置
 *       （按凭证 ID / 默认凭证 / 各类错误）、SSE 回调 onChunk/onComplete/onError 全部分支</li>
 * </ul>
 * </p>
 *
 * <p>测试技巧：SseEmitter 在未关联 HTTP 响应时（handler 为 null）会把 send 的事件
 * 缓冲到 earlySendAttempts，随后通过反射调用包级私有方法
 * {@code ResponseBodyEmitter#initialize(Handler)} 注入动态代理 Handler，即可
 * 确定性捕获实际发送的 SSE 事件，无需 MockMvc 与真实 HTTP 容器。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelExperienceManager 单元测试")
class ModelExperienceManagerTest {

    @Mock
    private UpstreamClientRegistry upstreamClientRegistry;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ModelInstanceRepository modelInstanceRepository;

    @Mock
    private ChannelCredentialRepository channelCredentialRepository;

    @Mock
    private ModelRepository modelRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ModelExperienceManager service;

    @BeforeEach
    void setUp() {
        service = new ModelExperienceManager(upstreamClientRegistry, providerRepository, channelRepository,
                modelInstanceRepository, channelCredentialRepository, modelRepository, objectMapper);
    }

    // ------------------------------------------------------------------
    // 测试辅助：SseEmitter Handler 动态代理（捕获实际发送事件）
    // ------------------------------------------------------------------

    /** 记录 handler 上发生的 send/complete/completeWithError 调用 */
    private static final class RecordingHandler {
        private final List<Object> sentEvents = new ArrayList<>();
        private final AtomicInteger completeCount = new AtomicInteger();
        private final List<Throwable> completeWithErrors = new ArrayList<>();

        /** 创建动态代理（ResponseBodyEmitter.Handler 为包级私有接口，无法直接引用） */
        Object proxy() {
            try {
                Class<?> handlerClass = Class.forName(
                        "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$Handler");
                return Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class<?>[]{handlerClass},
                        (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "send" -> {
                                    // 注意：args[0] 是 emitter 内部 earlySendAttempts 的实时引用，
                                    // initialize 冲刷后会 clear 原集合，必须先快照再保存。
                                    if (args != null && args.length == 1 && args[0] instanceof Set<?> s) {
                                        sentEvents.add(new java.util.HashSet<>(s));
                                    }
                                    return null;
                                }
                                case "complete" -> {
                                    completeCount.incrementAndGet();
                                    return null;
                                }
                                case "completeWithError" -> {
                                    completeWithErrors.add((Throwable) args[0]);
                                    return null;
                                }
                                default -> {
                                    return null;
                                }
                            }
                        });
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("无法加载 ResponseBodyEmitter.Handler", e);
            }
        }
    }

    /** 通过反射调用包级私有 initialize(Handler)，注入 Handler 并冲刷早发缓冲 */
    private static void initialize(SseEmitter emitter, Object handler) throws Exception {
        Method m = ResponseBodyEmitter.class.getDeclaredMethod("initialize", handler.getClass().getInterfaces()[0]);
        m.setAccessible(true);
        m.invoke(emitter, handler);
    }

    /**
     * 从发送的事件集合中提取数据对象。
     *
     * <p>SseEventBuilder 会把事件渲染成多个 DataWithMediaType：文本前缀
     * （如 "event:CONTENT\ndata:"）、实际数据对象、尾部换行。此处取
     * 非 String 的负载对象（即业务数据本体）。</p>
     */
    @SuppressWarnings("unchecked")
    private static Object payload(Object sentEvent) {
        Set<ResponseBodyEmitter.DataWithMediaType> set = (Set<ResponseBodyEmitter.DataWithMediaType>) sentEvent;
        for (ResponseBodyEmitter.DataWithMediaType dwmt : set) {
            if (!(dwmt.getData() instanceof String)) {
                return dwmt.getData();
            }
        }
        return null;
    }

    /** 轮询等待条件成立（异步任务在虚拟线程执行） */
    private static void awaitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("等待异步任务超时");
            }
            Thread.sleep(10);
        }
    }

    /** 启动 chatStream，返回 emitter 并捕获传入 UpstreamClient 的流式回调 */
    private SseEmitter startStream(ExperienceChatCommand request,
                                   UpstreamClient<ProtocolRequest> client,
                                   ArgumentCaptor<StreamCallback> captor) {
        SseEmitter emitter = service.chatStream(request);
        verify(client, timeout(5000)).chatStream(any(ProtocolRequest.class), captor.capture());
        return emitter;
    }

    /** 启动 chatStream，捕获回调并注入 RecordingHandler，返回回调 */
    private StreamCallback startStreamWithRecorder(ExperienceChatCommand request,
                                                   UpstreamClient<ProtocolRequest> client,
                                                   RecordingHandler recorder) throws Exception {
        ArgumentCaptor<StreamCallback> captor = ArgumentCaptor.forClass(StreamCallback.class);
        SseEmitter emitter = startStream(request, client, captor);
        initialize(emitter, recorder.proxy());
        return captor.getValue();
    }

    private ExperienceChatCommand openAiTempRequest() {
        return new ExperienceChatCommand(
                "gpt-4o", "openai",
                List.of(Map.of("role", "user", "content", "hello")),
                null, null, null, null, null, "sk-test-key", null, false);
    }

    // ------------------------------------------------------------------
    // getModelsByProviderId
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getModelsByProviderId 模型列表查询")
    class GetModelsTests {

        @Test
        @DisplayName("渠道为空时返回空列表")
        void noChannels_returnsEmpty() {
            when(channelRepository.findByProviderId(1L)).thenReturn(List.of());

            assertThat(service.getModelsByProviderId(1L)).isEmpty();
            verify(modelInstanceRepository, never()).findActiveByChannelId(any());
        }

        @Test
        @DisplayName("渠道存在但无模型实例时返回空列表")
        void channelsButNoInstances_returnsEmpty() {
            Channel channel = new Channel();
            channel.setId(10L);
            when(channelRepository.findByProviderId(1L)).thenReturn(List.of(channel));
            when(modelInstanceRepository.findActiveByChannelId(10L)).thenReturn(List.of());

            assertThat(service.getModelsByProviderId(1L)).isEmpty();
        }

        @Test
        @DisplayName("有实例但模型表无对应记录时返回空列表")
        void instancesButNoModels_returnsEmpty() {
            Channel channel = new Channel();
            channel.setId(10L);
            when(channelRepository.findByProviderId(1L)).thenReturn(List.of(channel));

            ModelInstance instance = new ModelInstance();
            instance.setModelId(100L);
            when(modelInstanceRepository.findActiveByChannelId(10L)).thenReturn(List.of(instance));
            when(modelRepository.findByIds(List.of(100L))).thenReturn(List.of());

            assertThat(service.getModelsByProviderId(1L)).isEmpty();
        }

        @Test
        @DisplayName("多渠道实例按模型去重、过滤不可用模型、实体原样返回")
        void modelsFound_filtersAndMaps() {
            Channel ch1 = new Channel();
            ch1.setId(10L);
            Channel ch2 = new Channel();
            ch2.setId(11L);
            when(channelRepository.findByProviderId(1L)).thenReturn(List.of(ch1, ch2));

            ModelInstance i1 = new ModelInstance();
            i1.setModelId(100L);
            ModelInstance i2 = new ModelInstance();
            i2.setModelId(100L); // 与 i1 同模型，distinct 去重
            ModelInstance i3 = new ModelInstance();
            i3.setModelId(200L);
            when(modelInstanceRepository.findActiveByChannelId(10L)).thenReturn(List.of(i1));
            when(modelInstanceRepository.findActiveByChannelId(11L)).thenReturn(List.of(i2, i3));

            Model m100 = new Model();
            m100.setId(100L);
            m100.setModelName("gpt-4o");
            m100.setDisplayName("GPT-4o");
            Model m200 = new Model();
            m200.setId(200L);
            m200.setModelName("claude-3-5"); // displayName 为 null → 回退 modelName
            Model m300 = new Model();
            m300.setId(300L);
            m300.setModelName("retired-model");
            m300.setDeprecatedAt(Instant.now()); // 不可用 → 过滤
            when(modelRepository.findByIds(List.of(100L, 200L))).thenReturn(List.of(m100, m200, m300));

            List<Model> result = service.getModelsByProviderId(1L);

            assertThat(result).hasSize(2)
                    .extracting(Model::getId)
                    .containsExactly(100L, 200L);
            assertThat(result.get(0).getDisplayName()).isEqualTo("GPT-4o");
            // displayName 为 null 的模型实体原样返回（缺省回退由 web 层 DTO 负责）
            assertThat(result.get(1).getDisplayName()).isNull();
            assertThat(result.get(1).getModelName()).isEqualTo("claude-3-5");
        }
    }

    // ------------------------------------------------------------------
    // chatStream：无效请求
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("chatStream 无效请求")
    class ChatStreamInvalidTests {

        @Test
        @DisplayName("无效请求时发送 ERROR 事件并完成")
        void invalidRequest_sendsErrorAndCompletes() throws Exception {
            // 临时配置缺 apiKey → isValid() 为 false
            ExperienceChatCommand request = new ExperienceChatCommand(
                    "gpt-4o", "openai",
                    List.of(Map.of("role", "user", "content", "hello")),
                    null, null, null, null, null, null, null, false);

            SseEmitter emitter = service.chatStream(request);
            RecordingHandler recorder = new RecordingHandler();
            initialize(emitter, recorder.proxy());

            awaitUntil(() -> !recorder.sentEvents.isEmpty());
            assertThat(recorder.sentEvents).hasSize(1);
            Object data = payload(recorder.sentEvents.get(0));
            assertThat(data).isInstanceOf(ExperienceChatEvent.ErrorData.class);
            assertThat(((ExperienceChatEvent.ErrorData) data).message()).contains("无效的请求");
            assertThat(recorder.completeCount.get()).isEqualTo(1);
        }
    }

    // ------------------------------------------------------------------
    // chatStream：临时配置（OpenAI）与回调分支
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("chatStream 临时配置（OpenAI）与回调分支")
    class ChatStreamTempConfigTests {

        @Test
        @DisplayName("成功流式分发：构建 OpenAI 请求并回调 onChunk 发送 CONTENT")
        void tempConfig_openai_capturesCallbackAndSendsContent() throws Exception {
            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-test-key"), eq(60)))
                    .thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(openAiTempRequest(), client, recorder);

            ArgumentCaptor<ProtocolRequest> reqCaptor = ArgumentCaptor.forClass(ProtocolRequest.class);
            verify(client).chatStream(reqCaptor.capture(), any(StreamCallback.class));
            OpenAIChatRequest built = (OpenAIChatRequest) reqCaptor.getValue();
            assertThat(built.getModel()).isEqualTo("gpt-4o");
            assertThat(built.getMessages()).hasSize(1);
            assertThat(built.getStream()).isTrue();

            callback.onChunk("{\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");
            assertThat(recorder.sentEvents).hasSize(1);
            assertThat(payload(recorder.sentEvents.get(0)))
                    .isEqualTo(new ExperienceChatEvent.ContentData("hello"));
        }

        @Test
        @DisplayName("onChunk 解析 reasoning_content 并发送 CONTENT")
        void onChunk_reasoningContent_sendsContent() throws Exception {
            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-test-key"), eq(60)))
                    .thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(openAiTempRequest(), client, recorder);

            callback.onChunk("{\"choices\":[{\"delta\":{\"reasoning_content\":\"thinking\"}}]}");
            assertThat(payload(recorder.sentEvents.get(0)))
                    .isEqualTo(new ExperienceChatEvent.ContentData("thinking"));
        }

        @Test
        @DisplayName("onChunk 空内容/非法 JSON 不发送事件")
        void onChunk_emptyOrInvalid_doesNotSend() throws Exception {
            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-test-key"), eq(60)))
                    .thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(openAiTempRequest(), client, recorder);

            callback.onChunk("{\"choices\":[{\"delta\":{\"content\":\"\"}}]}");
            callback.onChunk("{\"choices\":[]}");
            callback.onChunk("not-a-json");

            assertThat(recorder.sentEvents).isEmpty();
        }

        @Test
        @DisplayName("onComplete 发送 USAGE + DONE 并完成")
        void onComplete_sendsUsageAndDone() throws Exception {
            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-test-key"), eq(60)))
                    .thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(openAiTempRequest(), client, recorder);

            callback.onChunk("{\"choices\":[{\"delta\":{\"content\":\"hello\"}}]}");
            callback.onComplete();

            assertThat(recorder.sentEvents).hasSize(3); // CONTENT + USAGE + DONE
            assertThat(payload(recorder.sentEvents.get(1))).isEqualTo(new ExperienceChatEvent.UsageData(0, 1));
            assertThat(recorder.completeCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("onError 发送 ERROR 事件并完成")
        void onError_sendsErrorAndCompletes() throws Exception {
            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-test-key"), eq(60)))
                    .thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(openAiTempRequest(), client, recorder);

            callback.onError(new RuntimeException("上游异常"));
            assertThat(payload(recorder.sentEvents.get(0))).isInstanceOf(ExperienceChatEvent.ErrorData.class);
            assertThat(((ExperienceChatEvent.ErrorData) payload(recorder.sentEvents.get(0))).message())
                    .isEqualTo("上游异常");
            assertThat(recorder.completeCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("onError 发送失败时 completeWithError 传递原始异常")
        void onError_sendFails_completesWithError() throws Exception {
            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-test-key"), eq(60)))
                    .thenReturn(client);

            ArgumentCaptor<StreamCallback> captor = ArgumentCaptor.forClass(StreamCallback.class);
            SseEmitter emitter = startStream(openAiTempRequest(), client, captor);
            StreamCallback callback = captor.getValue();

            // 自定义 Handler：send 抛 IOException 模拟底层输出失败
            Class<?> handlerClass = Class.forName(
                    "org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$Handler");
            List<Throwable> errors = new ArrayList<>();
            Object handler = Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class<?>[]{handlerClass},
                    (proxy, method, args) -> {
                        if ("send".equals(method.getName())) {
                            throw new java.io.IOException("output closed");
                        }
                        if ("completeWithError".equals(method.getName())) {
                            errors.add((Throwable) args[0]);
                        }
                        return null;
                    });
            initialize(emitter, handler);

            RuntimeException upstream = new RuntimeException("上游异常");
            callback.onError(upstream);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)).isSameAs(upstream);
        }
    }

    // ------------------------------------------------------------------
    // chatStream：已保存配置
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("chatStream 已保存配置")
    class ChatStreamSavedConfigTests {

        private ExperienceChatCommand savedConfigRequest(Long channelId, Long credentialId) {
            return new ExperienceChatCommand(
                    "gpt-4o", null,
                    List.of(Map.of("role", "user", "content", "hello")),
                    null, null, null, channelId, credentialId, null, null, true);
        }

        private Channel channel(Long id) {
            Channel channel = new Channel();
            channel.setId(id);
            return channel;
        }

        private ChannelCredential credential(Long id, Long channelId, String key) {
            ChannelCredential credential = new ChannelCredential();
            credential.setId(id);
            credential.setChannelId(channelId);
            credential.setApiKeyPlain(key);
            return credential;
        }

        @Test
        @DisplayName("按凭证 ID 解析配置：使用凭证的 API Key 调用上游")
        void savedConfig_credentialById_usesCredentialKey() throws Exception {
            when(channelRepository.findById(10L)).thenReturn(Optional.of(channel(10L)));
            when(channelCredentialRepository.findById(20L)).thenReturn(Optional.of(credential(20L, 10L, "sk-saved-1")));

            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-saved-1"), eq(60))).thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(savedConfigRequest(10L, 20L), client, recorder);

            ArgumentCaptor<ProtocolRequest> reqCaptor = ArgumentCaptor.forClass(ProtocolRequest.class);
            verify(client).chatStream(reqCaptor.capture(), any(StreamCallback.class));
            assertThat(reqCaptor.getValue()).isInstanceOf(OpenAIChatRequest.class);
            assertThat(recorder.sentEvents).isEmpty(); // 未触发任何错误
            assertThat(callback).isNotNull();
        }

        @Test
        @DisplayName("未指定凭证 ID 时使用渠道默认凭证")
        void savedConfig_defaultCredential_usesDefaultKey() throws Exception {
            when(channelRepository.findById(10L)).thenReturn(Optional.of(channel(10L)));
            when(channelCredentialRepository.findDefaultByChannelId(10L))
                    .thenReturn(Optional.of(credential(30L, 10L, "sk-default-1")));

            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("openai"), eq(""), eq("sk-default-1"), eq(60))).thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(savedConfigRequest(10L, null), client, recorder);

            ArgumentCaptor<ProtocolRequest> reqCaptor = ArgumentCaptor.forClass(ProtocolRequest.class);
            verify(client).chatStream(reqCaptor.capture(), any(StreamCallback.class));
            assertThat(reqCaptor.getValue()).isInstanceOf(OpenAIChatRequest.class);
            assertThat(recorder.sentEvents).isEmpty();
        }

        @Test
        @DisplayName("渠道不存在时发送 ERROR 事件")
        void savedConfig_channelNotFound_sendsError() throws Exception {
            when(channelRepository.findById(99L)).thenReturn(Optional.empty());

            SseEmitter emitter = service.chatStream(savedConfigRequest(99L, null));
            RecordingHandler recorder = new RecordingHandler();
            initialize(emitter, recorder.proxy());

            awaitUntil(() -> !recorder.sentEvents.isEmpty());
            assertThat(((ExperienceChatEvent.ErrorData) payload(recorder.sentEvents.get(0))).message())
                    .contains("渠道不存在");
        }

        @Test
        @DisplayName("凭证不属于渠道时发送 ERROR 事件")
        void savedConfig_credentialMismatch_sendsError() throws Exception {
            when(channelRepository.findById(10L)).thenReturn(Optional.of(channel(10L)));
            when(channelCredentialRepository.findById(20L)).thenReturn(Optional.of(credential(20L, 999L, "sk-x")));

            SseEmitter emitter = service.chatStream(savedConfigRequest(10L, 20L));
            RecordingHandler recorder = new RecordingHandler();
            initialize(emitter, recorder.proxy());

            awaitUntil(() -> !recorder.sentEvents.isEmpty());
            assertThat(((ExperienceChatEvent.ErrorData) payload(recorder.sentEvents.get(0))).message())
                    .contains("凭证不属于该渠道");
        }

        @Test
        @DisplayName("默认凭证缺失时发送 ERROR 事件")
        void savedConfig_defaultCredentialMissing_sendsError() throws Exception {
            when(channelRepository.findById(10L)).thenReturn(Optional.of(channel(10L)));
            when(channelCredentialRepository.findDefaultByChannelId(10L)).thenReturn(Optional.empty());

            SseEmitter emitter = service.chatStream(savedConfigRequest(10L, null));
            RecordingHandler recorder = new RecordingHandler();
            initialize(emitter, recorder.proxy());

            awaitUntil(() -> !recorder.sentEvents.isEmpty());
            assertThat(((ExperienceChatEvent.ErrorData) payload(recorder.sentEvents.get(0))).message())
                    .contains("默认凭证");
        }

        @Test
        @DisplayName("凭证不存在时发送 ERROR 事件")
        void savedConfig_credentialNotFound_sendsError() throws Exception {
            when(channelRepository.findById(10L)).thenReturn(Optional.of(channel(10L)));
            when(channelCredentialRepository.findById(20L)).thenReturn(Optional.empty());

            SseEmitter emitter = service.chatStream(savedConfigRequest(10L, 20L));
            RecordingHandler recorder = new RecordingHandler();
            initialize(emitter, recorder.proxy());

            awaitUntil(() -> !recorder.sentEvents.isEmpty());
            assertThat(((ExperienceChatEvent.ErrorData) payload(recorder.sentEvents.get(0))).message())
                    .contains("凭证不存在");
        }
    }

    // ------------------------------------------------------------------
    // chatStream：Anthropic 协议
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("chatStream 临时配置（Anthropic）")
    class ChatStreamAnthropicTests {

        @Test
        @DisplayName("anthropic 协议构建 AnthropicMessagesRequest，maxTokens 缺省 1024")
        void tempConfig_anthropic_buildsAnthropicRequest() throws Exception {
            ExperienceChatCommand request = new ExperienceChatCommand(
                    "claude-3-5", "anthropic",
                    List.of(Map.of("role", "user", "content", "hello")),
                    0.7, null, null, null, null, "sk-ant-key", null, false);

            UpstreamClient<ProtocolRequest> client = org.mockito.Mockito.mock(UpstreamClient.class);
            when(upstreamClientRegistry.getClient(eq("anthropic"), eq(""), eq("sk-ant-key"), eq(60))).thenReturn(client);
            RecordingHandler recorder = new RecordingHandler();

            StreamCallback callback = startStreamWithRecorder(request, client, recorder);

            ArgumentCaptor<ProtocolRequest> reqCaptor = ArgumentCaptor.forClass(ProtocolRequest.class);
            verify(client).chatStream(reqCaptor.capture(), any(StreamCallback.class));
            AnthropicMessagesRequest built = (AnthropicMessagesRequest) reqCaptor.getValue();
            assertThat(built.getModel()).isEqualTo("claude-3-5");
            assertThat(built.getMessages()).hasSize(1);
            assertThat(built.getMessages().get(0).getRole()).isEqualTo("user");
            assertThat(built.getMaxTokens()).isEqualTo(1024);
            assertThat(built.getTemperature()).isEqualTo(0.7);
            assertThat(built.isStream()).isTrue();
            assertThat(recorder.sentEvents).isEmpty();
        }
    }

    // ------------------------------------------------------------------
    // shutdown
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("shutdown 优雅关闭")
    class ShutdownTests {

        @Test
        @DisplayName("shutdown 关闭执行器且不抛异常")
        void shutdown_terminatesCleanly() {
            ModelExperienceManager fresh = new ModelExperienceManager(upstreamClientRegistry, providerRepository,
                    channelRepository, modelInstanceRepository, channelCredentialRepository, modelRepository, objectMapper);
            fresh.shutdown();
            assertThat(fresh).isNotNull();
        }
    }
}
