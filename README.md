# DeuTex Android

Aplicación Android ARM64 para explorar y gestionar recursos WAD de Doom, Doom II, Heretic, Hexen y Strife.

## Estado actual: prototipo 0.1.0

- Interfaz Android nativa con Jetpack Compose.
- Selector de archivos del dispositivo.
- Lector inicial de cabeceras y directorios IWAD/PWAD.
- Listado de entradas WAD con nombre y tamaño.
- Puente JNI/C++ compilado únicamente para `arm64-v8a`.
- Flujo GitHub Actions para generar un APK de depuración.

**Importante:** esta primera compilación es una base funcional para abrir y explorar el índice de un WAD. Todavía no incluye el motor DeuTex original, ni extracción, importación, creación PWAD, previsualización de gráficos o reproducción de audio. Esas funciones se incorporarán y probarán por etapas; no se deben considerar disponibles todavía.

## Compilar el APK

1. Abre la pestaña **Actions**.
2. Selecciona **Build Android APK**.
3. Pulsa **Run workflow** y confirma la rama `main`.
4. Cuando termine correctamente, abre la ejecución y descarga el artefacto **DeuTex-Android-debug**.

También se ejecuta al hacer push a `main`.

## Tecnología

- Kotlin + Jetpack Compose para la interfaz.
- C++/JNI y Android NDK para la futura integración del motor.
- ABI objetivo: `arm64-v8a`.
- Android mínimo: API 23.

## Proyecto original

DeuTex: https://github.com/Doom-Utils/deutex

El proyecto original es un programa de línea de comandos escrito en C. Su código y sus licencias deben conservarse al integrarlo. DeuTex puede trabajar con WAD de Doom, Freedoom, Heretic, Hexen y Strife, y admite operaciones de extracción e inserción de recursos.

## Licencias

La aplicación de integración se distribuye separadamente del código original. Al incorporar DeuTex, se incluirán los avisos de copyright y los textos de licencia exigidos por cada componente (incluidas las licencias GPL/LGPL indicadas en el repositorio original).
