package org.amv.access.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.net.URISyntaxException;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories({
        "org.amv.access.model"
})
public class JpaConfig {

    private final DataSource dataSource;

    @Autowired
    public JpaConfig(DataSource dataSource) {
        this.dataSource = requireNonNull(dataSource);
    }

    private Properties jpaProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", org.hibernate.dialect.MySQL5InnoDBDialect.class.getName());
        //properties.setProperty("hibernate.show_sql", String.valueOf(true));
        //properties.setProperty("hibernate.generate_statistics", String.valueOf(true));
        //properties.setProperty("hibernate.connection.isolation", String.valueOf(Connection.TRANSACTION_READ_COMMITTED));

        return properties;
    }

    /**
     * Entity manager
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        // JPA settings
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        // vendorAdapter.setGenerateDdl(true);
        // vendorAdapter.setShowSql(true);
        vendorAdapter.setDatabase(Database.MYSQL);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.amv.access.model");
        factory.setDataSource(dataSource);
        factory.setJpaProperties(jpaProperties());
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Transaction Management: Using JPA for transactions
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        EntityManagerFactory entityManagerFactory = entityManagerFactory().getObject();
        JpaTransactionManager txManager = new JpaTransactionManager(entityManagerFactory);
        return txManager;
    }

    /**
     * Make sure instances of HibernateException are translated to Spring's
     * DataAccessException.
     */
    @Bean
    public HibernateExceptionTranslator exceptionTranslation() {
        return new HibernateExceptionTranslator();
    }
}
