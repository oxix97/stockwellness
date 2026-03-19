package org.stockwellness.global.logging;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.Set;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

/**
 * 로그 직렬화 전용 ObjectMapper 팩토리.
 * {@link Masked} 어노테이션이나 민감 키워드가 포함된 필드를 자동으로 마스킹합니다.
 */
public final class MaskingObjectMapper {

    private static final String MASKED_VALUE = "********";
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "pwd", "accesstoken", "refreshtoken",
            "token", "secret", "authorization"
    );

    private MaskingObjectMapper() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    /**
     * 민감 필드를 마스킹 처리하는 전용 ObjectMapper를 생성합니다.
     */
    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(WRITE_DATES_AS_TIMESTAMPS)
                .disable(FAIL_ON_EMPTY_BEANS);

        SimpleModule maskingModule = new SimpleModule("MaskingModule");
        maskingModule.setSerializerModifier(new SensitiveFieldModifier());
        mapper.registerModule(maskingModule);

        return mapper;
    }

    /**
     * 직렬화 시 민감 필드를 감지하여 {@link MaskingPropertyWriter}로 교체하는 Modifier.
     */
    private static class SensitiveFieldModifier extends BeanSerializerModifier {

        @Override
        public List<BeanPropertyWriter> changeProperties(
                SerializationConfig config,
                BeanDescription beanDesc,
                List<BeanPropertyWriter> beanProperties) {

            for (int i = 0; i < beanProperties.size(); i++) {
                BeanPropertyWriter writer = beanProperties.get(i);

                if (shouldMask(writer)) {
                    beanProperties.set(i, new MaskingPropertyWriter(writer));
                }
            }
            return beanProperties;
        }

        private boolean shouldMask(BeanPropertyWriter writer) {
            if (writer.getAnnotation(Masked.class) != null) {
                return true;
            }
            String fieldName = writer.getName().toLowerCase();
            return SENSITIVE_KEYWORDS.stream().anyMatch(fieldName::contains);
        }
    }

    /**
     * 필드 값을 "********"로 치환하여 직렬화하는 커스텀 Writer.
     */
    private static class MaskingPropertyWriter extends BeanPropertyWriter {

        MaskingPropertyWriter(BeanPropertyWriter base) {
            super(base);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov)
                throws Exception {
            Object value = get(bean);
            if (value != null) {
                gen.writeStringField(getName(), MASKED_VALUE);
            } else {
                gen.writeNullField(getName());
            }
        }
    }
}
