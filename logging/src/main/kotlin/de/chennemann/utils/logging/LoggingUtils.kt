package de.chennemann.utils.logging

import org.apache.logging.log4j.kotlin.KotlinLogger
import org.apache.logging.log4j.kotlin.loggerOf

/**
 * [Lazy] delegate for retrieving a Slf4j [Logger] instance.
 *
 * Usage:  private val log by logger(javaClass)
 */
fun logger(javaClass: Class<Any>): Lazy<KotlinLogger> = lazy {
    loggerOf(javaClass)
}
