package com.codingas.gateway.domain.team.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 团队未找到异常
 */
public class TeamNotFoundException extends GatewayException {

    public TeamNotFoundException(Long teamId) {
        super("TEAM_NOT_FOUND", "Team not found: id=" + teamId);
    }

    public TeamNotFoundException(String message) {
        super("TEAM_NOT_FOUND", message);
    }
}
