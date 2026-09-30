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
package com.codingas.gateway.protocol.transport;

/**
 * 出站会话开始上下文
 *
 * <p>由各协议 {@link UpstreamClient} 在发起上游 HTTP 请求前构造并传入
 * {@link SessionStartHook}，承载会话级元数据（不含 Trace ID——传输契约
 * 无该参数，如需全链路关联走请求契约扩展）。</p>
 *
 * <p><b>PII 注意</b>：{@link #requestBody} 为完整序列化请求体，含用户对话内容
 * （PII）；消费方负责脱敏，不得直接写入日志或外部系统。</p>
 *
 * @param provider     上游协议标识（"openai"/"anthropic"/"gemini"）
 * @param model        出站模型名（调谐后）
 * @param endpointUrl  上游端点地址
 * @param requestBody  已序列化请求体（含 PII，消费方须脱敏；供输入 token 估算）
 * @param requestBytes 请求体字节数
 * @param stream       是否流式调用
 */
public record SessionStartContext(String provider, String model, String endpointUrl,
                                  String requestBody, long requestBytes, boolean stream) {
}
