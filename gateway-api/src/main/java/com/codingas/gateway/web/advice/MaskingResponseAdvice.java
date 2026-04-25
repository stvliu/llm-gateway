package com.codingas.gateway.web.advice;

import com.codingas.gateway.core.security.masking.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@ControllerAdvice
public class MaskingResponseAdvice implements ResponseBodyAdvice<Object> {

    private final SensitiveDataMasker sensitiveDataMasker;

    public MaskingResponseAdvice(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getParameterType().equals(String.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof String text)) {
            return body;
        }
        if (text.isEmpty()) {
            return text;
        }
        String masked = sensitiveDataMasker.mask(text);
        log.debug("Sensitive data masked for {}: {} chars -> {} chars",
                request.getURI().getPath(), text.length(), masked.length());
        return masked;
    }
}
