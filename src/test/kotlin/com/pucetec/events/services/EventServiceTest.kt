package com.pucetec.events.services

import com.pucetec.events.dto.EventRequest
import com.pucetec.events.dto.EventResponse
import com.pucetec.events.entities.Event
import com.pucetec.events.exceptions.BlankFieldException
import com.pucetec.events.exceptions.EventNotFoundException
import com.pucetec.events.exceptions.InvalidCapacityException
import com.pucetec.events.repositories.EventRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class EventServiceTest {

    @Mock
    lateinit var eventRepository: EventRepository

    @InjectMocks
    lateinit var eventService: EventService

    // --- TESTS PARA createEvent ---

    @Test
    fun `cuando nombre esta en blanco lanza BlankFieldException al crear evento`() {
        // Arrange
        val request = EventRequest(name = "", venue = "Teatro", totalTickets = 100)

        // Act & Assert
        assertThrows(BlankFieldException::class.java) {
            eventService.createEvent(request)
        }
    }

    @Test
    fun `cuando venue esta en blanco lanza BlankFieldException al crear evento`() {
        // Arrange
        val request = EventRequest(name = "Concierto", venue = " ", totalTickets = 100)

        // Act & Assert
        assertThrows(BlankFieldException::class.java) {
            eventService.createEvent(request)
        }
    }

    @Test
    fun `cuando totalTickets es menor a 1 lanza InvalidCapacityException`() {
        // Arrange
        val request = EventRequest(name = "Concierto", venue = "Teatro", totalTickets = 0)

        // Act & Assert
        assertThrows(InvalidCapacityException::class.java) {
            eventService.createEvent(request)
        }
    }

    @Test
    fun `cuando datos son validos crea el evento exitosamente`() {
        // Arrange
        val request = EventRequest(name = "Concierto", venue = "Teatro", totalTickets = 100)
        val savedEntity = Event(id = 1L, name = "Concierto", venue = "Teatro", totalTickets = 100, availableTickets = 100)

        whenever(eventRepository.save(any())).thenReturn(savedEntity)

        // Act
        val response = eventService.createEvent(request)

        // Assert
        assertEquals(1L, response.id)
        assertEquals("Concierto", response.name)
        assertEquals("Teatro", response.venue)
        assertEquals(100, response.totalTickets)
        assertEquals(100, response.availableTickets)
    }

    // --- TESTS PARA getAllEvents ---

    @Test
    fun `cuando se listan eventos retorna todos los registrados`() {
        // Arrange
        val list = listOf(
            Event(id = 1L, name = "E1", venue = "V1", totalTickets = 10, availableTickets = 10),
            Event(id = 2L, name = "E2", venue = "V2", totalTickets = 20, availableTickets = 15)
        )
        whenever(eventRepository.findAll()).thenReturn(list)

        // Act
        val result = eventService.getAllEvents()

        // Assert
        assertEquals(2, result.size)
        assertEquals("E1", result[0].name)
        assertEquals("E2", result[1].name)
    }

    // --- TESTS PARA getEventById ---

    @Test
    fun `cuando el evento existe retorna su detalle`() {
        // Arrange
        val event = Event(id = 1L, name = "Concierto", venue = "Teatro", totalTickets = 100, availableTickets = 100)
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event))

        // Act
        val response = eventService.getEventById(1L)

        // Assert
        assertEquals(1L, response.id)
        assertEquals("Concierto", response.name)
    }

    @Test
    fun `cuando el evento no existe lanza EventNotFoundException`() {
        // Arrange
        whenever(eventRepository.findById(99L)).thenReturn(Optional.empty())

        // Act & Assert
        assertThrows(EventNotFoundException::class.java) {
            eventService.getEventById(99L)
        }
    }
}
