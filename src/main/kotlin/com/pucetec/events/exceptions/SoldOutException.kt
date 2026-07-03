package com.pucetec.events.exceptions

class SoldOutException (message: String = "Las entradas para el evento se encuentran agotadas.") :
        RuntimeException(message)