# Validación

## Línea base

- `npm ci`: completado.
- `npm run build`: completado con un warning ya existente por el presupuesto de `home.component.scss`.
- `mvn test`: no disponible inicialmente porque Maven no estaba instalado globalmente.
- `npm test`: no ejecutable inicialmente porque `angular.json` no tenía target `test`.

## Resultado posterior a la refactorización

- `npm run build`: compilación de producción completada.
- `npm test`: 3 pruebas Vitest aprobadas para persistencia y limpieza de sesión.
- `mvn -B test`: 19 pruebas aprobadas, incluidas migraciones Flyway, reglas de dominio, almacenamiento de comprobantes y cálculo de reportes.
- `npm audit --omit=dev`: 0 vulnerabilidades en dependencias de producción.
- `git diff --check`: sin errores de whitespace.
- Comparación visual de `/`: capturas original/refactorizada idénticas por SHA-256 en 1440×1000 y 390×844, con animaciones y video estabilizados en el mismo estado.

## Advertencias conocidas

- Angular mantiene un warning de presupuesto: `home.component.scss` pesa aproximadamente 16.44 kB frente al umbral de 12 kB. El archivo conserva los estilos para evitar cambios visuales.
- `npm audit` informa 16 vulnerabilidades moderadas transitivas de herramientas de desarrollo ligadas a Angular CLI. La corrección automática propuesta es un downgrade mayor incompatible; las dependencias de producción no están afectadas.
- La integración real con MySQL administrado, Culqi, Google Maps y Render requiere secretos y servicios externos. Se validaron compilación, contexto Spring, migraciones sobre H2 y pruebas automatizadas, no transacciones reales en producción.
