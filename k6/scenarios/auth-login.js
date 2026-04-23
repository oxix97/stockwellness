import { readOptions, quickOptions } from '../helpers/options.js';
import http from 'k6/http';
import { check } from 'k6';
import { optionalEnv, requireEnv } from '../helpers/env.js';
import { jsonHeaders } from '../helpers/headers.js';

const BASE_URL = requireEnv('BASE_URL');
const LOGIN_EMAIL = optionalEnv('LOGIN_EMAIL', 'k6.live@example.com');
const LOGIN_NICKNAME = optionalEnv('LOGIN_NICKNAME', 'k6-live');
const LOGIN_TYPE = optionalEnv('LOGIN_TYPE', 'NONE');

export const options = (__ENV.K6_MODE === 'quick' ? quickOptions : readOptions)('auth_login');

export default function () {
  const payload = JSON.stringify({
    email: LOGIN_EMAIL,
    nickname: LOGIN_NICKNAME,
    loginType: LOGIN_TYPE,
  });

  const res = http.post(`${BASE_URL}/api/v1/auth/login`, payload, {
    headers: jsonHeaders(),
    tags: { api: 'auth_login', type: 'write' },
  });

  check(res, {
    'login status is 200': (r) => r.status === 200,
    'login has token': (r) => {
      const body = r.json();
      return body && body.data && body.data.accessToken;
    },
  });

  const body = res.json();
  console.log(JSON.stringify(body.data));
}
