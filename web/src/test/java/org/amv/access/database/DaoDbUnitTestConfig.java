package org.amv.access.database;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.ext.mysql.MySqlMetadataHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration
public class DaoDbUnitTestConfig {

    /**
     * the connection factory - required since we have a custom H2 DB config in
     * {@link DaoDbUnitTestConfig#dbUnitDatabaseConfig()}
     */
    @Bean
    public DatabaseDataSourceConnectionFactoryBean dbUnitDatabaseConnection(DataSource dataSource) {
        DatabaseDataSourceConnectionFactoryBean bean = new DatabaseDataSourceConnectionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setDatabaseConfig(dbUnitDatabaseConfig());

        return bean;
    }

    /**
     * custom configuration since we have case-sensitive table names and requiring
     * MySQL specifics
     */
    @Bean
    public DatabaseConfigBean dbUnitDatabaseConfig() {
        DatabaseConfigBean config = new DatabaseConfigBean();
        config.setDatatypeFactory(new MySqlDataTypeFactory());
        config.setMetadataHandler(new MySqlMetadataHandler());
        config.setCaseSensitiveTableNames(true);
        config.setAllowEmptyFields(true);
        return config;
    }
}