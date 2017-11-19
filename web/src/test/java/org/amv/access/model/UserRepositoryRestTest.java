package org.amv.access.model;

import org.amv.access.AmvAccessApplication;
import org.amv.access.config.SqliteTestDatabaseConfig;
import org.amv.access.demo.DemoService;
import org.amv.access.demo.DemoUser;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AmvAccessApplication.class, SqliteTestDatabaseConfig.class}
)
public class UserRepositoryRestTest {
    @Autowired
    private DemoService demoService;

    @Autowired
    private TestRestTemplate restTemplate;

    private BasicJsonTester json = new BasicJsonTester(getClass());

    @Test
    public void getUsers() {
        DemoUser user = demoService.getOrCreateDemoUser();

        ResponseEntity<String> responseEntity = restTemplate
                //.withBasicAuth(user.getName(), user.getPassword())
                .getForEntity("/model-user", String.class);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        Assertions.assertThat(json.from(responseEntity.getBody()))
                .extractingJsonPathNumberValue("page.totalElements")
                .isEqualTo(1);
    }
}