package com.pucetec.events.exceptions

class EventNotFoundException (message: String = "El evento no fue entocntrado.") :
        RuntimeException(message)