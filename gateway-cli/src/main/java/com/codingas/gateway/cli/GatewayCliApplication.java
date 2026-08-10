/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway CLI 应用程序入口
 *
 * @author Claude
 */
@SpringBootApplication
public class GatewayCliApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayCliApplication.class, args);
    }
}
