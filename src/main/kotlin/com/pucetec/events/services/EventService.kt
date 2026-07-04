package com.pucetec.events.services

import com.pucetec.events.dto.EventRequest
import com.pucetec.events.dto.EventResponse
import com.pucetec.events.exceptions.BlankFieldException
import com.pucetec.events.exceptions.EventNotFoundException
import com.pucetec.events.exceptions.InvalidCapacityException
import com.pucetec.events.mappers.toEntity
import com.pucetec.events.mappers.toResponse
import com.pucetec.events.repositories.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventService (
    private val eventRepository : EventRepository
){
    private val logger = LoggerFactory.getLogger(EventService::class.java)

    /***
     * Registra un nuevo evento en el catalogo si es que cumple con las reglas de validacion
     */
    fun createEvent(request: EventRequest): EventResponse {
        logger.info("Iniciando la creacion del evento: ${request.name}")

        // Regla 1: name or venue estan en blanco -> BlankFielException
        if (
            request.name.isBlank() || request.venue.isBlank()
        ){
            logger.warn("Validacion fallida: Nombre o lugar en blanco")
            throw BlankFieldException()
        }

        // Regla 2: totalTickets menor a 1 -> InvalidCapacityExcetpion
        if (
            request.totalTickets < 1
            ){
            logger.warn("Validacion fallida: Capacidad de tickets invalida (${request.totalTickets}")
            throw InvalidCapacityException()
        }

        val eventEntity = request.toEntity()
        val savedEvent = eventRepository.save(eventEntity)
        logger.info("Evento guardado con ID: ${savedEvent.id}")
        return savedEvent.toResponse()
    }

    /***
     * Retorna todos los eventos registrados
     */
    fun getAllEvents() : List<EventResponse> {
        logger.info("Obtenendo la lista de eventos")
        val events = eventRepository.findAll()
        return events.map { it.toResponse() }
    }

    /***
     * Buscar un evento especifico por su ID
     */
    fun getEventById(id: Long) : EventResponse {
        logger.info("Buscando evento con ID: $id")
        val event = eventRepository.findById(id).orElseThrow {
            logger.warn("No se encontro el evento con ID: $id")
            throw EventNotFoundException()
        }
        return event.toResponse()
    }
}