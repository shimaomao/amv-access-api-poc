package org.amv.access.database;

import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import org.amv.access.config.JpaConfig;
import org.amv.access.util.OperationSystemHelper;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
        DirtiesContextTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class
})
@Transactional
@ContextConfiguration(classes = {
        EmbeddedMySqlConfig.class,
        DaoDbUnitTestConfig.class,
        JpaConfig.class
})
public class EmbeddedMySqlFlywayTest {

    @BeforeClass
    public static void skipWindowsOs() {
        Assume.assumeFalse(OperationSystemHelper.isWindows());
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void test() {
        long count = jdbcTemplate.query("SELECT count(1) as c from issuer",
                (rs, rowNum) -> rs.getLong("c")).stream()
                .mapToLong(i -> i)
                .sum();

        assertThat(count, is(0L));
    }

}
