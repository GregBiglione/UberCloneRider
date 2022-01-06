package com.greg.uberclonerider

object Common {
    var currentRider: Rider? = null

    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome")
            .append(" ")
            .append(currentRider!!.firstName!!.trim())
            .append(" ")
            .append(currentRider!!.lastName!!.trim())
            .toString()
    }
}