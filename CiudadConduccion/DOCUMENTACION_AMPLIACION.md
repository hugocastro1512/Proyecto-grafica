# Documentación Técnica: Ampliación de la Ciudad Interactiva con OpenGL

Este documento describe la arquitectura, ampliaciones y mejoras añadidas al proyecto **CiudadConduccion**, estructurado en cuatro lecciones progresivas (`clase1` a `clase4`). La documentación se enfoca en **qué se implementó o modificó**, **de qué manera se realizó** y **por qué se llevó a cabo el cambio**, cubriendo los requisitos obligatorios del enunciado y las mejoras opcionales con bonificación.

---

## 1. Resumen de Características y Estado del Proyecto

| Módulo / Requisito | Estado | Características Principales |
| :--- | :---: | :--- |
| **Ampliación de la Ciudad** | ✅ Operativo | Mapa expandido a 11 × 11 celdas (110 × 110 unidades), 16 manzanas de edificios, 5 parques, red vial conectada y cámara orbital recalibrada. |
| **Límites, Conducción y Spawn** | ✅ Operativo | Posición inicial y reinicio en calzada despejada `(-30, 30)`, colisiones circulares contra manzanas, aceras y bordes del mapa. |
| **Sistema de Iluminación** | ✅ Operativo | Ciclo Día/Noche (`N`), faros direccionales del auto (`F`), 16 farolas sobre aceras con atenuación cuadrática y cálculo Lambertiano en GPU. |
| **Funciones Urbanas y Minimapa** | ✅ Operativo | Semáforos animados sincronizados, decoración completa (árboles, bancos, ventanas iluminadas, pasos cebra), 4 entregas y minimapa ortográfico (`M`). |
| **Tráfico Autónomo (Mejora Opcional)** | ✅ Operativo | 3 vehículos autónomos que circulan por bucles cerrados de avenidas, con frenado inteligente ante obstáculos y colisiones físicas mutuas. |
| **Panel HUD (Mejora Opcional)** | ✅ Operativo | Interfaz gráfica en pantalla (`H`) con velocímetro dinámico, estado de iluminación y progreso visual de las entregas. |

---

## 2. Detalle de Cambios por Componente

### 2.1. Escenario Urbano y Proyecciones ([clase1.java](file:///c:/Users/Hugo/Desktop/Proy%20Grafica/CiudadConduccion/src/main/java/com/graphics/clase1.java))

#### ¿Qué cambió?
- Se sustituyó la matriz original de 7 × 7 por una **matriz `MAPA` de 11 × 11 celdas**.
- Se amplió la constante `LIMITE` de 35 a **55 unidades**, configurando una metrópoli de **110 × 110 unidades**.
- Se extendió la geometría del asfalto base a 110 × 110 (`LIMITE * 2`).
- Se reconfiguró la cámara orbital en `configurarCamara()` (radio de órbita 100, altura 85).
- Se adaptó el divisor de coordenadas en el **Vertex Shader** (`vMundo.x / 58.0`, `-vMundo.z / 58.0`) para encuadrar la ciudad ampliada dentro del volumen canónico (-1 a 1).

#### ¿De qué manera cambió?
- **Distribución de parcelas:**
  - **16 manzanas con edificios:** Con variaciones de altura (entre 5 y 13 unidades) y coloración generada procedimentalmente en base a su fila y columna.
  - **5 parques:** Parcelas con césped verde, vegetación y mobiliario.
  - **Red vial estructurada:** Avenidas en todas las filas y columnas pares (`-50, -30, -10, 10, 30, 50`), permitiendo circulación continua tanto en avenidas perimetrales como intermedias.
- **Transformación de vértices:** El shader utiliza una escala normalizada adaptada a 58 unidades en proyección ortográfica (`uMapa == 1`), garantizando que toda la ciudad se visualice sin recortes ni distorsiones en el minimapa.

#### ¿Por qué se cambió?
- Cumple con las dimensiones mínimas obligatorias y distribución espacial exigidas en el enunciado (mínimo 11 × 11 celdas, al menos 12 manzanas con edificios y al menos 4 parques), preservando la coherencia de escala entre el mundo 3D y la vista aérea.

---

### 2.2. Conducción, Colisiones y Spawn ([clase2.java](file:///c:/Users/Hugo/Desktop/Proy%20Grafica/CiudadConduccion/src/main/java/com/graphics/clase2.java))

