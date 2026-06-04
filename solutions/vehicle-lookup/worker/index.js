/**
 * Vehicle risk assessment worker
 * Job type: io.camunda.demo:vehicle-risk-assessment
 *
 * Input: vehicleApiResponse (from NHTSA VIN lookup connector)
 * Output: vehicleInfo, vehicleScore, eligible
 *
 * Risk scoring logic:
 *   Base score: 50
 *   Passenger car or MPV: -20 points
 *   Model year >= 2018: -15 points
 *   Eligible if score <= 40
 */

require('dotenv').config();
const { ZBClient } = require('zeebe-node');

const client = new ZBClient();
client.createWorker({
  taskType: 'io.camunda.demo:vehicle-risk-assessment',
  taskHandler: async (job) => {
    // Extract NHTSA response results (array of {Variable, Value} pairs)
    const results = job.variables.vehicleApiResponse?.body?.Results ?? [];
    const get = (v) => results.find((r) => r.Variable === v)?.Value ?? '';

    // Parse vehicle details from NHTSA response
    const make = get('Make');
    const model = get('Model');
    const year = get('Model Year');
    const vehicleType = get('Vehicle Type');
    const vehicleInfo = { make, model, year, vehicleType };

    // Calculate risk score
    let vehicleScore = 50;
    if (['PASSENGER CAR', 'MULTIPURPOSE PASSENGER VEHICLE (MPV)'].includes(vehicleType)) {
      vehicleScore -= 20;
    }
    if (parseInt(year, 10) >= 2018) {
      vehicleScore -= 15;
    }

    const eligible = vehicleScore <= 40;
    console.log(`${make} ${model} ${year} | score=${vehicleScore} eligible=${eligible}`);

    // Return output variables to BPMN
    return job.complete({ vehicleInfo, vehicleScore, eligible });
  },
});
