package com.pucetec.events.repositories

import com.pucetec.events.entities.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventsRepository : JpaRepository<Event, Long>

// Alias para usar de manera singular el nombre del repositorio en servicios y tests.
typealias EventRepository = EventsRepository



