package de.chennemann.utils.extensions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NullableTypeExtensionsTest {

    lateinit var performingMock: PerformingMock

    @BeforeEach
    fun setup() {
        performingMock = mock()
        whenever(performingMock.presentBlock(any())).thenReturn("presentBlock")
        whenever(performingMock.elseBlock()).thenReturn("elseBlock")
    }

    @Test
    fun ifPresent_onExisting() {
        val result = "existingType"
            .ifPresent { it -> performingMock.presentBlock(it) }
            .orElse { performingMock.elseBlock() }

        result `should be equal to` "presentBlock"

        verify(performingMock).presentBlock(any())
        verifyNoMoreInteractions(performingMock)
    }


    @Test
    fun else_onNonExisting() {
        var any: Any? = null
        val result = any
            .ifPresent { it -> performingMock.presentBlock(it) }
            .orElse { performingMock.elseBlock() }

        result `should be equal to` "elseBlock"

        verify(performingMock).elseBlock()
        verifyNoMoreInteractions(performingMock)
    }


    interface PerformingMock {
        fun presentBlock(any: Any): String
        fun elseBlock(): String
    }
}