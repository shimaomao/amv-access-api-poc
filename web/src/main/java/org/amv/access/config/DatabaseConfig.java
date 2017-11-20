package org.amv.access.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties({
        DatabaseProperties.class
})
@ConditionalOnProperty("amv.access.database.url")
public class DatabaseConfig {

    @Autowired
    private DatabaseProperties databaseProperties;

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        return hikariDataSource();
    }

    private DataSource hikariDataSource() {
        return new HikariDataSource(hikariConfig());
    }

    @Bean
    public HikariConfig hikariConfig() {
        log.info("Creating HikariConfig with:\n* username: {}\n* jdbcUrl: {}", databaseProperties.getUsername(), databaseProperties.getUrl());

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(databaseProperties.getDriverClassName());
        config.setJdbcUrl(databaseProperties.getUrl());
        config.setUsername(databaseProperties.getUsername());
        config.setPassword(databaseProperties.getPassword());
        config.setMaximumPoolSize(databaseProperties.getMaximumPoolSize());
        config.setIdleTimeout(databaseProperties.getIdleTimeout());
        config.setConnectionTestQuery(databaseProperties.getConnectionTestQuery());
        config.setPoolName(databaseProperties.getPoolName());
        config.setMetricsTrackerFactory(prometheusMetricsTrackerFactory());

        config.setDataSourceProperties(dataSourceProperties());

        return config;
    }

    /**
     * data source properties for MySQL as recommended by HikariCP.
     * See https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
     *
     * @return MySQL specific properties recommended by HikariCP
     */
    private Properties dataSourceProperties() {
        Properties properties = new Properties();
        properties.setProperty("cachePrepStmts", "true");
        properties.setProperty("prepStmtCacheSize", "250");
        properties.setProperty("prepStmtCacheSqlLimit", "2048");
        properties.setProperty("useServerPrepStmts", "true");
        properties.setProperty("useLocalSessionState", "true");
        properties.setProperty("useLocalTransactionState", "true");
        properties.setProperty("rewriteBatchedStatements", "true");
        properties.setProperty("cacheResultSetMetadata", "true");
        properties.setProperty("cacheServerConfiguration", "true");
        properties.setProperty("elideSetAutoCommits", "true");
        properties.setProperty("maintainTimeStats", "false");

        return properties;
    }

    @Bean
    public PrometheusMetricsTrackerFactory prometheusMetricsTrackerFactory() {
        return new PrometheusMetricsTrackerFactory();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(jdbcTemplate());
    }
}
