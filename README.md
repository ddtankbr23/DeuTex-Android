# DeuTex-Android

Editor Android ARM64 para explorar y modificar recursos WAD de Doom y derivados. El proyecto evoluciona hacia un taller modular multi-formato, inspirado en las capacidades de SLADE 3, con interfaz Android nativa.

## Funciones implementadas

- Abrir y explorar archivos IWAD/PWAD.
- Buscar recursos y mostrar tamaños.
- Vista previa de PNG y gráficos Doom (patches).
- Leer y editar offsets gráficos, incluidos offsets PNG grAb.
- Exportar gráficos PNG individuales y por lotes ZIP.
- Reemplazar gráficos desde PNG y lotes ZIP.
- Renombrar y eliminar entradas, también en selección múltiple.
- Reconstruir y exportar WAD conservando recursos no modificados.
- Editor táctil de offsets con zoom.
- Compilación automatizada del APK de depuración para arm64-v8a.

## Arquitectura futura

La aplicación se está preparando para separar archivos, recursos, editores y herramientas de motor:

- WAD: mantener y estabilizar el editor existente.
- ZIP/PK3: explorador y editor de paquetes.
- Gráficos y multimedia: PNG/JPEG y audio/música.
- Scripting: editor de ZScript, ACS, DECORATE, MAPINFO y otros lenguajes.
- Mapas: módulo futuro para Doom, Hexen y UDMF.

SLADE 3 sirve como referencia de funciones y compatibilidad. No se copiarán componentes automáticamente: cada dependencia se evaluará por rendimiento en Android, mantenimiento y licencia.

## Estado y limitaciones

El puente JNI/C++ existe, pero el motor original DeuTex todavía no está integrado. La detección ZIP/PK3 y la clasificación de lenguajes constituyen infraestructura inicial, no equivalen todavía a un editor PK3 o ZScript terminado. La edición/validación ZScript y ACS, compiladores, audio avanzado y editor de mapas siguen pendientes.

## Compilar el APK

1. Abre la pestaña Actions del repositorio.
2. Selecciona Build Android APK.
3. Abre la ejecución más reciente que haya terminado correctamente.
4. Descarga el artefacto DeuTex-Android-debug.

El workflow también se ejecuta automáticamente al hacer push a main.

## Tecnología

- Kotlin + Jetpack Compose.
- Android mínimo API 23.
- Java 17, Gradle 8.9, Android NDK.
- ABI inicial: arm64-v8a.

## Proyecto original y licencias

DeuTex: https://github.com/Doom-Utils/deutex

Antes de integrar DeuTex, ACC u otros componentes se revisarán sus licencias y dependencias, y se conservarán los avisos requeridos. No se incorporará código de SLADE ni de otros proyectos sin esa revisión.
