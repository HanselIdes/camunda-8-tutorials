package io.camunda.demo.vehiclelookup;

import io.camunda.process.test.api.CamundaSpringProcessTest;
import io.camunda.zeebe.client.ZeebeClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Process tests for vehicle-lookup.bpmn.
 *
 * Both service tasks are completed by inline mock workers:
 *   - io.camunda:http-json:1        — simulates the NHTSA API response
 *   - io.camunda.demo:vehicle-risk-assessment — simulates the Node.js worker
 *
 * No external network calls. No worker process required.
 * Run: mvn test
 */
@CamundaSpringProcessTest
class VehicleLookupProcessTest {

    @Autowired
    ZeebeClient client;

    // ── Fixture data ──────────────────────────────────────────────────────────

    /** Realistic NHTSA response body for a 2021 Honda Civic. */
    static final Map<String, Object> NHTSA_HONDA_CIVIC = Map.of(
        "body", Map.of(
            "Results", List.of(
                Map.of("Variable", "Make",         "Value", "HONDA"),
                Map.of("Variable", "Model",        "Value", "CIVIC"),
                Map.of("Variable", "Model Year",   "Value", "2021"),
                Map.of("Variable", "Vehicle Type", "Value", "PASSENGER CAR")
            )
        ),
        "status", 200
    );

    /** Realistic NHTSA response body for a 2005 Ford F-350 (heavy truck). */
    static final Map<String, Object> NHTSA_FORD_F350 = Map.of(
        "body", Map.of(
            "Results", List.of(
                Map.of("Variable", "Make",         "Value", "FORD"),
                Map.of("Variable", "Model",        "Value", "F-350"),
                Map.of("Variable", "Model Year",   "Value", "2005"),
                Map.of("Variable", "Vehicle Type", "Value", "TRUCK")
            )
        ),
        "status", 200
    );

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void passengerCar_recentYear_isEligible() {
        // Register mock workers
        try (var httpWorker = mockHttpConnector(NHTSA_HONDA_CIVIC);
             var riskWorker = mockRiskWorker("HONDA", "CIVIC", "2021", "PASSENGER CAR", 20, true)) {

            var result = client.newCreateInstanceCommand()
                .bpmnProcessId("vehicle-lookup")
                .latestVersion()
                .variables(Map.of("vehicle", Map.of("vin", "1HGBH41JXMN109186")))
                .withResult()
                .send()
                .join();

            @SuppressWarnings("unchecked")
            var vehicle = (Map<String, Object>) result.getVariable("vehicle");

            assertThat(vehicle).isNotNull();
            assertThat(vehicle.get("eligible")).isEqualTo(true);
            assertThat(vehicle.get("make")).isEqualTo("HONDA");
            assertThat(vehicle.get("model")).isEqualTo("CIVIC");
            assertThat(vehicle.get("riskScore")).isEqualTo(20);
        }
    }

    @Test
    void heavyTruck_oldYear_isNotEligible() {
        try (var httpWorker = mockHttpConnector(NHTSA_FORD_F350);
             var riskWorker = mockRiskWorker("FORD", "F-350", "2005", "TRUCK", 85, false)) {

            var result = client.newCreateInstanceCommand()
                .bpmnProcessId("vehicle-lookup")
                .latestVersion()
                .variables(Map.of("vehicle", Map.of("vin", "1FTBF2B67FEB78450")))
                .withResult()
                .send()
                .join();

            @SuppressWarnings("unchecked")
            var vehicle = (Map<String, Object>) result.getVariable("vehicle");

            assertThat(vehicle.get("eligible")).isEqualTo(false);
            assertThat(vehicle.get("make")).isEqualTo("FORD");
            assertThat(vehicle.get("riskScore")).isEqualTo(85);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Mock the NHTSA HTTP connector — returns a canned response body. */
    private AutoCloseable mockHttpConnector(Map<String, Object> nhtsaResponse) {
        var worker = client.newWorker()
            .jobType("io.camunda:http-json:1")
            .handler((c, job) -> c.newCompleteCommand(job)
                .variables(Map.of("vehicleApiResponse", nhtsaResponse))
                .send())
            .open();
        return worker;
    }

    /** Mock the risk assessment worker — returns an enriched vehicle object. */
    private AutoCloseable mockRiskWorker(
            String make, String model, String year, String vehicleType,
            int riskScore, boolean eligible) {
        var worker = client.newWorker()
            .jobType("io.camunda.demo:vehicle-risk-assessment")
            .handler((c, job) -> {
                @SuppressWarnings("unchecked")
                var vin = ((Map<String, Object>) job.getVariablesAsMap().get("vehicle")).get("vin");
                c.newCompleteCommand(job)
                    .variables(Map.of("vehicle", Map.of(
                        "vin",         vin,
                        "make",        make,
                        "model",       model,
                        "year",        year,
                        "vehicleType", vehicleType,
                        "riskScore",   riskScore,
                        "eligible",    eligible
                    )))
                    .send();
            })
            .open();
        return worker;
    }
}
