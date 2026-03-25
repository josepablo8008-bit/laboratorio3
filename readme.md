
# Laboratorio: Sincronización Avanzada con Locks y Conditions

## Objetivo
Implementar un sistema concurrente en Java que simule el funcionamiento de una estación de gasolina, utilizando mecanismos avanzados de sincronización para garantizar el acceso seguro a recursos compartidos.

## Descripción general
El sistema simula la llegada concurrente de múltiples vehículos a una estación de gasolina. Cada vehículo realiza las siguientes acciones:
1. Espera una bomba disponible
2. Carga combustible
3. Actualiza inventario
4. Realiza el proceso de pago
5. Registra la transacción

Para controlar estas operaciones se emplean semáforos, locks, conditions y read-write locks.

## Tecnologías utilizadas
- Lenguaje: Java
- Programación concurrente
- Semaphores
- ReentrantLock
- Condition
- ReadWriteLock

## Recursos compartidos
- Bombas de combustible
- Cajas de pago
- Inventario de combustible
- Registro de transacciones

## Cómo ejecutar el programa
```bash
javac EstacionGasolina.java
java EstacionGasolina
