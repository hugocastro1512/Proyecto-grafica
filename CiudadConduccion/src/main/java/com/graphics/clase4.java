package com.graphics; // Reúne la versión final con las otras tres lecciones.

import java.util.ArrayList; // Para la lista de vehículos autónomos.
import java.util.List; // Interfaz de lista.
import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
 * CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
 * Ampliada a mapa 11x11, paradas en red vial expandida, semáforos sincronizados,
 * tráfico autónomo (Mejora Opcional 1), y panel de información / HUD (Mejora Opcional 2).
 */
public class clase4 extends clase3 {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio (tecla M).
    private boolean mostrarAyuda = true; // Muestra panel de controles / HUD dentro de la ventana (tecla H).
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.

    // 4 destinos de entrega distribuidos sobre calles accesibles de la ciudad 11x11 (avenidas en -50, -30, -10, 10, 30, 50)
    private static final float[][] DESTINOS = {
        { 30, -30}, // Primera entrega: cruce noreste
        {-30, -30}, // Segunda entrega: cruce noroeste
        { 10,  10}, // Tercera entrega: intersección central este
        {-30,  30}  // Cuarta entrega: retorno al cruce suroeste
    };

    // Vehículos de tráfico autónomo que circulan por las calles (Mejora Opcional)
    private final List<AutoTrafico> trafico = new ArrayList<>();

    // Rutas cerradas (bucles) por las calles (coordenadas -50, -30, -10, 10, 30, 50)
    // Ruta 1: Circuito cuadrante norte (avenidas Z=-50, X=30, Z=-10, X=-30)
    private static final float[][] RUTA_VERDE = {
        {-30, -50}, { 30, -50}, { 30, -10}, {-30, -10}
    };
    // Ruta 2: Circuito cuadrante sur (avenidas Z=10, X=50, Z=50, X=-10)
    private static final float[][] RUTA_AMARILLA = {
        {-10,  10}, { 50,  10}, { 50,  50}, {-10,  50}
    };
    // Ruta 3: Anillo perimetral e interior oeste (avenidas X=-50, Z=30, X=10, Z=-30)
    private static final float[][] RUTA_AZUL = {
        {-50, -30}, { 10, -30}, { 10,  30}, {-50,  30}
    };

    public clase4() {
        super();
        iniciarTrafico();
    }

