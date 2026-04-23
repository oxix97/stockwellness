export function readOptions(name) {
  return {
    scenarios: {
      [name]: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
          { duration: '1m', target: 10 },
          { duration: '3m', target: 20 },
          { duration: '3m', target: 30 },
          { duration: '1m', target: 0 },
        ],
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
