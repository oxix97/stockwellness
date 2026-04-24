export function requireEnv(name) {
  const value = __ENV[name];
  if (!value || String(value).trim() === '') {
    throw new Error(`Missing required env: ${name}`);
  }
  return value;
}

export function optionalEnv(name, fallback) {
  const value = __ENV[name];
  return value && String(value).trim() !== '' ? value : fallback;
}
