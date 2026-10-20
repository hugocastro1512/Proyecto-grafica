package com.graphics;

import java.util.List;

/**
 * Representa un vehículo autónomo que circula por las calles de la ciudad.
 * Sigue rutas fijas y continuas que forman bucles a través de la red de calles,
 * garantizando que NUNCA atraviese ningún edificio ni abandone el mapa.
 * Detecta obstáculos adelante (el automóvil del jugador y otros vehículos bot)
 * y frena/se detiene para no empujarlos ni atravesarlos.
 */
public class AutoTrafico {
    public float x;
    public float z;
    public float angulo; // Orientación en radianes del modelo 3D
    public float velocidadBase;
    public float velocidadActual;
    public float colorR;
    public float colorG;
    public float colorB;

    // Ruta de puntos de paso (waypoints) sobre las avenidas transitables
    private final float[][] ruta;
    private int indiceRuta = 0;

    public AutoTrafico(float[][] ruta, float velocidad, float r, float g, float b) {
        this.ruta = ruta;
        this.velocidadBase = velocidad;
        this.velocidadActual = velocidad;
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.x = ruta[0][0];
        this.z = ruta[0][1];
        actualizarOrientacion();
    }

    private void actualizarOrientacion() {
        int siguiente = (indiceRuta + 1) % ruta.length;
        float dx = ruta[siguiente][0] - x;
        float dz = ruta[siguiente][1] - z;

        if (Math.abs(dx) > Math.abs(dz)) {
            // Movimiento horizontal en X
            if (dx > 0) angulo = (float) (3 * Math.PI / 2); // Este (+X)
            else angulo = (float) (Math.PI / 2);           // Oeste (-X)
        } else {
            // Movimiento longitudinal en Z
            if (dz > 0) angulo = (float) Math.PI;          // Sur (+Z)
            else angulo = 0;                               // Norte (-Z)
        }
    }

    /**
     * Comprueba si hay un obstáculo en el camino frontal a corta distancia (jugador u otro vehículo bot).
     */
    private boolean hayObstaculoAdelante(float jugX, float jugZ, float jugAngulo, List<AutoTrafico> otrosAutos, float dirVecX, float dirVecZ) {
        // Vector perpendicular hacia la derecha (eje lateral del carril)
        float latVecX = -dirVecZ;
        float latVecZ = dirVecX;

        // 1. Comprobar vehículo del jugador (auto principal)
        float dxJug = jugX - x;
        float dzJug = jugZ - z;
        float longJug = dxJug * dirVecX + dzJug * dirVecZ;   // Distancia hacia adelante/atrás
        float latJug = Math.abs(dxJug * latVecX + dzJug * latVecZ); // Desplazamiento lateral

        // Frena si el auto principal está adelante en el mismo carril/trayectoria
        if (longJug > 0.5f && longJug < 4.2f && latJug < 1.4f) {
            return true;
        }

        // 1b. Comprobar chata de almacenamiento del jugador
        float chataX = jugX + (float) Math.sin(jugAngulo) * clase2.ChataZ;
        float chataZ = jugZ + (float) Math.cos(jugAngulo) * clase2.ChataZ;
        float dxChata = chataX - x;
        float dzChata = chataZ - z;
        float longChata = dxChata * dirVecX + dzChata * dirVecZ;
        float latChata = Math.abs(dxChata * latVecX + dzChata * latVecZ);

        if (longChata > 0.5f && longChata < 4.2f && latChata < 1.4f) {
            return true;
        }

        // 2. Comprobar otros vehículos autónomos
        for (AutoTrafico otro : otrosAutos) {
            if (otro == this) continue;
            float dxOtro = otro.x - x;
            float dzOtro = otro.z - z;
            float longOtro = dxOtro * dirVecX + dzOtro * dirVecZ;
            float latOtro = Math.abs(dxOtro * latVecX + dzOtro * latVecZ);

            // Solo detenerse si el otro vehículo está por delante en la misma trayectoria/carril
            // Evita frenar si el otro está al costado o ya pasó la intersección
            if (longOtro > 0.6f && longOtro < 4.2f && latOtro < 1.4f) {
                return true;
            }
        }

        return false;
    }

    /**
     * Mueve el vehículo autónomo hacia el siguiente punto de su ruta en la calle,
     * frenando si hay un obstáculo o vehículo adelante.
     */
    public void actualizar(float deltaTime, float jugX, float jugZ, float jugAngulo, List<AutoTrafico> otrosAutos) {
        int siguiente = (indiceRuta + 1) % ruta.length;
        float destinoX = ruta[siguiente][0];
        float destinoZ = ruta[siguiente][1];

        float dx = destinoX - x;
        float dz = destinoZ - z;
        float distancia = (float) Math.sqrt(dx * dx + dz * dz);

        if (distancia < 0.001f) {
            indiceRuta = siguiente;
            actualizarOrientacion();
            return;
        }

        float dirVecX = dx / distancia;
        float dirVecZ = dz / distancia;

        // Comprueba si el jugador u otro auto está bloqueando el paso adelante
        if (hayObstaculoAdelante(jugX, jugZ, jugAngulo, otrosAutos, dirVecX, dirVecZ)) {
            velocidadActual = 0; // Se detiene por completo respetando la colisión y distancia de seguridad
            return;
        }

        // Si el camino está despejado, recupera suavemente su velocidad
        velocidadActual = velocidadBase;
        float paso = velocidadActual * deltaTime;

        if (distancia <= paso) {
            // Llegó al cruce/esquina: posicionar en el punto exacto y cambiar al siguiente segmento
            x = destinoX;
            z = destinoZ;
            indiceRuta = siguiente;
            actualizarOrientacion();
        } else {
            // Avanza linealmente hacia el siguiente cruce sobre el asfalto
            x += dirVecX * paso;
            z += dirVecZ * paso;
        }
    }
}
