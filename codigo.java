import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EstacionGasolina {

    // ======== CONFIGURACION GENERAL ========
    private static final int TOTAL_BOMBAS = 4;
    private static final int TOTAL_CAJAS = 2;
    private static final int TOTAL_VEHICULOS = 8;

    // Semaforos
    private static final Semaphore bombasSemaphore = new Semaphore(TOTAL_BOMBAS, true);
    private static final Semaphore cajasSemaphore = new Semaphore(TOTAL_CAJAS, true);

    // Lock y Condition para controlar bombas libres
    private static final ReentrantLock bombasLock = new ReentrantLock(true);
    private static final Condition bombasDisponiblesCond = bombasLock.newCondition();

    // Lock para inventario
    private static final ReentrantLock inventarioLock = new ReentrantLock(true);

    // ReadWriteLock para registro de ventas
    private static final ReadWriteLock registroLock = new ReentrantReadWriteLock(true);

    // Estado compartido
    private static int bombasDisponibles = TOTAL_BOMBAS;
    private static int vehiculosEnEspera = 0;

    // Inventario inicial
    private static double inventarioRegular = 500.0;
    private static double inventarioSuper = 300.0;
    private static double inventarioDiesel = 200.0;

    // Registro de ventas
    private static final List<String> registroVentas = new ArrayList<>();

    // Estadisticas
    private static int transaccionNumero = 0;
    private static int vehiculosAtendidos = 0;
    private static double ingresosTotales = 0.0;

    // Random general
    private static final Random random = new Random();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== LLEGADA DE VEHICULOS ===");

        Thread[] vehiculos = new Thread[TOTAL_VEHICULOS];

        for (int i = 0; i < TOTAL_VEHICULOS; i++) {
            Vehiculo v = new Vehiculo(i + 1);
            vehiculos[i] = new Thread(v, "VEH-" + (i + 1));
            vehiculos[i].start();

            // Pequeña pausa para simular llegadas escalonadas
            Thread.sleep(500);
        }

        for (Thread t : vehiculos) {
            t.join();
        }

        mostrarResumenFinal();
    }

    // ======== CLASE VEHICULO ========
    static class Vehiculo implements Runnable {
        private final int id;
        private final String placas;
        private final Combustible combustible;
        private final double galones;
        private final String metodoPago;

        public Vehiculo(int id) {
            this.id = id;
            this.placas = generarPlacas(id);
            this.combustible = Combustible.values()[random.nextInt(Combustible.values().length)];
            this.galones = redondear(3 + (8 - 3) * random.nextDouble(), 1); // entre 3 y 8 galones
            this.metodoPago = random.nextBoolean() ? "TARJETA" : "EFECTIVO";
        }

        @Override
        public void run() {
            String nombre = "[VEH-" + id + "]";

            try {
                System.out.println(nombre + " Llegó | Placas: " + placas + " | Combustible: " + combustible.nombre);

                ocuparBomba(nombre);

                int tiempoCarga = (int) Math.round(galones * 2); // 2 segundos por galón
                System.out.println(nombre + " Cargando " + galones + " galones de " + combustible.nombre
                        + " (" + tiempoCarga + "s)");

                Thread.sleep(tiempoCarga * 1000L);

                double totalPagar = actualizarInventarioYCalcular(nombre);

                liberarBomba(nombre);

                System.out.println(nombre + " Dirigiendose a caja...");

                cajasSemaphore.acquire();
                try {
                    System.out.println(nombre + " Pagando Q" + String.format("%.2f", totalPagar)
                            + " | Metodo: " + metodoPago);

                    Thread.sleep(2000); // tiempo de pago

                    registrarTransaccion(placas, combustible.nombre, galones, totalPagar, metodoPago);

                    System.out.println(nombre + " Pago completado");
                } finally {
                    cajasSemaphore.release();
                }

                System.out.println(nombre + " Salió de la estacion");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(nombre + " fue interrumpido.");
            }
        }

        private void ocuparBomba(String nombre) throws InterruptedException {
            bombasLock.lock();
            try {
                while (bombasDisponibles == 0) {
                    vehiculosEnEspera++;
                    System.out.println(nombre + " Esperando bomba... | Vehiculos en espera: " + vehiculosEnEspera);
                    try {
                        bombasDisponiblesCond.await();
                    } finally {
                        vehiculosEnEspera--;
                    }
                }

                bombasSemaphore.acquire();
                bombasDisponibles--;

                System.out.println(nombre + " Ocupó bomba");
                mostrarEstadoBombas();
            } finally {
                bombasLock.unlock();
            }
        }

        private void liberarBomba(String nombre) {
            bombasLock.lock();
            try {
                bombasDisponibles++;
                bombasSemaphore.release();

                System.out.println(nombre + " Liberó bomba");
                mostrarEstadoBombas();

                bombasDisponiblesCond.signal();
            } finally {
                bombasLock.unlock();
            }
        }

        private double actualizarInventarioYCalcular(String nombre) {
            inventarioLock.lock();
            try {
                double total = redondear(galones * combustible.precio, 2);

                switch (combustible) {
                    case REGULAR:
                        inventarioRegular = redondear(inventarioRegular - galones, 1);
                        System.out.println(nombre + " Inventario REGULAR: " + inventarioRegular + " gal");
                        break;
                    case SUPER:
                        inventarioSuper = redondear(inventarioSuper - galones, 1);
                        System.out.println(nombre + " Inventario SUPER: " + inventarioSuper + " gal");
                        break;
                    case DIESEL:
                        inventarioDiesel = redondear(inventarioDiesel - galones, 1);
                        System.out.println(nombre + " Inventario DIESEL: " + inventarioDiesel + " gal");
                        break;
                }

                System.out.println(nombre + " Total a pagar: Q" + String.format("%.2f", total));
                mostrarEstadoInventario();

                return total;
            } finally {
                inventarioLock.unlock();
            }
        }
    }

    // ======== ENUM DE COMBUSTIBLES ========
    enum Combustible {
        REGULAR("REGULAR", 38.50),
        SUPER("SUPER", 39.85),
        DIESEL("DIESEL", 41.20);

        String nombre;
        double precio;

        Combustible(String nombre, double precio) {
            this.nombre = nombre;
            this.precio = precio;
        }
    }

    // ======== METODOS AUXILIARES ========
    private static void registrarTransaccion(String placas, String combustible, double galones,
                                             double total, String metodoPago) {
        registroLock.writeLock().lock();
        try {
            transaccionNumero++;
            vehiculosAtendidos++;
            ingresosTotales += total;

            String transaccion = String.format(
                    "[#%03d] %s | %s | %.1f gal | Q%.2f | %s",
                    transaccionNumero, placas, combustible, galones, total, metodoPago
            );

            registroVentas.add(transaccion);
            System.out.println("Registro actualizado: " + transaccion);

        } finally {
            registroLock.writeLock().unlock();
        }
    }

    private static void mostrarResumenFinal() {
        System.out.println("\nRESUMEN DE OPERACIONES");

        inventarioLock.lock();
        try {
            System.out.println("INVENTARIO FINAL:");
            System.out.println("REGULAR: " + inventarioRegular + " galones");
            System.out.println("SUPER: " + inventarioSuper + " galones");
            System.out.println("DIESEL: " + inventarioDiesel + " galones");
        } finally {
            inventarioLock.unlock();
        }

        registroLock.readLock().lock();
        try {
            System.out.println("\nREGISTRO DE TRANSACCIONES:");
            for (String t : registroVentas) {
                System.out.println(" " + t);
            }

            System.out.println("\nESTADISTICAS:");
            System.out.println(" Vehículos atendidos: " + vehiculosAtendidos);
            System.out.println(" Ingresos totales: Q" + String.format("%.2f", ingresosTotales));
        } finally {
            registroLock.readLock().unlock();
        }
    }

    private static void mostrarEstadoBombas() {
        System.out.println("Estado bombas -> Disponibles: " + bombasDisponibles
                + " | Ocupadas: " + (TOTAL_BOMBAS - bombasDisponibles)
                + " | En espera: " + vehiculosEnEspera);
    }

    private static void mostrarEstadoInventario() {
        System.out.println("Inventario actual -> REGULAR: " + inventarioRegular
                + " | SUPER: " + inventarioSuper
                + " | DIESEL: " + inventarioDiesel);
    }

    private static String generarPlacas(int id) {
        String[] sufijos = {"ABC", "XYZ", "DEF", "GHI", "JKL", "MNO", "PQR", "STU", "VWX", "LMN"};
        return String.format("P%03d-%s", id, sufijos[(id - 1) % sufijos.length]);
    }

    private static double redondear(double valor, int decimales) {
        double factor = Math.pow(10, decimales);
        return Math.round(valor * factor) / factor;
    }
}
