/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package de.chennemann.utils.logging.converter


import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.stream.Collectors
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions
import org.apache.logging.log4j.core.layout.PatternLayout
import org.apache.logging.log4j.core.pattern.ConverterKeys
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter
import org.apache.logging.log4j.core.pattern.PatternConverter
import org.apache.logging.log4j.core.pattern.PatternFormatter
import org.apache.logging.log4j.core.pattern.PatternParser
import org.apache.logging.log4j.util.Strings


/**
 * Outputs the Throwable portion of the LoggingEvent as a full stack trace
 * unless this converter's option is 'short', where it just outputs the first line of the trace, or if
 * the number of lines to print is explicitly specified.
 */
@Plugin(name = "EnhancedThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys("eEx", "eThrowable", "eException")
class EnhancedThrowablePatternConverter
/**
 * Constructor.
 * @param name Name of converter.
 * @param style CSS style for output.
 * @param options options, may be null.
 */
private constructor(name: String, style: String, options: Array<String>?, config: Configuration) : LogEventPatternConverter(name, style) {

    /**
     * Lists [PatternFormatter]s for the suffix attribute.
     */
    private val formatters: List<PatternFormatter>
    private var rawOption: String? = null

    /**
     * Options.
     */
    private val options: ThrowableFormatOptions

    private val padding = "\t\t"

    private val isSubShortOption: Boolean = ThrowableFormatOptions.MESSAGE.equals(rawOption, ignoreCase = true) ||
            ThrowableFormatOptions.LOCALIZED_MESSAGE.equals(rawOption, ignoreCase = true) ||
            ThrowableFormatOptions.FILE_NAME.equals(rawOption, ignoreCase = true) ||
            ThrowableFormatOptions.LINE_NUMBER.equals(rawOption, ignoreCase = true) ||
            ThrowableFormatOptions.METHOD_NAME.equals(rawOption, ignoreCase = true) ||
            ThrowableFormatOptions.CLASS_NAME.equals(rawOption, ignoreCase = true)

    init {
        this.options = ThrowableFormatOptions.newInstance(options)
        if (options != null && options.size > 0) {
            rawOption = options[0]
        }
        if (this.options.suffix != null) {
            val parser = PatternLayout.createPatternParser(config)
            val parsedSuffixFormatters = parser.parse(this.options.suffix)
            // filter out nested formatters that will handle throwable
            var hasThrowableSuffixFormatter = false
            for (suffixFormatter in parsedSuffixFormatters) {
                if (suffixFormatter.handlesThrowable()) {
                    hasThrowableSuffixFormatter = true
                }
            }
            if (!hasThrowableSuffixFormatter) {
                this.formatters = parsedSuffixFormatters
            } else {
                val suffixFormatters = ArrayList<PatternFormatter>()
                for (suffixFormatter in parsedSuffixFormatters) {
                    if (!suffixFormatter.handlesThrowable()) {
                        suffixFormatters.add(suffixFormatter)
                    }
                }
                this.formatters = suffixFormatters
            }
        } else {
            this.formatters = emptyList()
        }

    }

    /**
     * {@inheritDoc}
     */
    override fun format(event: LogEvent, buffer: StringBuilder) {
        val t = event.thrown

        appendIfThrowableExists(t, buffer)

        if (isSubShortOption) {
            formatSubShortOption(t, getSuffix(event), buffer)
        } else if (t != null && options.anyLines()) {
            formatOption(t, getSuffix(event), buffer)
        }

        appendIfThrowableExists(t, buffer)
    }

    private fun appendIfThrowableExists(t: Throwable?, buffer: StringBuilder) {
        if (t != null) {
            buffer.append(Strings.LINE_SEPARATOR)
        }
    }

    private fun formatSubShortOption(t: Throwable?, suffix: String, buffer: StringBuilder) {
        val trace: Array<StackTraceElement>?
        var throwingMethod: StackTraceElement? = null
        val len: Int

        if (t != null) {
            trace = t.stackTrace
            if (trace != null && trace.size > 0) {
                throwingMethod = trace[0]
            }
        }

        if (t != null && throwingMethod != null) {
            var toAppend = Strings.EMPTY

            if (ThrowableFormatOptions.CLASS_NAME.equals(rawOption!!, ignoreCase = true)) {
                toAppend = throwingMethod.className
            } else if (ThrowableFormatOptions.METHOD_NAME.equals(rawOption!!, ignoreCase = true)) {
                toAppend = throwingMethod.methodName
            } else if (ThrowableFormatOptions.LINE_NUMBER.equals(rawOption!!, ignoreCase = true)) {
                toAppend = throwingMethod.lineNumber.toString()
            } else if (ThrowableFormatOptions.MESSAGE.equals(rawOption!!, ignoreCase = true)) {
                toAppend = t.message ?: "null"
            } else if (ThrowableFormatOptions.LOCALIZED_MESSAGE.equals(rawOption!!, ignoreCase = true)) {
                toAppend = t.localizedMessage
            } else if (ThrowableFormatOptions.FILE_NAME.equals(rawOption!!, ignoreCase = true)) {
                toAppend = throwingMethod.fileName
            }

            len = buffer.length
            if (len > 0 && !Character.isWhitespace(buffer[len - 1])) {
                buffer.append(' ')
            }
            buffer.append(toAppend)

            if (Strings.isNotBlank(suffix)) {
                buffer.append(' ')
                buffer.append(suffix)
            }
        }
    }

    private fun formatOption(throwable: Throwable, suffix: String, buffer: StringBuilder) {
        val w = StringWriter()

        throwable.printStackTrace(PrintWriter(w))
        val len = buffer.length
        if (len > 0 && !Character.isWhitespace(buffer[len - 1])) {
            buffer.append(' ')
        }

        val padded = w
            .toString()
            .split(Strings.LINE_SEPARATOR.toRegex())
            .dropLastWhile { it.isEmpty() }
            .map { it -> padding + it }
            .toTypedArray()

        val resultBuilder = StringBuilder()
        if (!options.allLines() || Strings.LINE_SEPARATOR != options.separator || Strings.isNotBlank(suffix)) {
            val limit = options.minLines(padded.size) - 1
            val suffixNotBlank = Strings.isNotBlank(suffix)
            for (i in 0..limit) {
                resultBuilder.append(padded[i])
                if (suffixNotBlank) {
                    resultBuilder.append(' ')
                    resultBuilder.append(suffix)
                }
                if (i < limit) {
                    resultBuilder.append(options.separator)
                }
            }
        } else {

            val lineCount = padded.size
            for (i in 0 until lineCount) {
                val appendingLine = padded[i]

                resultBuilder.append(appendingLine)
                if (i < lineCount - 1) {
                    resultBuilder.append(Strings.LINE_SEPARATOR)
                }
            }
        }
        resultBuilder.append(Strings.LINE_SEPARATOR)
        buffer.append(resultBuilder.toString())
    }

    /**
     * This converter obviously handles throwables.
     *
     * @return true.
     */
    override fun handlesThrowable(): Boolean {
        return true
    }

    private fun getSuffix(event: LogEvent): String {

        val toAppendTo = StringBuilder()
        for (formatter in formatters) {
            formatter.format(event, toAppendTo)
        }
        return toAppendTo.toString()
    }

    companion object {

        /**
         * Gets an instance of the class.
         *
         * @param config Configuration of the Converter
         * @param options pattern options, may be null.  If first element is "short",
         * only the first line of the throwable will be formatted.
         * @return instance of class.
         */
        @JvmStatic
        fun newInstance(config: Configuration, options: Array<String>): EnhancedThrowablePatternConverter {
            return EnhancedThrowablePatternConverter("Throwable", "throwable", options, config)
        }
    }

}
