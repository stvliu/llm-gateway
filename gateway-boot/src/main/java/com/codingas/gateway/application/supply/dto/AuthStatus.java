package com.codingas.gateway.application.supply.dto;

/**
 * 单 Key 认证状态
 *
 * <ul>
 *   <li>PASS：连通性测试通过</li>
 *   <li>FAIL：测试失败（认证错误、网络错误等）</li>
 *   <li>TIMEOUT：测试超时（单 Key 5s 超时）</li>
 * </ul>
 */
public enum AuthStatus {
    PASS,
    FAIL,
    TIMEOUT
}
