package com.pucetec.events.services

import com.pucetec.events.repositories.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EventService (
    private val eventRepository : EventRepository
){
    private val logger = LoggerFactory.getLogger(EventService::class.java)

}