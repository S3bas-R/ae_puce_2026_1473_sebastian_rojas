package com.pucetec.events.exceptions

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Molde de datos para enviar errores estructurados al cliente.
 */
data class ExceptionResponse (
    val message: String?,
    val source: String
)


/***
 * El RestControllerAdvice es el que vigila al sistema
 * Intercepta los errores de los controladores y genera respuestas usando 'Exception Response'
 */

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // --- ERROR 400: Bad Request

    @ExceptionHandler(BlankFieldException::class)
    fun handleBlankField(ex: BlankFieldException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción de validación: BlankFieldException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Validation"), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(InvalidCapacityException::class)
    fun handleInvalidCapacity(ex: InvalidCapacityException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción de validación: InvalidCapacityException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Validation"), HttpStatus.BAD_REQUEST)
    }

    // --- ERROR 404: Not Found ---

    @ExceptionHandler(AttendeeNotFoundException::class)
    fun handleAttendeeNotFound(ex: AttendeeNotFoundException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción detectada: AttendeeNotFoundException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Database"), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(EventNotFoundException::class)
    fun handleEventNotFound(ex: EventNotFoundException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción detectada: EventNotFoundException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Database"), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ReservationNotFoundException::class)
    fun handleReservationNotFound(ex: ReservationNotFoundException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción detectada: ReservationNotFoundException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Database"), HttpStatus.NOT_FOUND)
    }

    // --- ERROR 409: CONFLICT ---

    @ExceptionHandler(SoldOutException::class)
    fun handleSoldOut(ex: SoldOutException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción de negocio: SoldOutException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Business Logic"), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ReservationLimitExceededException::class)
    fun handleReservationLimitExceeded(ex: ReservationLimitExceededException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción de negocio: ReservationLimitExceededException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Business Logic"), HttpStatus.CONFLICT)
    }

    @ExceptionHandler(ReservationAlreadyCancelledException::class)
    fun handleReservationAlreadyCancelled(ex: ReservationAlreadyCancelledException): ResponseEntity<ExceptionResponse> {
        logger.warn("Excepción de negocio: ReservationAlreadyCancelledException - Mensaje: ${ex.message}")
        return ResponseEntity(ExceptionResponse(ex.message, "Business Logic"), HttpStatus.CONFLICT)
    }

    // --- CATCH-ALL ERRORES 500 ---

    @ExceptionHandler(Exception::class)
    fun handleAllUncaughtExceptions(ex: Exception): ResponseEntity<ExceptionResponse> {
        logger.error("Excepción interna del servidor no controlada: ", ex)
        return ResponseEntity(
            ExceptionResponse("Ocurrió un error inesperado en el servidor. Por favor, comuníquese con soporte técnico.", "Server Error"),
            HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}