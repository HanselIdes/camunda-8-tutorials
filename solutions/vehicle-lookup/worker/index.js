'use strict';

require('dotenv').config();
const { ZBClient } = require('zeebe-node');

const zbc = new ZBClient();

// Risk factors by vehicle type keyword (additive)
const TYPE_RISK = {
  TRUCK: 30,
  BUS: 35,
  MOTORCYCLE: 25,
  'LOW SPEED VEHICLE': 20,
  'INCOMPLETE VEHICLE': 15,
};

/**
 * Extract a single field value from the NHTSA Results array.
 * @param {Array<{Variable: string, Value: string}>} results
 * @param {string} variable
 */
function field(results, variable) {
  return results?.find((r) => r.Variable === variable)?.Value ?? null;
}

/**
 * Compute a 0-100 risk score from decoded NHTSA vehicle data.
 * Lower = safer. Threshold for eligibility is set by the process (default 70).
 */
function computeRiskScore(vehicleType, year) {
  let score = 20; // base score

  // Vehicle type uplift
  const typeUpper = (vehicleType ?? '').toUpperCase();
  for (const [keyword, uplift] of Object.entries(TYPE_RISK)) {
    if (typeUpper.includes(keyword)) {
      score += uplift;
      break;
    }
  }

  // Age uplift: +3 per year beyond 15 years old, capped at +30
  const age = new Date().getFullYear() - parseInt(year ?? 0, 10);
  if (age > 15) score += Math.min((age - 15) * 3, 30);

  return Math.min(score, 100);
}

const worker = zbc.createWorker({
  taskType: 'io.camunda.demo:vehicle-risk-assessment',
  taskHandler: async (job) => {
    const { vehicleApiResponse, vehicle, riskThreshold = 70 } = job.variables;

    const results = vehicleApiResponse?.body?.Results ?? [];

    const make        = field(results, 'Make');
    const model       = field(results, 'Model');
    const year        = field(results, 'Model Year');
    const vehicleType = field(results, 'Vehicle Type');

    const riskScore = computeRiskScore(vehicleType, year);
    const eligible  = riskScore <= Number(riskThreshold);

    return job.complete({
      vehicle: {
        ...vehicle,
        make,
        model,
        year,
        vehicleType,
        riskScore,
        eligible,
      },
    });
  },
});

console.log('Vehicle risk assessment worker running (job type: io.camunda.demo:vehicle-risk-assessment)');
