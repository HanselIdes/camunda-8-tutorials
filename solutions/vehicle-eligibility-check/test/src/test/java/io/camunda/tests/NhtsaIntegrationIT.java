package io.camunda.tests;

import io.camunda.process.test.api.CamundaAssert;
import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.api.TestDeployment;
import io.camunda.process.test.api.testCases.TestCase;
import io.camunda.process.test.api.testCases.TestCaseRunner;
import io.camunda.process.test.api.testCases.TestCaseSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest(properties = {
    "io.camunda.process.test.connectors-enabled=true"
})
@CamundaSpringProcessTest
@TestDeployment(resources = {"Vehicle Eligibility Check.bpmn", "vehicle-eligibility.dmn"})
public class NhtsaIntegrationIT {

    @Autowired
    private TestCaseRunner testCaseRunner;

    @BeforeAll
    static void configureTimeout() {
        CamundaAssert.setAssertionTimeout(Duration.ofSeconds(60));
    }

    @ParameterizedTest
    @TestCaseSource(directory = "/integration-scenarios")
    void shouldCallNhtsaAndRoute(final TestCase testCase, final String fileName) {
        testCaseRunner.run(testCase);
    }
}
