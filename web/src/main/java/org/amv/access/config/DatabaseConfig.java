package org.amv.access.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
public class DatabaseConfig {

    @Primary
    @Bean(destroyMethod = "close")
    public DataSource hikariDataSource() {
        return new HikariDataSource(hikariConfig());
    }

    @Bean
    public HikariConfig hikariConfig() {
        HikariConfig config = new HikariConfig();
        config.setPoolName("amv-access-sqlite-connection-pool");
        config.setDriverClassName(org.sqlite.JDBC.class.getName());
        config.setJdbcUrl("jdbc:sqlite:amv-access.db?journal_mode=wal");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        config.setMetricsTrackerFactory(prometheusMetricsTrackerFactory());

        return config;
    }

    public DataSource dataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName(org.sqlite.JDBC.class.getName());
        dataSourceBuilder.url("jdbc:sqlite:amv-access.db");
        return dataSourceBuilder.build();
    }

    @Bean
    public PrometheusMetricsTrackerFactory prometheusMetricsTrackerFactory() {
        return new PrometheusMetricsTrackerFactory();
    }
}
