package org.stockwellness.domain.stock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Converter
public class LeadingStocksConverter implements AttributeConverter<List<LeadingStock>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<LeadingStock> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting LeadingStocks to JSON", e);
            throw new RuntimeException("JSON writing error", e);
        }
    }

    @Override
    public List<LeadingStock> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<LeadingStock>>() {});
        } catch (IOException e) {
            log.error("Error converting JSON to LeadingStocks", e);
            throw new RuntimeException("JSON reading error", e);
        }
    }
}