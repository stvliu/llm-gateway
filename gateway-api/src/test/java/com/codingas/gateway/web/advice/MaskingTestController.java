package com.codingas.gateway.web.advice;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/mask-test")
public class MaskingTestController {

    @GetMapping("/string")
    public String maskString() {
        return "13812345678";
    }

    @GetMapping("/empty")
    public String empty() {
        return "";
    }

    @GetMapping("/map")
    public Map<String, String> map() {
        return Map.of("name", "test");
    }
}
