### Protección del inventario

El inventario de gasolina es un recurso compartido que se modifica por múltiples hilos. Para evitar inconsistencias, se protege usando un ReentrantLock, asegurando que solo un vehículo pueda modificar el inventario a la vez.
``