#### ¿Qué cambió?
- La posición inicial del vehículo del jugador y las coordenadas del método `reiniciar()` se establecieron en **`(-30, 30)`**.
- El sistema de detección de colisiones (`puedeCircular`) se expandió para abarcar los nuevos límites del mapa y las 21 parcelas sólidas.

#### ¿De qué manera cambió?
- Las coordenadas de la calzada corresponden a las líneas viales de índice par en la matriz (`-50, -30, -10, 10, 30, 50`). La posición `(-30, 30)` ubica al vehículo exactamente en el centro de la intersección suroeste.
- El algoritmo de colisión evalúa el radio del vehículo (`RADIO_AUTO = 1.65`) contra cada acera de $10 \times 10$ unidades mediante proyección de punto más cercano (*AABB clamp*), impidiendo invadir cualquier acera o sobrepasar el límite perimetral (`LIMITE - RADIO_AUTO`).

#### ¿Por qué se cambió?
- Asegura que el jugador inicie su recorrido sobre el asfalto libre y pueda comenzar a conducir de inmediato sin quedar bloqueado dentro de construcciones urbanas.

---

### 2.3. Sistema de Iluminación y Farolas ([clase3.java](file:///c:/Users/Hugo/Desktop/Proy%20Grafica/CiudadConduccion/src/main/java/com/graphics/clase3.java))

#### ¿Qué cambió?
- Se configuró una red de **16 farolas** (`LUCES`) situadas en las esquinas de las aceras de la metrópoli.
- Se actualizó el **Fragment Shader** en GLSL para procesar un arreglo de 16 fuentes de luz puntuales (`uniform vec3 uLuces[16]`).

#### ¿De qué manera cambió?
- **Posicionamiento:** Cada farola se sitúa a una altura de $4.5$ unidades en las esquinas de las manzanas (`centro de manzana ± 4.2` unidades), orientando el poste sobre la acera y la bombilla iluminando hacia el asfalto.
- **Cálculo de Iluminación:**
  - **Luz difusa:** Aplica el modelo de reflexión Lambertiana ($N \cdot L$).
  - **Atenuación lumínica:** Disminuye en función de la distancia cuadrática:  
    $$\text{Atenuación} = 1.0 + 0.12d + 0.045d^2$$
  - **Emisión visual vs. aporte al entorno:** Las bombillas utilizan `uEmision = 1` para mantener su color cálido constante sin oscurecerse, mientras proyectan luz dinámica sobre el asfalto y las fachadas contiguas.
  - **Faros del automóvil:** Dos haces de luz cónicos con apertura angular (`smoothstep(0.85, 0.97, alineacion)`) que se desplazan y rotan solidarios al vehículo del jugador.

#### ¿Por qué se cambió?
- Satisface la especificación de contar con al menos 9 farolas distribuidas en sectores antiguos y nuevos, proporcionando visibilidad clara y realista durante la conducción nocturna (`tecla N`).

---

### 2.4. Dinámica de Misión, Decoración y Semáforos ([clase4.java](file:///c:/Users/Hugo/Desktop/Proy%20Grafica/CiudadConduccion/src/main/java/com/graphics/clase4.java))

#### ¿Qué cambió?
- Se configuró una ruta de **4 destinos de entrega** sobre calzadas transitables de la ciudad ampliada:
  1. `{30, -30}` (Sector Noreste)
  2. `{-30, -30}` (Sector Noroeste)
  3. `{10, 10}` (Intersección Central-Este)
  4. `{-30, 30}` (Retorno al punto base Suroeste)
- Se incorporaron semáforos animados con temporización cíclica continua en cada manzana.
- Se implementó la decoración de parques con árboles y bancos de madera, ventanas de edificios con iluminación nocturna y pasos de cebra.
- La tecla `R` reinicia la posición del auto, los vehículos de tráfico, el tiempo de partida y el progreso de las entregas.

#### ¿De qué manera cambió?
- **Semáforos animables:** Ciclo temporizado de 12 segundos:
  - Fase Roja: 0 a 5 s (5 segundos)
  - Fase Verde: 5 a 10 s (5 segundos)
  - Fase Amarilla: 10 a 12 s (2 segundos)
