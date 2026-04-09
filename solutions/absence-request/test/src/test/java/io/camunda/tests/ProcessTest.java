package io.camunda.tests;

import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.process.test.api.TestDeployment;
import io.camunda.process.test.api.testCases.TestCase;
import io.camunda.process.test.api.testCases.TestCaseRunner;
import io.camunda.process.test.api.testCases.TestCaseSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs JSON process tests from src/test/resources/scenarios/ against an embedded Zeebe engine.
 *
 * Uses the CPT 8.9 instruction-based format (TestCaseRunner). Each instruction explicitly
 * targets a specific element ID, so routing bugs cause test failures.
 *
 * Run with: mvn test   (Docker must be running)
 */
@SpringBootTest
@CamundaSpringProcessTest
@TestDeployment(resources = {
    "Absence Request.bpmn",
    "Form_05b6cf95-4c81-452b-bebb-9d4f516e8465.form",
    "Form_21505344-3f9e-443c-b488-4cc88422ec3e.form",
    "Form_145b7eab-a1fa-46b9-899d-8b51c3eb26d2.form",
    "Form_1a49c114-43ae-491a-8b1d-8be9956e8496.form",
    "Form_39c4a16f-4acd-4181-bd4f-abab9b13f7aa.form",
    "Form_05218fe1-7e95-4430-8387-d74c6dc7b58b.form"
})
public class ProcessTest {

    @Autowired
    private TestCaseRunner testCaseRunner;

    @ParameterizedTest
    @TestCaseSource(directory = "/scenarios")
    void shouldPass(final TestCase testCase, final String fileName) {
        testCaseRunner.run(testCase);
    }
}
