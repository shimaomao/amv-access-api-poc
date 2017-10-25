package org.amv.access.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.annotations.VisibleForTesting;
import com.netflix.hystrix.*;
import feign.*;
import feign.hystrix.HystrixFeign;
import feign.hystrix.SetterFactory;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkArgument;
import static com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
import static java.util.concurrent.TimeUnit.SECONDS;

public final class Clients {
    @VisibleForTesting
    public static final ObjectMapper defaultObjectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Clients() {
        throw new UnsupportedOperationException();
    }

    public static HystrixFeign.Builder simpleFeignBuilder() {
        return HystrixFeign.builder()
                .setterFactory(new DefaultSetterFactory())
                .logger(new Slf4jLogger())
                .logLevel(Logger.Level.FULL)
                .retryer(new Retryer.Default())
                .contract(new Contract.Default())
                .client(new OkHttpClient())
                .options(new Request.Options())
                .encoder(new JacksonEncoder(defaultObjectMapper))
                .decoder(new JacksonDecoder(defaultObjectMapper));
    }

    public static Feign simpleFeign() {
        return simpleFeignBuilder().build();
    }

    public static DeviceCertClient simpleDeviceCertClient(String baseUrl) {
        checkArgument(baseUrl != null);

        return simpleFeign().newInstance(deviceCertClientTarget(baseUrl));
    }
    public static DeviceCertClient deviceCertClient(Feign feign, String baseUrl) {
        checkArgument(feign != null);
        checkArgument(baseUrl != null);

        return feign.newInstance(deviceCertClientTarget(baseUrl));
    }
    private static Target<DeviceCertClient> deviceCertClientTarget(String baseUrl) {
        checkArgument(StringUtils.isNotBlank(baseUrl));

        return hardCodedTarget(DeviceCertClient.class, baseUrl);
    }

    public static AccessCertClient simpleAccessCertClient(String baseUrl) {
        checkArgument(baseUrl != null);

        return simpleFeign().newInstance(accessCertClientTarget(baseUrl));
    }

    public static AccessCertClient accessCertClient(Feign feign, String baseUrl) {
        checkArgument(feign != null);
        checkArgument(baseUrl != null);

        return feign.newInstance(accessCertClientTarget(baseUrl));
    }

    private static Target<AccessCertClient> accessCertClientTarget(String baseUrl) {
        checkArgument(StringUtils.isNotBlank(baseUrl));

        return hardCodedTarget(AccessCertClient.class, baseUrl);
    }

    private static <T> Target<T> hardCodedTarget(Class<T> clazz, String baseUrl) {
        checkArgument(clazz != null);
        checkArgument(StringUtils.isNotBlank(baseUrl));

        return new Target.HardCodedTarget<T>(clazz, baseUrl);
    }

    private static final class DefaultSetterFactory implements SetterFactory {
        private static int DEFAULT_THREAD_POOL_SIZE = 10;
        private static int DEFAULT_TIMEOUT_IN_MS = (int) SECONDS.toMillis(30);

        @Override
        public HystrixCommand.Setter create(Target<?> target, Method method) {
            String groupKey = target.name();
            String commandKey = Feign.configKey(target.type(), method);

            HystrixThreadPoolProperties.Setter threadPoolProperties = HystrixThreadPoolProperties.Setter()
                    .withCoreSize(DEFAULT_THREAD_POOL_SIZE);

            HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter()
                    .withFallbackEnabled(false)
                    .withExecutionTimeoutEnabled(true)
                    .withExecutionTimeoutInMilliseconds(DEFAULT_TIMEOUT_IN_MS)
                    .withExecutionIsolationStrategy(THREAD)
                    .withExecutionIsolationThreadInterruptOnTimeout(true);

            return HystrixCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                    .andThreadPoolPropertiesDefaults(threadPoolProperties)
                    .andCommandPropertiesDefaults(commandProperties);
        }
    }
}
