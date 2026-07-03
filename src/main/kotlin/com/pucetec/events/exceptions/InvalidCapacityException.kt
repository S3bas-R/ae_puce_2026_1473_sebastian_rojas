package com.pucetec.events.exceptions

/**
 * InvalidCapacityException: Se lanza si la capacidad total de tickets es menor a 1.
 * Retorna HTTP 400.
 */
class InvalidCapacityException(message: String =
"La capacidad total de tickets debe ser al menos 1") : RuntimeException(message)