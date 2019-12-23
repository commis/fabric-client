package com.energy.justsdk.util;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * @author Bryan
 * @date 2019-12-09
 */
public class JsonTools {

    @Data
    static class ParamValue {

        private String value;
    }

    /**
     * 将参数转化为字符串数组，格式：{"a","b","c"}或{'a','b','c'}
     *
     * @param paramDataStr 参数格式JSON字符串
     * @return 参数数组
     */
    public static String[] fromStringArray(String paramDataStr) {
        if (!StringUtils.isEmpty(paramDataStr)) {
            String srcDataStr = paramDataStr.trim();
            String arrayStr = srcDataStr.substring(1, srcDataStr.length() - 1);
            String[] values = arrayStr.split(",");
            for (int i = 0; i < values.length; i++) {
                String tempValue = values[i];
                values[i] = tempValue
                    .replace("\"", "")
                    .replace("'", "")
                    .trim();
            }
            return values;
        }
        return null;
    }
}
