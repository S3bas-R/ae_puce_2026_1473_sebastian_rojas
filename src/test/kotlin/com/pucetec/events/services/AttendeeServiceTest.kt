package com.pucetec.events.services

import com.pucetec.events.dto.AttendeeRequest
import com.pucetec.events.dto.AttendeeResponse
import com.pucetec.events.entities.Attendee
import com.pucetec.events.exceptions.BlankFieldException
import com.pucetec.events.repositories.AttendeeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AttendeeServiceTest {

    @Mock
    lateinit var attendeeRepository: AttendeeRepository

    @InjectMocks
    lateinit var attendeeService: AttendeeService

    // --- TESTS REGLAS DE NEGOCIO ---

    // Regla 1: Nombre o email en blanco lanza BlankFieldException
    @Test
    fun `cuando nombre esta en blanco lanza BlankFieldException`() {
        // Arrange
        val request = AttendeeRequest(name = "", email = "lobo@test.com")

        // Act & Assert
        assertThrows(BlankFieldException::class.java) {
            attendeeService.createAttendee(request)
        }
    }

    @Test
    fun `cuando email esta en blanco lanza BlankFieldException`() {
        // Arrange
        val request = AttendeeRequest(name = "Lobo", email = " ")

        // Act & Assert
        assertThrows(BlankFieldException::class.java) {
            attendeeService.createAttendee(request)
        }
    }

    // Camino feliz: Guarda exitosamente y mapea a DTO
    @Test
    fun `cuando datos son validos crea el asistente exitosamente`() {
        // Arrange
        val request = AttendeeRequest(name = "Lobo", email = "lobo@test.com")
        val savedEntity = Attendee(id = 1L, name = "Lobo", email = "lobo@test.com")

        whenever(attendeeRepository.save(any())).thenReturn(savedEntity)

        // Act
        val response = attendeeService.createAttendee(request)

        // Assert
        assertEquals(1L, response.id)
        assertEquals("Lobo", response.name)
        assertEquals("lobo@test.com", response.email)
    }
}
