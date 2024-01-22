package com.walter.starry.security.base.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author: walter.tan
 * @datetime: 2023/8/27 19:29
 */
@UtilityClass
public class JsonUtil {

    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T toBean(String json, Class<T> clz){
        if(StringUtils.isBlank(json)){
            return null;
        }

        try {
            return objectMapper.readValue(json, clz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> toList(String json, TypeReference<List<T>> typeReference){
        if(StringUtils.isBlank(json)){
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(Object object){
        if(Objects.isNull(object)){
            return null;
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
