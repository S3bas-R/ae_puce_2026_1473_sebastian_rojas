package com.pucetec.events.services

import com.pucetec.events.dto.AttendeeRequest
import com.pucetec.events.dto.AttendeeResponse
import com.pucetec.events.exceptions.BlankFieldException
import com.pucetec.events.mappers.toEntity
import com.pucetec.events.mappers.toResponse
import com.pucetec.events.repositories.AttendeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AttendeeService (
    private val attendeeRepository: AttendeeRepository
){
    private val logger = LoggerFactory.getLogger(AttendeeService::class.java)

    /***
     * Registra un asistente nuevo en el sistema
     */
    fun createAttendee(request: AttendeeRequest): AttendeeResponse {
        logger.info("Iniciando la creacion del asistente: ${request.name}") //Ayuda visual para el developer

        // Regla 1: Name or email no pueden estar en blanco
        if (
            request.name.isBlank() || request.email.isBlank()
        ){
            logger.warn(" Validacion fallida: Nombre o email en blanco")
            throw BlankFieldException()
        }

        val attendeeEntity = request.toEntity()
        val savedAttendee = attendeeRepository.save(attendeeEntity)
        logger.info("Asistente con Id: ${savedAttendee.id} creado exitosamente.")

        return savedAttendee.toResponse()
    }
}