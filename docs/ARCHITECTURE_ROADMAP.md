# DeuTex-Android — arquitectura y hoja de ruta

## Objetivo

Convertir la aplicación en un taller Android modular para recursos de Doom y motores compatibles, tomando SLADE 3 como referencia funcional. SLADE es una guía de capacidades, no una obligación de reutilizar su código ni sus bibliotecas de escritorio.

## Principios

1. Android nativo primero: Kotlin y Jetpack Compose; ARM64 (arm64-v8a) como ABI inicial.
2. Formatos desacoplados: cada formato tendrá lector/escritor independiente. La interfaz no dependerá de WAD.
3. Editores por tipo de recurso: imágenes, audio, música, texto/scripts, mapas y modelos.
4. Motores como adaptadores: DeuTex, herramientas ACS y utilidades futuras detrás de interfaces propias.
5. Compatibilidad y seguridad: validar tamaños, offsets, rutas ZIP, archivos anidados y límites de memoria.
6. Licencias antes de integrar código: revisar licencia, avisos y dependencias antes de incluir componentes en el APK.

## Capas previstas

- core: modelos neutrales, detección de formatos, operaciones comunes y validación.
- archives: WAD, ZIP/PK3 y futuros contenedores.
- resources: clasificación y conversión de recursos.
- editors: editores de gráficos, audio, texto y mapas.
- scripting: lenguaje, resaltado, navegación y validación de scripts.
- engines: adaptadores para DeuTex, ACS/ACC y herramientas futuras.
- ui: navegación, pestañas, explorador y editores Compose.

La primera base compartida está en ResourceFormats.kt: detección WAD/ZIP, clasificación de recursos y reconocimiento de lenguajes de scripts.

## Hoja de ruta

### Etapa 1 — estabilizar el editor WAD existente
- Mantener las funciones probadas de listado, búsqueda, vista previa, offsets, importación/exportación y operaciones por lotes.
- Añadir pruebas automatizadas para lectura, escritura, renombrado, eliminación y reemplazo.
- Mantener la documentación alineada con las capacidades reales.

### Etapa 2 — arquitectura multi-formato
- Extraer contratos neutrales para archivos, entradas, lectores, escritores y validadores.
- Separar la lógica WAD actual de la interfaz.
- Mantener operaciones destructivas como cambios pendientes hasta exportar.

### Etapa 3 — ZIP/PK3
- Explorar ZIP/PK3 sin extraer todo el archivo a memoria.
- Navegar carpetas y recursos, incluyendo rutas internas.
- Importar, exportar, reemplazar y reconstruir conservando entradas no modificadas.
- Proteger contra ZIP bombs, rutas ../, nombres duplicados y tamaños excesivos.

### Etapa 4 — recursos gráficos y multimedia
- Añadir JPG/JPEG y formatos gráficos comunes.
- Mejorar conversión entre PNG y gráficos Doom cuando sea posible.
- Añadir preview de OGG, WAV y MIDI mediante componentes Android o bibliotecas con licencia compatible.

### Etapa 5 — editor de texto y scripting
- Editor de texto con resaltado, búsqueda, autoindentación y navegación.
- ZScript (.zs, .zsc) como lenguaje prioritario, con documentación y referencias de GZDoom.
- ACS (.acs) como módulo distinto; investigar compilación ACC y su licencia antes de distribuirlo.
- Añadir después DECORATE, MAPINFO/ZMAPINFO, TEXTURES y otros lenguajes/configuraciones.
- No prometer compilación ZScript independiente: primero edición y análisis estático; la validación real debe corresponder a la versión de GZDoom objetivo.

### Etapa 6 — herramientas de motor y formatos avanzados
- Investigar selectivamente componentes de SLADE cuando aporten valor y su licencia/dependencias permitan portarlos.
- Evaluar modelos, texturas compuestas y formatos adicionales según demanda.
- Mantener motores externos como módulos reemplazables.

### Etapa 7 — editor de mapas
- Módulo separado por su complejidad.
- Comenzar con vista previa y geometría 2D; después Doom/Hexen/UDMF, sectores, linedefs, alturas y vista 3D.
- Integrar recursos para que las texturas editadas se reflejen en el mapa.

## Estado real

La aplicación cuenta con editor WAD Android con operaciones de recursos y un flujo GitHub Actions para generar APK ARM64. La integración del motor DeuTex original y los módulos PK3, ZScript, ACS y mapas siguen pendientes; no deben mostrarse como terminados hasta estar implementados y probados.
