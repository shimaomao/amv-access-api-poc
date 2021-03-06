package org.amv.access.database;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.TransactionDbUnitTestExecutionListener;
import org.amv.access.AmvAccessApplication;
import org.amv.access.util.OperationSystemHelper;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                AmvAccessApplication.class,
                EmbeddedMySqlConfig.class
        }
)
@TestExecutionListeners({
        DirtiesContextTestExecutionListener.class,
        DependencyInjectionTestExecutionListener.class,
        TransactionDbUnitTestExecutionListener.class,
        DbUnitTestExecutionListener.class
})
@Transactional
@ActiveProfiles("embedded-mysql-application-it")
public class EmbeddedMySqlApplicationIT {

    @BeforeClass
    public static void skipWindowsOs() {
        Assume.assumeFalse(OperationSystemHelper.isWindows());
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    public void itShouldVerifyThatAtLeastOneIssuerExists() {
        long count = jdbcTemplate.query("SELECT count(1) as c from issuer",
                (rs, rowNum) -> rs.getLong("c")).stream()
                .mapToLong(i -> i)
                .sum();

        assertThat(count, is(greaterThanOrEqualTo(1L)));
    }

}
