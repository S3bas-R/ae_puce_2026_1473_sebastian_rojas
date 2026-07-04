package com.pucetec.events.services

import com.pucetec.events.dto.ReservationRequest
import com.pucetec.events.dto.ReservationResponse
import com.pucetec.events.entities.Reservation
import com.pucetec.events.exceptions.AttendeeNotFoundException
import com.pucetec.events.exceptions.EventNotFoundException
import com.pucetec.events.exceptions.ReservationAlreadyCancelledException
import com.pucetec.events.exceptions.ReservationLimitExceededException
import com.pucetec.events.exceptions.ReservationNotFoundException
import com.pucetec.events.exceptions.SoldOutException
import com.pucetec.events.mappers.toResponse
import com.pucetec.events.repositories.AttendeeRepository
import com.pucetec.events.repositories.AttendeesRepository
import com.pucetec.events.repositories.EventRepository
import com.pucetec.events.repositories.ReservationRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ReservationService (
    private val reservationRepository: ReservationRepository,
    private val eventRepository: EventRepository,
    private val attendeeRepository: AttendeeRepository
) {
    private val logger = LoggerFactory.getLogger(ReservationService::class.java)

    /***
     * Genera una reserva de entrada para un asistente en un evento
     */
    @Transactional
    fun createReservation(request: ReservationRequest): ReservationResponse {
        logger.info("Iniciando reserva para Attendee ID: ${request.attendeeId} en Event ID: ${request.eventId}")

        // Regla 1: El asistente debe existir -> AttendeeNotFoundException
        val attendee = attendeeRepository.findById(request.attendeeId).orElseThrow {
            logger.warn("No se encontró el asistente con ID: ${request.attendeeId}")
            throw AttendeeNotFoundException()
        }

        // Regla 2: El evento debe existir -> EventNotFoundException
        val event = eventRepository.findById(request.eventId).orElseThrow {
            logger.warn("No se encontró el evento con ID: ${request.eventId}")
            throw EventNotFoundException()
        }

        // Regla 3: El evento debe tener availableTickets > 0 -> SoldOutException
        if (event.availableTickets <= 0) {
            logger.warn("El evento con ID: ${event.id} no tiene entradas disponibles")
            throw SoldOutException()
        }

        // Regla 4: El asistente no puede tener más de 4 reservas ACTIVE -> ReservationLimitExceededException
        val activeReservationsCount = reservationRepository.countByAttendeeIdAndStatus(attendee.id, "ACTIVE")
        if (activeReservationsCount >= 4) {
            logger.warn("El asistente con ID: ${attendee.id} superó el límite de reservas activas")
            throw ReservationLimitExceededException()
        }

        // Si todo es válido:
        // - Decrementamos las entradas del evento
        event.availableTickets -= 1
        eventRepository.save(event)

        // - Creamos y guardamos la reserva (status = ACTIVE, createdAt = ahora)
        val newReservation = Reservation(
            status = "ACTIVE",
            createdAt = LocalDateTime.now(),
            event = event,
            attendee = attendee
        )
        val savedReservation = reservationRepository.save(newReservation)
        logger.info("Reserva creada con éxito. ID: ${savedReservation.id}")

        return savedReservation.toResponse()
    }

    /**
     * Cancela una reserva activa de entradas.
     */
    @Transactional
    fun cancelReservation(reservationId: Long): ReservationResponse {
        logger.info("Iniciando la cancelación de la reserva con ID: $reservationId")

        // Regla 1: La reserva debe existir -> ReservationNotFoundException
        val reservation = reservationRepository.findById(reservationId).orElseThrow {
            logger.warn("No se encontró la reserva con ID: $reservationId")
            throw ReservationNotFoundException()
        }

        // Regla 2: La reserva debe estar ACTIVE -> ReservationAlreadyCancelledException
        if (reservation.status == "CANCELLED") {
            logger.warn("La reserva con ID: $reservationId ya se encuentra cancelada")
            throw ReservationAlreadyCancelledException()
        }

        // Si todo es válido:
        // - status = CANCELLED
        reservation.status = "CANCELLED"
        val updatedReservation = reservationRepository.save(reservation)

        // - incrementamos availableTickets en 1
        val event = reservation.event
        event.availableTickets += 1
        eventRepository.save(event)

        logger.info("Reserva con ID: $reservationId cancelada correctamente")
        return updatedReservation.toResponse()
    }

    /**
     * Lista todas las reservas del sistema.
     */
    fun getAllReservations(): List<ReservationResponse> {
        logger.info("Obteniendo la lista de todas las reservas")
        val reservations = reservationRepository.findAll()
        return reservations.map { it.toResponse() }
    }
}