package com.pucetec.events.exceptions

class AttendeeNotFoundException (message: String = "El asistente no fue entocntrado.") :
        RuntimeException(message)