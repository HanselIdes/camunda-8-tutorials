/**
 * Vehicle risk assessment worker
 * Job type: io.camunda.demo:vehicle-risk-assessment
 *
 * Reads vehicleApiResponse (NHTSA VIN decode HTTP connector output).
 * Writes: vehicleInfo (object), riskScore (number), eligible (boolean).
 *
 * Risk scoring:
 *   - Base score: 50
 *   - PASSENGER CAR or MPV: -20
 *   - Model year ≥ 2018: -15
 *   - Score ≤ 40 → eligible = true
 *
 * Run: node index.js
 * Env: ZEEBE_ADDRESS, ZEEBE_CLIENT_ID, ZEEBE_CLIENT_SECRET (or .env file)
 */

require('dotenv').config();
const { ZBClient } = require('zeebe-node');

const RISK_THRESHOLD = 40;

const client = new ZBClient({
  onReady: () => console.log('Worker connected'),
  onConnectionError: (err) => console.error('Connection error', err.message),
});

client.createWorker({
  taskType: 'io.camunda.demo:vehicle-risk-assessment',
  taskHandler: async (job) => {
    const { vehicleApiResponse } = job.variables;

    const results = vehicleApiResponse?.body?.Results ?? [];

    const get = (variable) =>
      results.find((r) => r.Variable === variable)?.Value ?? '';

    const make = get('Make');
    const model = get('Model');
    const year = get('Model Year');
    const vehicleType = get('Vehicle Type');

    const vehicleInfo = { make, model, year, vehicleType };

    let riskScore = 50;
    if (['PASSENGER CAR', 'MULTIPURPOSE PASSENGER VEHICLE (MPV)'].includes(vehicleType)) {
      riskScore -= 20;
    }
    if (parseInt(year, 10) >= 2018) {
      riskScore -= 15;
    }

    const eligible = riskScore <= RISK_THRESHOLD;

    console.log(`VIN assessed: ${make} ${model} ${year} | score=${riskScore} eligible=${eligible}`);

    return job.complete({ vehicleInfo, riskScore, eligible });
  },
});
