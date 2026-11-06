package org.stockwellness.adapter.in.web.actual;

import java.util.Set;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.stockwellness.domain.actual.ActualAccountRules;

/** Strict parsing is local to actual input; simulation coercion and DTO compatibility are unchanged. */
final class ActualAccountJson {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private ActualAccountJson() { }
    static Create create(String json) {
        JsonNode node = object(json, Set.of("name", "baseCurrency", "brokerLabel"));
        String broker = null;
        if (node.has("brokerLabel") && !node.get("brokerLabel").isNull()) broker = text(node, "brokerLabel");
        return new Create(text(node, "name"), text(node, "baseCurrency"), broker);
    }
    static String rename(String json) { return text(object(json, Set.of("name")), "name"); }
    private static JsonNode object(String json, Set<String> allowed) {
        if (json == null) throw ActualAccountRules.invalidInput();
        try {
            JsonNode node = MAPPER.readTree(json);
            if (node == null || !node.isObject()) throw ActualAccountRules.invalidInput();
            var fields = node.fieldNames();
            while (fields.hasNext()) if (!allowed.contains(fields.next())) throw ActualAccountRules.invalidInput();
            return node;
        } catch (JsonProcessingException ignored) { throw ActualAccountRules.invalidInput(); }
    }
    private static String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || !value.isTextual()) throw ActualAccountRules.invalidInput();
        return value.textValue();
    }
    record Create(String name, String baseCurrency, String brokerLabel) { }
}
