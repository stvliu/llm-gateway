package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内建用户加载器
 *
 * <p>从 classpath:data/builtin/users.json 加载内建用户（如 admin）。</p>
 */
@Slf4j
@Component
public class BuiltinUserLoader implements DataLoader {

    private static final String USERS_JSON = "data/builtin/users.json";

    private final UserGateway userGateway;
    private final UserCreator userCreator;

    public BuiltinUserLoader(UserGateway userGateway,
                             UserCreator userCreator) {
        this.userGateway = userGateway;
        this.userCreator = userCreator;
    }

    // ========== JSON DTO record ==========

    private record BuiltinUserData(String username, String email, String role, String password) {}

    // ========== DataLoader ==========

    @Override
    public InitPhase getPhase() {
        return InitPhase.BUILTIN_USER;
    }

    @Override
    public void load(DataLoadContext context) {
        List<BuiltinUserData> users = JsonResourceReader.readList(USERS_JSON, new TypeReference<>() {});
        for (BuiltinUserData user : users) {
            ensureBuiltinUser(user);
        }
    }

    // ========== Internal methods ==========

    private void ensureBuiltinUser(BuiltinUserData data) {
        if (userGateway.findByUsername(data.username()).isPresent()) {
            return;
        }
        userCreator.create(data.username(), data.email(), data.password(), data.role(), true);
        log.info("  内建用户 '{}' 已创建", data.username());
    }
}
