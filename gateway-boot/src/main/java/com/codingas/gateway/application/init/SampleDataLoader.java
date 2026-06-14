package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.entity.UserApiKey;
import com.codingas.gateway.domain.iam.gateway.UserApiKeyGateway;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.entity.TeamChannel;
import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import com.codingas.gateway.infrastructure.config.GatewayProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 示例数据加载器
 *
 * <p>从 classpath:data/sample/ 加载示例团队、用户和 API Key 数据。
 * 受 {@code gateway.init.demo-data-enabled} 开关控制，仅在开发/测试环境启用。</p>
 */
@Slf4j
@Component
public class SampleDataLoader implements DataLoader {

    private static final String TEAMS_JSON = "data/sample/teams.json";
    private static final String USERS_JSON = "data/sample/users.json";
    private static final String API_KEYS_JSON = "data/sample/apikeys.json";

    private final UserGateway userGateway;
    private final TeamGateway teamGateway;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;
    private final UserApiKeyGateway userApiKeyGateway;
    private final UserCreator userCreator;
    private final GatewayProperties gatewayProperties;

    public SampleDataLoader(UserGateway userGateway,
                            TeamGateway teamGateway,
                            UserTeamGateway userTeamGateway,
                            TeamChannelGateway teamChannelGateway,
                            UserApiKeyGateway userApiKeyGateway,
                            UserCreator userCreator,
                            GatewayProperties gatewayProperties) {
        this.userGateway = userGateway;
        this.teamGateway = teamGateway;
        this.userTeamGateway = userTeamGateway;
        this.teamChannelGateway = teamChannelGateway;
        this.userApiKeyGateway = userApiKeyGateway;
        this.userCreator = userCreator;
        this.gatewayProperties = gatewayProperties;
    }

    // ========== JSON DTO records ==========

    private record SampleTeamData(String name, String description, List<String> channels) {}

    private record SampleUserData(
            String username, String email, String password, String role,
            List<TeamAssignmentData> teamAssignments
    ) {}

    private record TeamAssignmentData(String team, String role) {}

    private record SampleApiKeyData(String username, String name) {}

    // ========== DataLoader ==========

    @Override
    public InitPhase getPhase() {
        return InitPhase.SAMPLE_DATA;
    }

    @Override
    public boolean isEnabled(GatewayProperties properties) {
        return properties.getInit().isDemoDataEnabled();
    }

    @Override
    public void load(DataLoadContext context) {
        // 幂等守卫
        if (isLoaded()) {
            log.info("示例数据已存在，跳过初始化");
            return;
        }

        log.info("开始初始化示例数据...");

        // 读取上游数据
        Map<String, Channel> channelMap = context.getRequired(DataLoadContext.ChannelIndex.class).getMap();
        Map<String, Team> teamMap = loadTeams(channelMap);
        context.set(DataLoadContext.TeamIndex.class, new DataLoadContext.TeamIndex(teamMap));

        Map<String, User> userMap = loadUsers(teamMap);
        context.set(DataLoadContext.UserIndex.class, new DataLoadContext.UserIndex(userMap));

        loadApiKeys(userMap);

        logInitializationSummary(channelMap.size(), teamMap.size(), userMap.size());
    }

    // ========== Internal methods ==========

    private boolean isLoaded() {
        return userGateway.findByUsername("test1").isPresent();
    }

    private Map<String, Team> loadTeams(Map<String, Channel> channelMap) {
        log.info("加载示例团队数据...");
        Map<String, Team> teamMap = new HashMap<>();

        List<SampleTeamData> teams = JsonResourceReader.readList(TEAMS_JSON, new TypeReference<>() {});
        for (SampleTeamData teamData : teams) {
            Team saved = createTeam(teamData.name(), teamData.description());
            teamMap.put(teamData.name(), saved);

            if (teamData.channels() != null) {
                for (String channelKey : teamData.channels()) {
                    Channel channel = channelMap.get(channelKey);
                    if (channel != null) {
                        teamChannelGateway.save(new TeamChannel(saved.getId(), channel.getId()));
                    } else {
                        log.warn("  渠道 key '{}' 未找到，跳过", channelKey);
                    }
                }
            }
        }

        log.info("  共加载 {} 个团队", teamMap.size());
        return teamMap;
    }

    private Map<String, User> loadUsers(Map<String, Team> teamMap) {
        log.info("加载示例用户数据...");
        Map<String, User> userMap = new HashMap<>();

        List<SampleUserData> users = JsonResourceReader.readList(USERS_JSON, new TypeReference<>() {});
        for (SampleUserData userData : users) {
            User saved = userGateway.findByUsername(userData.username())
                    .orElseGet(() -> userCreator.create(
                            userData.username(), userData.email(),
                            userData.password(), userData.role(), false));
            userMap.put(userData.username(), saved);

            if (userData.teamAssignments() != null) {
                for (TeamAssignmentData assignment : userData.teamAssignments()) {
                    Team team = teamMap.get(assignment.team());
                    if (team != null && !userTeamGateway.isMember(saved.getId(), team.getId())) {
                        UserTeam ut = new UserTeam();
                        ut.setUserId(saved.getId());
                        ut.setTeamId(team.getId());
                        ut.setRole(assignment.role());
                        userTeamGateway.save(ut);
                    }
                }
            }
        }

        log.info("  共加载 {} 个用户", userMap.size());
        return userMap;
    }

    private void loadApiKeys(Map<String, User> userMap) {
        log.info("加载示例 API Key 数据...");
        int count = 0;

        List<SampleApiKeyData> keys = JsonResourceReader.readList(API_KEYS_JSON, new TypeReference<>() {});
        for (SampleApiKeyData key : keys) {
            User user = userMap.get(key.username());
            if (user != null) {
                createUserApiKey(user.getId(), key.name());
                count++;
            } else {
                log.warn("  用户 '{}' 未找到，跳过 API Key", key.username());
            }
        }

        log.info("  共加载 {} 个 API Key", count);
    }

    private Team createTeam(String name, String description) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setState("ACTIVE");
        return teamGateway.save(team);
    }

    private void createUserApiKey(Long userId, String name) {
        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUserId(userId);
        userApiKey.setName(name);
        String uuid8 = UUID.randomUUID().toString().substring(0, 8);
        userApiKey.setKeyPlain("sk-" + uuid8 + "-" + name.toLowerCase().replace(" ", "-"));
        userApiKeyGateway.save(userApiKey);
    }

    private void logInitializationSummary(int channelCount, int teamCount, int userCount) {
        log.info("========================================");
        log.info("示例数据初始化完成!");
        log.info("========================================");
        log.info("  渠道: {}", channelCount);
        log.info("  团队: {}", teamCount);
        log.info("  用户: {}", userCount);
        log.info("========================================");
        log.info("Test accounts:");
        log.info("  Admin - Username: admin, Password: admin, Role: ADMIN");
        log.info("  Demo users - Username: test1 ~ test10, Password: same as username");
        log.info("  Volcengine users - hermes-agent / claude-code / bi-report");
        log.info("========================================");
    }
}