- **Validación de entregas:** Para completar cada parada se requiere aproximarse a la marca dorada (distancia $< 3.5$ unidades) y frenar el vehículo ($\text{velocidad} < 1.2\text{ u/s}$).
- **Minimapa:** Renderizado en segundo pase mediante `glScissor` y proyección ortográfica en la esquina superior derecha, preservando la vista principal al redimensionar la ventana.

#### ¿Por qué se cambió?
- Garantiza una jugabilidad completa con objetivos claros en todo el mapa extendido, retroalimentación visual en tiempo real y reinicio funcional completo.

---

### 2.5. Mejoras Opcionales Implementadas (Bonificación)

#### A. Tráfico Autónomo y Colisiones Inteligentes ([AutoTrafico.java](file:///c:/Users/Hugo/Desktop/Proy%20Grafica/CiudadConduccion/src/main/java/com/graphics/AutoTrafico.java))
- **Circulación por bucles de waypoints:** Los vehículos autónomos siguen trayectorias cerradas formadas por coordenadas de intersecciones viales:
  - **Auto Verde:** Circuito cuadrante norte (avenidas en $Z = -50$, $X = 30$, $Z = -10$, $X = -30$).
  - **Taxi Amarillo:** Circuito cuadrante sur (avenidas en $Z = 10$, $X = 50$, $Z = 50$, $X = -10$).
  - **Auto Azul:** Anillo perimetral e interior oeste (avenidas en $X = -50$, $Z = 30$, $X = 10$, $Z = -30$).
- **Frenado inteligente y colisión bidireccional:**
  - Cada bot evalúa su cono de avance frontal (`hayObstaculoAdelante`). Si el auto del jugador u otro bot se encuentra en su camino, el vehículo frena a cero automáticamente respetando la distancia de seguridad.
  - El jugador posee chequeo de colisión física contra los bots en `puedeCircular` (radio de 2.5 unidades), impidiendo que los vehículos se superpongan o se atraviesen.
- **Representación:** Circulan a velocidades constantes ajustadas con `deltaTime`, poseen faros delanteros y aparecen en el minimapa como marcadores de color naranja.

#### B. Panel HUD en Pantalla (Interfaz Urbana)
- **Implementación:** Tercer pase de renderizado en la esquina inferior izquierda de la ventana (activable/desactivable con la tecla `H`).
- **Elementos gráficos:**
  - **Velocímetro dinámico:** Barra horizontal que se extiende y cambia de tonalidad verde a roja conforme aumenta la velocidad.
  - **Indicador de Faros:** Bloque luminoso que refleja el estado de las luces delanteras (amarillo brillante en ON, gris en OFF).
  - **Indicador de Ambiente:** Icono de sol (amarillo) durante el día y luna (azul) durante la noche.
  - **Indicador de Entregas:** Cuatro bloques de progreso que se iluminan al completar cada parada.

---

## 3. Instrucciones de Compilación y Ejecución

### Ejecución de la Versión Final Completa

Desde la carpeta raíz del proyecto (`CiudadConduccion`), donde se ubica el archivo `pom.xml`:

```powershell
mvn compile exec:exec
```

### Ejecución por Lecciones Individuales

```powershell
mvn compile exec:exec "-DmainClass=com.graphics.clase1"
mvn compile exec:exec "-DmainClass=com.graphics.clase2"
mvn compile exec:exec "-DmainClass=com.graphics.clase3"
mvn compile exec:exec "-DmainClass=com.graphics.clase4"
```

---

## 4. Tabla de Controles

| Tecla | Función |
| :---: | :--- |
| `W` / `↑` | Acelerar hacia adelante |
| `S` / `↓` | Frenar / Reversa |
| `A` / `←` | Girar a la izquierda |
| `D` / `→` | Girar a la derecha |
| `Espacio` | Freno de mano |
| `C` | Alternar entre cámara de seguimiento y cámara aérea |
| `N` | Alternar ambiente Día / Noche |
| `F` | Encender / apagar faros delanteros del vehículo |
| `M` | Mostrar / ocultar el minimapa ortográfico |
| `H` | Mostrar / ocultar el panel HUD de velocidad y luces |
| `R` | Reiniciar posición del vehículo, tráfico y recorrido de entregas |
| `ESC` | Cerrar la aplicación |
