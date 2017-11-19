package org.amv.access;

import com.google.common.base.Charsets;
import io.github.robwin.markup.builder.MarkupLanguage;
import org.amv.access.swagger.SwaggerConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.staticdocs.Swagger2MarkupResultHandler;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test which generates the static swagger documentation.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = {
                SwaggerTestApplication.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = "test")
public class SwaggerRestApiDocumentation {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private List<Docket> dockets;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .build();
    }

    @Test
    public void createSpringfoxSwaggerJson() throws Exception {
        assertThat(dockets, is(notNullValue()));
        assertThat(dockets, hasSize(greaterThan(0)));

        dockets.stream()
                .map(Docket::getGroupName)
                .forEach(groupName -> {
                    String urlTemplate = String.format("/v2/api-docs?group=%s", groupName);
                    String outputDirectory = String.format("build/docs/swagger/generated/%s", groupName);

                    try {
                        MvcResult mvcResult = this.mockMvc.perform(get(urlTemplate).accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andReturn();

                        MockHttpServletResponse response = mvcResult.getResponse();
                        String swaggerJson = prettifyJson(response.getContentAsString());

                        Files.createDirectories(Paths.get(outputDirectory));
                        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputDirectory, "swagger.json"), Charsets.UTF_8)) {
                            writer.write(swaggerJson);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void convertSwaggerToAsciiDoc() throws Exception {
        assertThat(dockets, is(notNullValue()));
        assertThat(dockets, hasSize(greaterThan(0)));

        dockets.stream()
                .map(Docket::getGroupName)
                .forEach(groupName -> {
                    String urlTemplate = String.format("/v2/api-docs?group=%s", groupName);
                    String outputDirectory = String.format("build/docs/swagger/generated/%s", groupName);

                    try {
                        this.mockMvc.perform(get(urlTemplate).accept(MediaType.APPLICATION_JSON))
                                .andDo(print())
                                .andDo(Swagger2MarkupResultHandler.outputDirectory(outputDirectory)
                                        .withMarkupLanguage(MarkupLanguage.ASCIIDOC)
                                        .build())
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private String prettifyJson(String json) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine scriptEngine = manager.getEngineByName("JavaScript");
            scriptEngine.put("jsonString", json);
            scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 2)");

            return (String) scriptEngine.get("result");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}