    private void iniciarTrafico() {
        trafico.clear();
        // 3 vehículos autónomos que siguen estrictamente las avenidas
        trafico.add(new AutoTrafico(RUTA_VERDE,    8.5f, 0.2f, 0.7f, 0.3f));  // Auto verde
        trafico.add(new AutoTrafico(RUTA_AMARILLA, 7.5f, 0.9f, 0.8f, 0.1f));  // Taxi amarillo
        trafico.add(new AutoTrafico(RUTA_AZUL,     9.0f, 0.2f, 0.3f, 0.85f)); // Auto azul
    }

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade controles del minimapa y panel HUD a los controles anteriores. */
    @Override
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) {
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
        if (key == GLFW_KEY_H) {
            mostrarAyuda = !mostrarAyuda; // Alterna la visibilidad del HUD / panel interior.
        }
    }

    /** Reinicia el vehículo, el progreso de las entregas y el tráfico. */
    @Override
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición (-30, 30), velocidad y orientación del vehículo del jugador.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        iniciarTrafico(); // Restaura las posiciones iniciales de las rutas
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS Y TRÁFICO ====================

    /**
     * Comprueba colisiones con la ciudad Y con los vehículos autónomos de tráfico.
     * Si la nueva posición invade un auto bot, rechaza el movimiento impidiendo que se superpongan.
     */
    @Override
    protected boolean puedeCircular(float x, float z) {
        // Primero verifica las colisiones normales con las manzanas y bordes de la ciudad (incluye auto y remolque)
        if (!super.puedeCircular(x, z)) {
            return false;
        }

        // Posición de la chata de almacenamiento
        float chataX = x + (float) Math.sin(angulo) * ChataZ;
        float chataZ = z + (float) Math.cos(angulo) * ChataZ;

        // Verifica colisión con cada uno de los vehículos autónomos
        // Radio de colisión para auto y remolque respectivamente
        float radioCuadradoAuto = 2.5f * 2.5f;
        float radioCuadradoChata = 2.0f * 2.0f;

        for (AutoTrafico auto : trafico) {
            // Colisión auto principal con bot
            float dxAuto = x - auto.x;
            float dzAuto = z - auto.z;
            if (dxAuto * dxAuto + dzAuto * dzAuto < radioCuadradoAuto) {
                return false;
            }

            // Colisión remolque / chata con bot
            float dxChata = chataX - auto.x;
            float dzChata = chataZ - auto.z;
            if (dxChata * dxChata + dzChata * dzChata < radioCuadradoChata) {
                return false;
            }
        }
        return true;
    }

    /** Actualiza el jugador, el tráfico autónomo y comprueba entregas. */
    @Override
    protected void actualizar(float deltaTime) {
        // Actualizar primero el movimiento de los vehículos autónomos considerando la presencia del jugador, su remolque y otros bots
        for (AutoTrafico auto : trafico) {
            auto.actualizar(deltaTime, autoX, autoZ, angulo, trafico);
        }

        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones del jugador y título.

        if (entregas >= DESTINOS.length) {
            return; // Partida finalizada: conserva el tiempo
        }

        tiempo += deltaTime; // Acumula el cronómetro
        float distanciaX = autoX - DESTINOS[entregas][0];
        float distanciaZ = autoZ - DESTINOS[entregas][1];
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ;
        boolean estaCerca = distanciaCuadrada < 3.5f * 3.5f; // Radio de parada
        boolean estaFrenando = Math.abs(velocidad) < 1.2f; // Frenado en la marca

        if (estaCerca && estaFrenando) {
            entregas++; // Completa la parada activa
        }
    }

    /** Compone el progreso e información del título. */
    @Override
    protected String estadoExtra() {
        String mensaje = super.estadoExtra();
        mensaje += " | M: mapa | H: panel | ";
        if (entregas == DESTINOS.length) {
            mensaje += "GANASTE en " + (int) tiempo + " s! R: reiniciar";
        } else {
            mensaje += "Entregas " + entregas + "/" + DESTINOS.length;
            mensaje += " | Frena en marca dorada | " + (int) tiempo + " s";
        }
        return mensaje;
    }

    // ==================== 4. ESCENA FINAL, DESTINO Y TRÁFICO ====================

    @Override
    protected void escena() {
        super.escena(); // Dibuja ciudad, auto del jugador y farolas
        if (!vistaMapa) {
            decorarCiudad(); // Árboles, bancos, ventanas, cruces y semáforos
        }
        if (entregas < DESTINOS.length) {
            dibujarDestino(); // Plataforma y baliza dorada activa
        }
        dibujarTrafico(); // Dibuja los vehículos autónomos circulando
    }

    /** Dibuja los vehículos de tráfico autónomo. */
    private void dibujarTrafico() {
        for (AutoTrafico auto : trafico) {
            cajaGirada(auto.x, 0.60f, auto.z, 1.6f, 0.5f, 2.4f, auto.colorR, auto.colorG, auto.colorB, auto.angulo);
            cajaGirada(auto.x, 1.05f, auto.z + 0.1f, 1.2f, 0.45f, 1.2f, 0.85f, 0.9f, 0.95f, auto.angulo);
            // Faros delanteros del auto de tráfico (orientados y posicionados con el ángulo del auto)
            float frenteX = -(float) Math.sin(auto.angulo);
            float frenteZ = -(float) Math.cos(auto.angulo);
            cajaGirada(auto.x + frenteX * 1.25f, 0.65f, auto.z + frenteZ * 1.25f, 1.1f, 0.18f, 0.18f, 1.0f, 0.95f, 0.4f, auto.angulo);
        }
    }

    /** Marca la próxima parada con una plataforma y baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0];
        float z = DESTINOS[entregas][1];
        entero("uEmision", 1);
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Plataforma dorada en asfalto
        if (!vistaMapa) {
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2.5f) * 0.4f;
            cajaGirada(x, alturaBaliza, z, 0.85f, 0.85f, 0.85f, 1, 0.85f, 0.15f, tiempo * 1.5f);
        }
        entero("uEmision", 0);
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) {
            for (int columna = 0; columna < MAPA[fila].length; columna++) {
                float x = centro(columna);
                float z = centro(fila);
                int tipo = MAPA[fila][columna];
                if (tipo == 2) {
                    dibujarParque(x, z);
                }
                if (tipo == 1) {
                    float altura = 5 + (fila * 3 + columna * 7) % 9;
                    dibujarVentanas(x, z, altura);
                }
                if (tipo != 0) {
                    dibujarPasoPeatonal(x, z);
                    dibujarSemaforo(x + 4, z - 4);
                }
            }
        }
    }

    /** Cuatro árboles y un banco en cada parque. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f};
        for (float desplazamientoX : posiciones) {
            for (float desplazamientoZ : posiciones) {
                float arbolX = x + desplazamientoX;
                float arbolZ = z + desplazamientoZ;
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Tronco
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Copa
            }
        }
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Asiento
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Soporte
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Respaldo
    }

    /** Ventanas iluminadas en los edificios (aparentan luz nocturna). */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) {
            entero("uEmision", 1);
        }
        for (float y = 1.7f; y < altura; y += 2) {
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) {
                caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f);
                caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f);
                caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f);
                caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f);
            }
        }
        entero("uEmision", 0);
    }

    /** Franjas de paso peatonal junto a cada manzana. */
    private void dibujarPasoPeatonal(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) {
            caja(x + desplazamiento, 0.045f, z - 8, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f);
        }
    }

    /** Semáforo animado sincronizado: Rojo (5s), Verde (5s), Amarillo (2s). */
    private void dibujarSemaforo(float x, float z) {
        caja(x, 1.7f, z, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Poste
        caja(x, 3.1f, z, 0.55f, 1.2f, 0.45f, 0.08f, 0.10f, 0.12f); // Carcasa
        int fase = (int) (tiempo % 12);
        for (int indice = 0; indice < 3; indice++) {
            boolean encendida = false;
            if (indice == 0) encendida = fase < 5; // Rojo
            else if (indice == 1) encendida = fase >= 10; // Amarillo
            else encendida = fase >= 5 && fase < 10; // Verde

            float brillo = 0.15f;
            entero("uEmision", 0);
            if (encendida) {
                brillo = 1;
                entero("uEmision", 1);
            }
            float rojo = (indice < 2) ? brillo : 0;
            float verde = (indice > 0) ? brillo : 0;
            float altura = 3.45f - indice * 0.35f;
            caja(x, altura, z - 0.24f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f);
        }
        entero("uEmision", 0);
    }

    // ==================== 6. MINIMAPA Y HUD INTERACTIVO ====================

    @Override
    protected void dibujarFrame() {
        super.dibujarFrame(); // 1er Pase: Vista principal en 3D
        if (mostrarMapa) {
            dibujarMinimapa(); // 2do Pase: Minimapa ortográfico en esquina superior derecha
        }
        if (mostrarAyuda) {
            dibujarPanelHUD(); // 3er Pase: Panel en pantalla con velocímetro e indicadores (Mejora Opcional)
        }
    }

    /** Dibuja el minimapa ortográfico en la esquina superior derecha. */
    private void dibujarMinimapa() {
        int dimensionMenor = Math.min(ancho, alto);
        int lado = Math.min(270, dimensionMenor / 3);
        int margen = Math.min(18, dimensionMenor / 20);
        int x = ancho - lado - margen;
        int y = alto - lado - margen;

        glEnable(GL_SCISSOR_TEST);
        glScissor(x - 3, y - 3, lado + 6, lado + 6);
        glClearColor(0.82f, 0.88f, 0.95f, 1); // Marco claro
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glScissor(x, y, lado, lado);
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Fondo del minimapa
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glViewport(x, y, lado, lado);
        entero("uMapa", 1);
        vistaMapa = true;
        try {
            escena();
            dibujarIndicadorAuto();
            dibujarIndicadoresTraficoMinimapa();
        } finally {
            vistaMapa = false;
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto);
        }
    }

    /** Resalta la posición y frente del jugador en el minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 3.2f, 0.1f, 4.5f, 0.1f, 1, 1, angulo);
        float frenteX = -(float) Math.sin(angulo);
        float frenteZ = -(float) Math.cos(angulo);
        caja(autoX + frenteX * 3.0f, 26, autoZ + frenteZ * 3.0f, 1.2f, 0.1f, 1.2f, 1, 1, 1);
    }

    /** Representa los autos de tráfico en el minimapa como pequeños puntos naranjas. */
    private void dibujarIndicadoresTraficoMinimapa() {
        for (AutoTrafico auto : trafico) {
            caja(auto.x, 24, auto.z, 2.5f, 0.1f, 2.5f, 1.0f, 0.5f, 0.1f);
        }
    }

    /**
     * Dibuja un panel gráfico HUD dentro de la ventana (Mejora opcional: Panel e interfaz).
     * Muestra barra de velocidad, estado de luces, progreso de entregas y ayuda de controles.
     */
    private void dibujarPanelHUD() {
        int panelAncho = Math.min(290, ancho / 3);
        int panelAlto = 150;
        int panelX = 18;
        int panelY = 18;

        glEnable(GL_SCISSOR_TEST);
        glScissor(panelX - 2, panelY - 2, panelAncho + 4, panelAlto + 4);
        glClearColor(0.2f, 0.35f, 0.5f, 0.85f); // Borde azul metálico
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glScissor(panelX, panelY, panelAncho, panelAlto);
        glClearColor(0.08f, 0.11f, 0.15f, 0.9f); // Fondo oscuro traslúcido
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glViewport(panelX, panelY, panelAncho, panelAlto);
        entero("uMapa", 1);
        entero("uEmision", 1);
        try {
            // Fondo plano del velocímetro
            caja(0, 0, 10, 48, 1, 12, 0.15f, 0.20f, 0.28f);

            // Barra de progreso de velocidad (0 a 16 unidades/s)
            float ratioVel = Math.min(1.0f, Math.abs(velocidad) / 16.0f);
            float barraW = 44.0f * ratioVel;
            caja(-22.0f + barraW / 2.0f, 0, 10, barraW, 1, 8, 0.1f + 0.85f * ratioVel, 0.9f - 0.7f * ratioVel, 0.2f);

            // Icono / indicador de Faros (tecla F): Verde si ON, gris si OFF
            float farosR = faros ? 1.0f : 0.3f;
            float farosG = faros ? 0.9f : 0.3f;
            float farosB = faros ? 0.2f : 0.3f;
            caja(-15, 0, 30, 10, 1, 10, farosR, farosG, farosB);

            // Icono / indicador de Ambiente (tecla N): Amarillo = Día, Azul oscuro = Noche
            float solR = noche ? 0.2f : 1.0f;
            float solG = noche ? 0.35f : 0.8f;
            float solB = noche ? 0.8f : 0.2f;
            caja(15, 0, 30, 10, 1, 10, solR, solG, solB);

            // Bloques de estado de entregas (4 bloques de progreso)
            for (int i = 0; i < DESTINOS.length; i++) {
                float blockR = (i < entregas) ? 0.2f : 0.35f;
                float blockG = (i < entregas) ? 0.9f : 0.35f;
                float blockB = (i < entregas) ? 0.3f : 0.35f;
                caja(-18 + i * 12, 0, -25, 8, 1, 10, blockR, blockG, blockB);
            }
        } finally {
            entero("uEmision", 0);
            entero("uMapa", 0);
            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, ancho, alto);
        }
    }

    /** Punto de entrada del proyecto final ampliado. */
    public static void main(String[] args) {
        clase4 aplicacion = new clase4();
        aplicacion.run();
    }
}
