package com.dascribs.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ApplicationTest {

    @Test
    void contextLoads(ApplicationContext context) {
        // Verify that the application context loads successfully
        assertThat(context).isNotNull();
    }

    @Test
    void mainMethodStartsApplication() {
        // Test that the main method starts without errors
        Application.main(new String[]{});
    }
}