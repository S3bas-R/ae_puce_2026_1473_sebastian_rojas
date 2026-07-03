package com.pucetec.events.exceptions

class ReservationNotFoundException (message: String = "La reserva no fue encontrada.") :
        RuntimeException(message)