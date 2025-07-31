AgroBot: Monitoreo y Evaluación de Suelo con IoT
Descripción del Proyecto
AgroBot es un sistema de monitoreo y control agrícola basado en el Internet de las Cosas (IoT). El objetivo es proporcionar una solución integral para evaluar las condiciones ambientales y del suelo en tiempo real, ayudando a optimizar el proceso de plantado y el cuidado de los cultivos. El proyecto integra un prototipo de hardware con un microcontrolador Arduino y sensores, con una aplicación móvil Android moderna que permite el monitoreo remoto, el control de actuadores, y la gestión segura de datos en la nube.

Características Principales
Monitoreo en Tiempo Real: Lectura de sensores de gas (MQ-135) y humedad del suelo, con visualización de datos en la aplicación móvil.

Evaluación de Aptitud de Suelo: La aplicación evalúa las lecturas de los sensores para determinar si las condiciones son óptimas para el plantado.

Control Remoto: La aplicación móvil puede enviar comandos al microcontrolador para activar actuadores, como un LED indicador.

Comunicación Inalámbrica: Utiliza un módulo Bluetooth (HC-06/HC-05) para una comunicación bidireccional, estable y de bajo consumo entre el microcontrolador y el dispositivo móvil.

Autenticación Segura de Usuarios: Integración con Firebase Authentication que permite el inicio de sesión con email/contraseña y Google Sign-In.

Persistencia y Respaldo de Datos: Las lecturas de los sensores se almacenan en Firebase Realtime Database, proporcionando un historial accesible en la nube.

Gestión de Datos Offline: La aplicación almacena temporalmente los datos de los sensores de forma local si no hay conexión a Internet, y los sincroniza automáticamente con Firebase al recuperar la conectividad.

Tecnologías Utilizadas
Hardware
Microcontrolador: Arduino Uno R3

Módulo de Comunicación: Bluetooth HC-06/HC-05

Sensor 1: Sensor de Gas MQ-135

Sensor 2: Módulo Sensor de Humedad de Suelo (Digital)

Actuador: LED Rojo

Componentes Adicionales: Resistencias (1kΩ, 2kΩ, 220Ω), Protoboard, Cables Jumper.

Software
IDE de Hardware: Arduino IDE

Lenguaje de Hardware: C++

IDE de Software: Android Studio

Lenguaje de Software: Kotlin

Framework de UI: Jetpack Compose

Servicios en la Nube: Firebase Authentication, Firebase Realtime Database

Librerías de Android: androidx.lifecycle, SharedPreferences

Librerías de Arduino: SoftwareSerial.h

Arquitectura del Sistema
El sistema se compone de dos módulos principales que interactúan entre sí:

Módulo de Hardware (Arduino): Lee los datos de los sensores de gas y humedad. Usa la librería SoftwareSerial para establecer una comunicación serial con el módulo Bluetooth. Escucha comandos de la aplicación móvil y envía los datos de los sensores en el formato G:valor,H:estado\n.

Módulo de Software (App Android): Desarrollada con Kotlin y Jetpack Compose. Se conecta al módulo Bluetooth del Arduino. Envía comandos de solicitud de datos (GET_DATA\n) o de control (1\n, 0\n). Recibe, procesa y visualiza los datos en una interfaz moderna. Gestiona la autenticación de usuarios y la persistencia de datos en la nube con Firebase, incluyendo una robusta gestión de datos offline.

Configuración y Uso
1. Configuración del Hardware
Para configurar el prototipo, sigue el siguiente esquema de conexión:

[Aquí, inserta tu diagrama de conexión corregido. Puedes usar una imagen del diagrama de Fritzing o Circuito.io que has creado.]

2. Código del Microcontrolador
El código del Arduino se encuentra en la carpeta Arduino/ del repositorio.

Abre el archivo .ino en el Arduino IDE.

Asegúrate de que la librería SoftwareSerial.h esté instalada.

Sube el código a tu placa Arduino Uno.

3. Configuración de la Aplicación Android
Requisitos: Necesitas tener Android Studio instalado y un proyecto de Firebase configurado.

Clonar el Repositorio: Clona el repositorio de la aplicación desde GitHub.

Configurar Firebase:

Descarga el archivo google-services.json desde la consola de Firebase.

Copia el archivo y pégalo en la carpeta app/ de tu proyecto.

Ejecutar la Aplicación: Abre el proyecto en Android Studio y ejecútalo en un dispositivo Android con Bluetooth activado y emparejado previamente con el módulo HC-06.
