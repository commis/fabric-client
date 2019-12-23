package com.energy.config;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.configuration.Swagger2DocumentationConfiguration;

/**
 * @author Bryan
 * @date 2019-11-11
 */
@Configuration
@Import({
    Swagger2DocumentationConfiguration.class
})
public class SwaggerAutoConfiguration implements BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Bean
    @ConditionalOnMissingBean
    public SwaggerProperties swaggerProperties() {
        return new SwaggerProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "swagger.enabled", matchIfMissing = true)
    public List<Docket> createRestApi(SwaggerProperties swaggerProperties) {
        ConfigurableBeanFactory configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
        List<Docket> docketList = new LinkedList<>();

        ApiInfo apiInfo = new ApiInfoBuilder()
            .title(swaggerProperties.getTitle())
            .description(swaggerProperties.getDescription())
            .version(swaggerProperties.getVersion())
            .build();

        // base-path处理
        List<Predicate<String>> basePath = getPredicates(
            swaggerProperties.getBasePath(), "/**");

        // exclude-path处理
        List<Predicate<String>> excludePath = getPredicates(
            swaggerProperties.getExcludePath(), "/error");

        Docket docketForBuilder = new Docket(DocumentationType.SWAGGER_2)
            .enable(swaggerProperties.isEnabled())
            .apiInfo(apiInfo)
            .globalOperationParameters(buildGlobalOperationParametersFromSwaggerProperties(
                swaggerProperties.getGlobalOperationParameters()));

        Docket docket = docketForBuilder.select()
            .apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()))
            .paths(Predicates.and(
                Predicates.not(Predicates.or(excludePath)),
                Predicates.or(basePath)
                )
            ).build();

        configurableBeanFactory.registerSingleton("defaultDocket", docket);
        docketList.add(docket);
        return docketList;
    }

    private List<Predicate<String>> getPredicates(List<String> pathList, String defaultPath) {
        List<Predicate<String>> predicateList = new ArrayList<>();
        if (pathList.isEmpty()) {
            predicateList.add(PathSelectors.ant(defaultPath));
        }
        for (String path : pathList) {
            predicateList.add(PathSelectors.ant(path));
        }
        return predicateList;
    }

    private List<Parameter> buildGlobalOperationParametersFromSwaggerProperties(
        List<SwaggerProperties.GlobalOperationParameter> globalOperationParameters) {
        List<Parameter> parameters = newArrayList();

        if (Objects.isNull(globalOperationParameters)) {
            return parameters;
        }
        for (SwaggerProperties.GlobalOperationParameter globalOperationParameter : globalOperationParameters) {
            parameters.add(new ParameterBuilder()
                .name(globalOperationParameter.getName())
                .description(globalOperationParameter.getDescription())
                .modelRef(new ModelRef(globalOperationParameter.getModelRef()))
                .parameterType(globalOperationParameter.getParameterType())
                .required(Boolean.parseBoolean(globalOperationParameter.getRequired()))
                .build());
        }
        return parameters;
    }
}