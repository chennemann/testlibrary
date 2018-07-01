package de.chennemann.utils.logging

import kotlin.reflect.full.companionObject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * [Lazy] delegate for retrieving a Slf4j [Logger] instance.
 *
 * Usage:  private val log by logger(javaClass)
 */
fun logger(javaClass: Class<Any>): Lazy<Logger> = lazy {
    LogManager.getLogger(unwrapCompanionClass(javaClass).name)
}

/**
 * Unwrap companion class to enclosing class given a Java class
 */
internal fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}