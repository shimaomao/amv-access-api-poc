package org.amv.access.database;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.wix.mysql.EmbeddedMysql;
import com.wix.mysql.config.MysqldConfig;
import com.wix.mysql.config.SchemaConfig;
import com.wix.mysql.distribution.Version;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.wix.mysql.EmbeddedMysql.anEmbeddedMysql;
import static com.wix.mysql.config.Charset.UTF8;
import static com.wix.mysql.config.MysqldConfig.aMysqldConfig;
import static com.wix.mysql.config.SchemaConfig.aSchemaConfig;

@TestConfiguration
@EnableTransactionManagement
public class EmbeddedMySqlConfig {
    private static final Version embeddedMySqlServerVersion = Version.v5_5_40;
    private static final String SCHEMA_NAME = "access_api";

    @Bean(destroyMethod = "stop")
    public EmbeddedMysql embeddedMysql() {
        EmbeddedMysql mysqld = anEmbeddedMysql(mysqldConfig())
                .addSchema(schemaConfig())
                .start();

        return mysqld;
    }

    @Bean
    public SchemaConfig schemaConfig() {
        return aSchemaConfig(SCHEMA_NAME)
                .build();
    }

    @Bean
    public MysqldConfig mysqldConfig() {
        try {
            return aMysqldConfig(embeddedMySqlServerVersion)
                    .withFreePort()
                    .withUser("differentUser", "anotherPassword")
                    .withCharset(UTF8)
                    .withTimeZone(TimeZone.getDefault())
                    .withTimeout(10, TimeUnit.SECONDS)
                    .withServerVariable("max_connect_errors", 1)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }

    @Primary
    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        final EmbeddedMysql embeddedMysql = embeddedMysql(); // make sure embeddedMySql is started.

        Map<String, String> params = ImmutableMap.<String, String>builder()
                .put("profileSQL", String.valueOf(false))
                .put("generateSimpleParameterMetadata", String.valueOf(true))
                .build();

        final String url = String.format("jdbc:mysql://localhost:%d/%s?%s",
                embeddedMysql.getConfig().getPort(),
                SCHEMA_NAME,
                Joiner.on("&").withKeyValueSeparator("=").join(params));

        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.username(embeddedMysql.getConfig().getUsername());
        dataSourceBuilder.password(embeddedMysql.getConfig().getPassword());
        dataSourceBuilder.driverClassName(com.mysql.jdbc.Driver.class.getName());
        dataSourceBuilder.url(url);
        return dataSourceBuilder.build();
    }

    @PostConstruct
    void startSchemaMigration() {
        final Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource());
        flyway.setLocations("classpath:/db/migration/mysql");

        flyway.migrate();
    }
}