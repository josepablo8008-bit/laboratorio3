### Lock y Condition para bombas

Se utiliza un ReentrantLock junto con una Condition para manejar la espera de los vehículos cuando no hay bombas disponibles.

Cuando todas las bombas están ocupadas:
- El vehículo entra en espera usando await()
- Se libera el lock de forma segura
- Al liberarse una bomba, se notifica con signal()

Esto evita el uso de busy waiting y mejora la eficiencia del sistema.
