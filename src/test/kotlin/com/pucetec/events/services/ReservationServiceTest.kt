package com.pucetec.events.services

import com.pucetec.events.dto.ReservationRequest
import com.pucetec.events.entities.Attendee
import com.pucetec.events.entities.Event
import com.pucetec.events.entities.Reservation
import com.pucetec.events.exceptions.AttendeeNotFoundException
import com.pucetec.events.exceptions.EventNotFoundException
import com.pucetec.events.exceptions.ReservationAlreadyCancelledException
import com.pucetec.events.exceptions.ReservationLimitExceededException
import com.pucetec.events.exceptions.ReservationNotFoundException
import com.pucetec.events.exceptions.SoldOutException
import com.pucetec.events.repositories.AttendeeRepository
import com.pucetec.events.repositories.EventRepository
import com.pucetec.events.repositories.ReservationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ReservationServiceTest {

    @Mock
    lateinit var reservationRepository: ReservationRepository

    @Mock
    lateinit var eventRepository: EventRepository

    @Mock
    lateinit var attendeeRepository: AttendeeRepository

    @InjectMocks
    lateinit var reservationService: ReservationService

    // --- TESTS PARA createReservation ---

    @Test
    fun `cuando el asistente no existe lanza AttendeeNotFoundException`() {
        // Arrange
        val request = ReservationRequest(attendeeId = 1L, eventId = 2L)
        whenever(attendeeRepository.findById(1L)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows(AttendeeNotFoundException::class.java) {
            reservationService.createReservation(request)
        }
    }

    @Test
    fun `cuando el evento no existe lanza EventNotFoundException`() {
        // Arrange
        val request = ReservationRequest(attendeeId = 1L, eventId = 2L)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        whenever(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee))
        whenever(eventRepository.findById(2L)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows(EventNotFoundException::class.java) {
            reservationService.createReservation(request)
        }
    }

    @Test
    fun `cuando el evento no tiene entradas disponibles lanza SoldOutException`() {
        // Arrange
        val request = ReservationRequest(attendeeId = 1L, eventId = 2L)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        val event = Event(id = 2L, name = "C1", venue = "V1", totalTickets = 10, availableTickets = 0)
        whenever(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee))
        whenever(eventRepository.findById(2L)).thenReturn(Optional.of(event))

        // Act & Assert
        assertThrows(SoldOutException::class.java) {
            reservationService.createReservation(request)
        }
    }

    @Test
    fun `cuando el asistente supera el limite de 4 reservas activas lanza ReservationLimitExceededException`() {
        // Arrange
        val request = ReservationRequest(attendeeId = 1L, eventId = 2L)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        val event = Event(id = 2L, name = "C1", venue = "V1", totalTickets = 10, availableTickets = 5)
        whenever(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee))
        whenever(eventRepository.findById(2L)).thenReturn(Optional.of(event))
        whenever(reservationRepository.countByAttendeeIdAndStatus(1L, "ACTIVE")).thenReturn(4)

        // Act & Assert
        assertThrows(ReservationLimitExceededException::class.java) {
            reservationService.createReservation(request)
        }
    }

    @Test
    fun `cuando la reserva es valida se descuentan tickets y se guarda exitosamente`() {
        // Arrange
        val request = ReservationRequest(attendeeId = 1L, eventId = 2L)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        val event = Event(id = 2L, name = "C1", venue = "V1", totalTickets = 10, availableTickets = 5)
        val savedReservation = Reservation(id = 10L, status = "ACTIVE", createdAt = LocalDateTime.now(), event = event, attendee = attendee)

        whenever(attendeeRepository.findById(1L)).thenReturn(Optional.of(attendee))
        whenever(eventRepository.findById(2L)).thenReturn(Optional.of(event))
        whenever(reservationRepository.countByAttendeeIdAndStatus(1L, "ACTIVE")).thenReturn(2)
        whenever(reservationRepository.save(any())).thenReturn(savedReservation)

        // Act
        val response = reservationService.createReservation(request)

        // Assert
        assertEquals(10L, response.id)
        assertEquals("ACTIVE", response.status)
        assertEquals(4, event.availableTickets) // Se decrementó de 5 a 4
        verify(eventRepository).save(event) // Verifica que se guardó el evento actualizado
    }

    // --- TESTS PARA cancelReservation ---

    @Test
    fun `cuando la reserva a cancelar no existe lanza ReservationNotFoundException`() {
        // Arrange
        whenever(reservationRepository.findById(99L)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows(ReservationNotFoundException::class.java) {
            reservationService.cancelReservation(99L)
        }
    }

    @Test
    fun `cuando la reserva ya esta cancelada lanza ReservationAlreadyCancelledException`() {
        // Arrange
        val event = Event(id = 2L, name = "C1", venue = "V1", totalTickets = 10, availableTickets = 5)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        val reservation = Reservation(id = 10L, status = "CANCELLED", event = event, attendee = attendee)
        whenever(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation))

        // Act & Assert
        assertThrows(ReservationAlreadyCancelledException::class.java) {
            reservationService.cancelReservation(10L)
        }
    }

    @Test
    fun `cuando la cancelacion es valida cambia status a CANCELLED e incrementa tickets`() {
        // Arrange
        val event = Event(id = 2L, name = "C1", venue = "V1", totalTickets = 10, availableTickets = 5)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        val reservation = Reservation(id = 10L, status = "ACTIVE", event = event, attendee = attendee)

        whenever(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation))
        whenever(reservationRepository.save(any())).thenReturn(reservation)

        // Act
        val response = reservationService.cancelReservation(10L)

        // Assert
        assertEquals("CANCELLED", response.status)
        assertEquals(6, event.availableTickets) // Se incrementó de 5 a 6
        verify(reservationRepository).save(reservation)
        verify(eventRepository).save(event)
    }

    // --- TESTS PARA getAllReservations ---

    @Test
    fun `cuando se listan todas las reservas retorna la lista mapeada`() {
        // Arrange
        val event = Event(id = 2L, name = "C1", venue = "V1", totalTickets = 10, availableTickets = 5)
        val attendee = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")
        val list = listOf(
            Reservation(id = 10L, status = "ACTIVE", event = event, attendee = attendee),
            Reservation(id = 11L, status = "CANCELLED", event = event, attendee = attendee)
        )
        whenever(reservationRepository.findAll()).thenReturn(list)

        // Act
        val result = reservationService.getAllReservations()

        // Assert
        assertEquals(2, result.size)
        assertEquals("ACTIVE", result[0].status)
        assertEquals("CANCELLED", result[1].status)
    }
}
