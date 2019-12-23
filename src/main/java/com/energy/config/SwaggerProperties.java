package com.energy.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Bryan
 * @date 2019-11-11
 */
@Data
@Configuration
@ConfigurationProperties("swagger")
public class SwaggerProperties {

    /**
     * 是否开启swagger
     **/
    private boolean enabled = false;

    /**
     * 标题
     **/
    private String title = "APPLICATION.NAME";

    /**
     * 描述
     **/
    private String description = "";

    /**
     * 版本
     **/
    private String version = "1.0";

    /**
     * swagger会解析的包路径
     **/
    private String basePackage = "";

    /**
     * swagger会解析的url规则
     **/
    private List<String> basePath = new ArrayList<>();

    /**
     * 在basePath基础上需要排除的url规则
     **/
    private List<String> excludePath = new ArrayList<>();

    /**
     * 全局参数配置
     **/
    private List<GlobalOperationParameter> globalOperationParameters;

    @Data
    @NoArgsConstructor
    public static class GlobalOperationParameter {

        /**
         * 参数名
         **/
        private String name;
        /**
         * 描述信息
         **/
        private String description;
        /**
         * 指定参数类型
         **/
        private String modelRef;
        /**
         * 参数放在哪个地方:header,query,path,body.form
         **/
        private String parameterType;
        /**
         * 参数是否必须传
         **/
        private String required;
    }
}
