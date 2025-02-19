package de.swm.lhm.geoportal.gateway.search.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class ElasticSearchQueryCreator {
    private static final String SERIALIZATION_FAILURE = "Failed to serialize query components to JSON";
    private static final Pattern HOUSE_NUMBER_OR_ZIP_PATTERN = Pattern.compile("\\d+\\w?");
    private final ObjectMapper objectMapper;

    String createGeoDataQuery(String searchString){
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode match = objectMapper.createObjectNode();

        match.put("geoDataValue", searchString);
        query.set("match", match);
        root.set("query", query);

        try {
            return objectMapper.writer().writeValueAsString(root);
        } catch(JsonProcessingException e) {
            throw new RuntimeException(SERIALIZATION_FAILURE, e);
        }
    }

    String createAddressQuery(String queryString, int resultAmount) {
        Matcher matcher = HOUSE_NUMBER_OR_ZIP_PATTERN.matcher(queryString);
        String houseNumberQuery = matcher.find() ? matcher.group() : null;
        String streetNameQuery = queryString;

        if (houseNumberQuery != null) {
            streetNameQuery = streetNameQuery.replace(houseNumberQuery, "").trim();
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("size", resultAmount);

        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode bool = objectMapper.createObjectNode();
        ArrayNode must = objectMapper.createArrayNode();
        ObjectNode match = objectMapper.createObjectNode();

        ObjectNode streetNameParams = objectMapper.createObjectNode();
        streetNameParams.put("query", streetNameQuery);
        streetNameParams.put("operator", "and");
        streetNameParams.put("fuzziness", "auto");

        match.set("streetName", streetNameParams);
        must.add(objectMapper.createObjectNode().set("match", match));

        if (houseNumberQuery != null) {
            ObjectNode prefix = objectMapper.createObjectNode();
            prefix.put("houseNumber", houseNumberQuery);
            must.add(objectMapper.createObjectNode().set("prefix", prefix));
        }

        bool.set("must", must);
        query.set("bool", bool);
        root.set("query", query);

        try {
            return objectMapper.writer().writeValueAsString(root);
        } catch(JsonProcessingException e) {
            throw new RuntimeException(SERIALIZATION_FAILURE, e);
        }
    }
}
