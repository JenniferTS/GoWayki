# gowayki go UI — identidad + motion (v1.5.6)

## Marca
- Nombre: **gowayki go** (go naranja + wayki tinta)
- Primarios: `#F05709` / `#292929` · tipografías Righteous · Ubuntu · Poppins
- Logo: zorro-G con fondo blanco (drawable + launcher)

## Referencias (ver `REFERENCIAS_TURUTA_LUMA.md`)
- **TuRuta**: claridad funcional (buscar → comparar → pasos). Sin copiar marca; sin GPS de buses.
- **Luma**: jerarquía, paneles, tipografía e interacciones cuidadas.

## Motion (Compose, calidad tipo Flutter)
1. Pantallas: fade + slide corto; respeta movimiento reducido.
2. CTA: press scale spring (sin shimmer infinito).
3. Búsqueda: panel `animateContentSize` + `imePadding`; foco con borde.
4. Resultados: stagger **una vez** por plan (`StaggerIn` + `enterKey`).
5. Selección de ruta: color/escala animados; mapa `easeCamera` con padding de hoja.
6. `JourneySheet`: snapeos peek / mid / full + drag con spring `panel()`.
7. Reportes: `ActionConfirmBanner` + háptica `Success`.
8. Háptica discreta (`platformHaptic`) en Android.

## Recorrido entregable
Inicio → Search → planificar → MapsJourney (comparar / pasos / Atrás conserva O-D y selección).
