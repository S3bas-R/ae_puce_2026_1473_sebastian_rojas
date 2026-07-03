package com.pucetec.events.repositories

import com.pucetec.events.entities.Attendee
import com.pucetec.events.entities.Reservation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReservationsRepository : JpaRepository<Reservation, Long> {
    // Cuenta cuantas reservas activas tiene un asistente por su ID
    fun countByAttendeeIdAndStatus(attendeeID: Long, status: String ): Int
}

// Alias para usar de manera singular el nombre del repositorio en servicios y tests.
typealias ReservationRepository = ReservationsRepository