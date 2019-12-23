package com.energy.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.util.List;
import java.util.Map;
import org.springframework.util.ObjectUtils;

/**
 * @author Bryan
 */
public class JsonUtil {

    public static <T> String toString(T object) {
        return JSON.toJSONString(object);
    }

    public static <T> String toString(List<T> data) {
        if (!ObjectUtils.isEmpty(data)) {
            return JSON.toJSONString(data);
        }
        return null;
    }

    public static <T> T fromJson(String jsonStr, Class<T> clazz) {
        return JSON.parseObject(jsonStr, clazz);
    }

    public static <T> List<T> fromJsonArray(String jsonStr, Class<T> clazz) {
        return JSON.parseArray(jsonStr, clazz);
    }

    public static <T> Map<String, String> toCollect(String jsonStr) {
        Map<String, String> map = JSON.parseObject(
            jsonStr, new TypeReference<Map<String, String>>() {
            });
        return map;
    }
}
