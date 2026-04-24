export function readOptions(name) {
  const mode = __ENV.K6_MODE || 'standard';
  const stagesByMode = {
    short: [
      { duration: '30s', target: 10 },
      { duration: '1m', target: 20 },
      { duration: '30s', target: 0 },
    ],
    standard: [
      { duration: '1m', target: 10 },
      { duration: '2m', target: 20 },
      { duration: '1m', target: 30 },
      { duration: '1m', target: 0 },
    ],
    stress: [
      { duration: '2m', target: 20 },
      { duration: '4m', target: 40 },
      { duration: '2m', target: 60 },
      { duration: '2m', target: 0 },
    ],
  };

  return {
    scenarios: {
      [name]: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: stagesByMode[mode] || stagesByMode.standard,
        gracefulRampDown: '30s',
      },
    },
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<800', 'p(99)<1500'],
      checks: ['rate>0.99'],
    },
  };
}

export function heavyReadOptions(name) {
  return {
    scenarios: {
      [name]: {
        executor: 'constant-vus',
        vus: 3,
        duration: '3m',
      },
    },
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<5000', 'p(99)<8000'],
      checks: ['rate>0.99'],
    },
  };
}

export function quickOptions(name) {
  return {
    scenarios: {
      [name]: {
        executor: 'constant-vus',
        vus: 1,
        duration: '10s',
      },
    },
    thresholds: {
      http_req_failed: ['rate<0.1'],
      checks: ['rate>0.9'],
    },
  };
}
