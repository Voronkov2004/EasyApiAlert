package io.github.voronkov.easyapialert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableScheduling
@EnableConfigurationProperties({AlertProperties.class, TelegramBotProperties.class})
@ConditionalOnProperty(prefix = "easyapialert", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EasyApiAlertAutoConfiguration {

    @Bean
    public StatsCollector statsCollector() {
        return new StatsCollector();
    }

    @Bean
    public AlertStateStore alertStateStore() {
        return new AlertStateStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public AppProperties serviceNameProvider(@Value("${spring.application.name:}") String serviceName) {
        return new AppProperties(serviceName);
    }

    @Bean
    public TelegramNotifier telegramNotifier(RestClient restClient, TelegramBotProperties props) {
        return new TelegramNotifier(restClient, props);
    }

    @Bean
    public RequestMonitor requestMonitor(StatsCollector statsCollector) {
        return new RequestMonitor(statsCollector);
    }

    @Bean
    public WebMvcConfigurer easyApiAlertWebMvcConfigurer(RequestMonitor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    @Bean
    public AlertScheduler alertScheduler(StatsCollector statsCollector, TelegramNotifier notifier, AlertStateStore stateStore, AlertProperties props, AppProperties appProperties) {
        return new AlertScheduler(statsCollector, notifier, stateStore, props, appProperties);
    }
}