package com.pucetec.events.mappers

import com.pucetec.events.dto.AttendeeRequest
import com.pucetec.events.dto.AttendeeResponse
import com.pucetec.events.entities.Attendee

fun AttendeeRequest.toEntity() = Attendee (
    name = this.name,
    email = this.email
)

fun Attendee.toResponse() = AttendeeResponse (
    id = this.id,
    name = this.name,
    email = this.email
)