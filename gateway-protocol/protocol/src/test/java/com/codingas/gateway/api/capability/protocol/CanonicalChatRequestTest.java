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
package com.codingas.gateway.api.capability.protocol;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalChatRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void builderConstructsAndSerializes() throws Exception {
        CanonicalMessage msg = CanonicalMessage.builder()
                .role("user").content("你好").build();
        CanonicalChatRequest req = CanonicalChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(msg))
                .stream(true)
                .build();

        String json = mapper.writeValueAsString(req);

        assertThat(req.getModel()).isEqualTo("gpt-4o");
        assertThat(req.getMessages().get(0).getContent()).isEqualTo("你好");
        assertThat(json).contains("\"model\"");
    }
}
