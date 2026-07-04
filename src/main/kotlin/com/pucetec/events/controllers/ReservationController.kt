package com.pucetec.events.controllers

import com.pucetec.events.dto.ReservationRequest
import com.pucetec.events.dto.ReservationResponse
import com.pucetec.events.services.ReservationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {
    private val logger = LoggerFactory.getLogger(ReservationController::class.java)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createReservation(@RequestBody request: ReservationRequest): ReservationResponse {
        logger.info("Recibida petición para crear una reserva")
        return reservationService.createReservation(request)
    }

    @GetMapping
    fun getAllReservations(): List<ReservationResponse> {
        logger.info("Recibida petición para listar todas las reservas")
        return reservationService.getAllReservations()
    }

    @PutMapping("/{id}/cancel")
    fun cancelReservation(@PathVariable id: Long): ReservationResponse {
        logger.info("Recibida petición para cancelar la reserva con ID: $id")
        return reservationService.cancelReservation(id)
    }
}