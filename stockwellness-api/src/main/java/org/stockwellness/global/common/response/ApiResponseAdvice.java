package org.stockwellness.global.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 컨트롤러에서 반환되는 ApiResponse 객체를 가로채어
 * ApiResponse 내부에 설정된 status 값을 실제 HTTP 응답 상태 코드로 설정합니다.
 */
@RestControllerAdvice(basePackages = "org.stockwellness")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 반환 타입이 ApiResponse이거나 ApiResponse를 상속/구현한 타입인 경우 지원
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // Body가 ApiResponse인 경우 내부의 status 값을 HTTP Response Status로 설정
        if (body instanceof ApiResponse<?> apiResponse) {
            response.setStatusCode(HttpStatusCode.valueOf(apiResponse.status()));
        }

        return body;
    }
}
