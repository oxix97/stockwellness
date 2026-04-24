import { check } from 'k6';

export function assertApiSuccess(name, res) {
  check(res, {
    [`${name} status is 200`]: (r) => r.status === 200,
    [`${name} has data`]: (r) => {
      const body = r.json();
      return body && Object.prototype.hasOwnProperty.call(body, 'data');
    },
  });
}
