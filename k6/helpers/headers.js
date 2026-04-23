import { requireEnv } from './env.js';

export function jsonHeaders() {
  return {
    'Content-Type': 'application/json',
  };
}

export function authHeaders() {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${requireEnv('ACCESS_TOKEN')}`,
  };
}
