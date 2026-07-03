package com.pucetec.events.exceptions

/**
 * BlankFieldException: Se lanza cuando un campo obligatorio está vacío o en blanco.
 * Retorna HTTP 400.
 */
class BlankFieldException(message: String = "El campo obligatorio no puede estar vacío") :
    RuntimeException(message)