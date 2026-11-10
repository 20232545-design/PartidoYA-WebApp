import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { parseEnv } from 'node:util';

// "development" genera un environment local (ignorado por git) para `npm start`.
// Sin argumento genera environment.prod.ts para `npm run build` (Render).
const isDevelopment = process.argv[2] === 'development';

if (isDevelopment && !process.env.GOOGLE_MAPS_API_KEY) {
  // Reutiliza el .env local del proyecto (ignorado por git), pero solo toma la clave
  // de Maps: ese archivo también contiene secretos del backend que no deben llegar al frontend.
  const envFile = fileURLToPath(new URL('../../partidoya-api-20232545.env', import.meta.url));
  if (existsSync(envFile)) {
    const { GOOGLE_MAPS_API_KEY } = parseEnv(readFileSync(envFile, 'utf8'));
    if (GOOGLE_MAPS_API_KEY) {
      process.env.GOOGLE_MAPS_API_KEY = GOOGLE_MAPS_API_KEY;
    }
  }
}

const outputFile = isDevelopment ? 'environment.local.ts' : 'environment.prod.ts';
const outputPath = fileURLToPath(new URL(`../src/environments/${outputFile}`, import.meta.url));
const defaultApiUrl = isDevelopment
  ? 'http://localhost:8080/api/v1'
  : 'https://partidoya-api-20232545.onrender.com/api/v1';
const apiUrl = (process.env.API_URL || defaultApiUrl).replace(/\/$/, '');
const googleMapsApiKey = process.env.GOOGLE_MAPS_API_KEY || '';

const source = `export const environment = {
  production: ${!isDevelopment},
  apiUrl: ${JSON.stringify(apiUrl)},
  googleMapsApiKey: ${JSON.stringify(googleMapsApiKey)}
};
`;

writeFileSync(outputPath, source, 'utf8');
