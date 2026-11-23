# Referencias: TuRuta (función) + Luma (acabado)

Fecha: 2026-11-23 · GoWayki KMP (Arequipa, GTFS offline)

## Canales oficiales TuRuta (comprobados)

| Canal | URL / id |
| --- | --- |
| Sitio | https://turuta.pe/ |
| Android | Play Store `pe.tumicro.android` |
| iOS | App Store id `1186323189` |
| Contacto | hola@turuta.pe · TUMICRO S.A.C. |

**No se instaló ni se navegó la app dentro del dispositivo** en este servidor. Lo siguiente se basa en ficha Play/App Store, capturas públicas del listing y el sitio oficial. Donde no se pudo comprobar, se marca como *no verificado*.

## Observado (TuRuta) vs propuesto (GoWayki)

| Tema | Observado en TuRuta | Propuesto / adaptado en GoWayki |
| --- | --- | --- |
| Búsqueda O/D | Barra superior tipo “¿A dónde vas?” sobre mapa; CTA “BUSCAR” (capturas Play) | Chip O/D sobre mapa + pantalla Search con FTS offline Arequipa; origen = GPS o lugar |
| Comparar rutas | Panel inferior con alternativas (tiempo, líneas) | Chips horizontales de itinerarios RAPTOR + duración / caminata / tarifa estimada |
| Mapa ↔ resultados | Mapa visible con polilínea al elegir opción | Mapa a sangre + `easeCamera` al cambiar itinerario; padding inferior según hoja |
| Paraderos / transbordos / recorrido | Pasos caminar + subir/bajar (listing); GPS buses en vivo (sitio) | Paraderos GTFS + pasos Walk/Transit; **sin** ubicación de vehículos en vivo (no hay feed) |
| Instrucciones de viaje | Lista paso a paso | Hoja expandible con pasos; “Iniciar viaje” local |
| Accesos rápidos | Buscar como acción dominante | Home: CTA buscar, recientes, reportes, nav inferior |
| Tiempo real / fotos de línea | Sitio: GPS ~3000 buses, imágenes de líneas | **No prometido** en GoWayki sin datos |

## Luma (acabado visual)

Referencia de jerarquía, paneles limpios, tipografía y cuidado de interacción. GoWayki mantiene identidad propia (zorro-G, `#F05709` / `#292929`, Righteous/Ubuntu/Poppins) y no copia marca ni assets de Luma ni de TuRuta.

## Motion (calidad tipo Flutter, en Compose)

- Springs controlados para hoja y press; sin rebotes excesivos.
- Resultados: stagger **una sola vez** por plan; al actualizar selección no se reanima la lista.
- Hoja arrastrable con snapeos peek / mid / full.
- Confirmación breve al reportar; háptica discreta (Android).
- Respeta `reduceMotion`.
- Sin shimmer infinito, blur permanente ni recrear el mapa en cada frame.
