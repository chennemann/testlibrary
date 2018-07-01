package de.chennemann.utils.extensions

import org.amshove.kluent.shouldBe
import org.junit.Assert
import org.junit.jupiter.api.Test

class BooleanExtensionsTest {

    @Test
    fun ifTrue_true() {
        val boolean = true

        var result = false
        boolean
            .ifTrue { result = true }
            .ifFalse { Assert.fail() }

        result shouldBe true
    }

    @Test
    fun ifTrue_false() {
        val boolean = false

        var result = false
        boolean
            .ifTrue { Assert.fail() }
            .ifFalse { result = true }

        result shouldBe true
    }

    @Test
    fun ifTrue_null() {
        val boolean = null

        boolean
            .ifTrue { Assert.fail() }
            .ifFalse { Assert.fail() }

    }

    @Test
    fun runIfTrue_true() {
        val boolean = true

        boolean.runIfTrue { true } shouldBe true
        boolean.runIfFalse { Assert.fail() }

    }

    @Test
    fun runIfTrue_false() {
        val boolean = false

        boolean.runIfTrue { Assert.fail() }
        boolean.runIfFalse { true } shouldBe true

    }

    @Test
    fun runIfTrue_null() {
        val boolean = null

        boolean.runIfTrue { Assert.fail() }
        boolean.runIfFalse { Assert.fail() }

    }

}