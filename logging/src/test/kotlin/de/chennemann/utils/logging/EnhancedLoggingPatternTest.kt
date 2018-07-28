package de.chennemann.utils.logging

import org.junit.jupiter.api.Test

class EnhancedLoggingPatternTest {

    private val log by logger(javaClass)

    @Test
    fun testLoggingFormat() {
        try {
            outerException()
        } catch (e: Exception) {
            log.debug("Message with Exception", e)
        }


        log.trace { "Trace Message" }
        log.debug { "Debug Message" }
        log.info { "Info Message" }
        log.warn { "Warning Message" }
        log.error { "Error Message" }
        log.fatal() { "Fatal Message" }

        log.debug { "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet." }

        log.debug { "This\nis\na\nmultiline\nstring" }
    }

    private fun outerException(): Nothing {
        try {
            nestedException()
        } catch (e: Exception) {
            throw IllegalArgumentException("Outer Exception", e)
        }
    }

    private fun nestedException(): Nothing {
        throw IllegalStateException("Nested Exception")
    }

}