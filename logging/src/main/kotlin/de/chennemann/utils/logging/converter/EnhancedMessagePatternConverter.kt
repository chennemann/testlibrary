package de.chennemann.utils.logging.converter


import java.text.BreakIterator
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.Locale
import java.util.StringTokenizer
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.config.Configuration
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.pattern.ConverterKeys
import org.apache.logging.log4j.core.pattern.HtmlTextRenderer
import org.apache.logging.log4j.core.pattern.JAnsiTextRenderer
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter
import org.apache.logging.log4j.core.pattern.PatternConverter
import org.apache.logging.log4j.core.pattern.TextRenderer
import org.apache.logging.log4j.core.util.ArrayUtils
import org.apache.logging.log4j.core.util.Constants
import org.apache.logging.log4j.core.util.Loader
import org.apache.logging.log4j.message.Message
import org.apache.logging.log4j.message.MultiformatMessage
import org.apache.logging.log4j.status.StatusLogger
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable
import org.apache.logging.log4j.util.PerformanceSensitive
import org.apache.logging.log4j.util.ProcessIdUtil
import org.apache.logging.log4j.util.StringBuilderFormattable
import org.apache.logging.log4j.util.Strings


/**
 * Returns the event's rendered message in a StringBuilder.
 */
@Plugin(name = "EnhancedMessagePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys("wMessage")
@PerformanceSensitive("allocation")
class EnhancedMessagePatternConverter
/**
 * Private constructor.
 *
 * @param options options, may be null.
 */
private constructor(private val config: Configuration?, private val formats: Array<String>) : LogEventPatternConverter(
    "Message",
    "message"
) {
    private val textRenderer: TextRenderer?
    private val noLookups: Boolean

    private val padding: String


    init {
        val noLookupsIdx = loadNoLookups(formats)
        this.noLookups = Constants.FORMAT_MESSAGES_PATTERN_DISABLE_LOOKUPS || noLookupsIdx >= 0
        this.textRenderer = loadMessageRenderer(if (noLookupsIdx >= 0) ArrayUtils.remove(formats, noLookupsIdx) else formats)

        val discoveredPid = ProcessIdUtil.getProcessId()
        val paddingSize = 85 + discoveredPid.length
        val paddingBuilder = StringBuilder(paddingSize)
        for (i in 0 until paddingSize) {
            paddingBuilder.append(" ")
        }
        padding = paddingBuilder.toString()
    }


    private fun loadNoLookups(options: Array<String>?): Int {
        if (options != null) {
            for (i in options.indices) {
                val option = options[i]
                if (NOLOOKUPS.equals(option, ignoreCase = true)) {
                    return i
                }
            }
        }
        return -1
    }


    private fun loadMessageRenderer(options: Array<String>?): TextRenderer? {
        if (options != null) {
            for (option in options) {
                when (option.toUpperCase(Locale.ROOT)) {
                    "ANSI" -> {
                        if (Loader.isJansiAvailable()) {
                            return JAnsiTextRenderer(options, mutableMapOf())
                        }
                        StatusLogger.getLogger()
                            .warn("You requested ANSI message rendering but JANSI is not on the classpath.")
                        return null
                    }
                    "HTML" -> return HtmlTextRenderer(options)
                }
            }
        }
        return null
    }


    /**
     * {@inheritDoc}
     */
    override fun format(event: LogEvent, resultBuilder: StringBuilder) {


        val msg = event.message
        if (msg != null) {
            val toAppendTo = StringBuilder()

            if (msg is StringBuilderFormattable) {

                val doRender = textRenderer != null
                val workingBuilder = if (doRender) StringBuilder(80) else toAppendTo

                val offset = workingBuilder.length
                if (msg is MultiFormatStringBuilderFormattable) {
                    msg.formatTo(formats, workingBuilder)
                } else {
                    (msg as StringBuilderFormattable).formatTo(workingBuilder)
                }

                // TODO can we optimize this?
                if (config != null && !noLookups) {
                    for (i in offset until workingBuilder.length - 1) {
                        if (workingBuilder[i] == '$' && workingBuilder[i + 1] == '{') {
                            val value = workingBuilder.substring(offset, workingBuilder.length)
                            workingBuilder.setLength(offset)
                            workingBuilder.append(config.strSubstitutor.replace(event, value))
                        }
                    }
                }
                if (doRender) {
                    textRenderer!!.render(workingBuilder, toAppendTo)
                }
            } else {
                val result: String?
                if (msg is MultiformatMessage) {
                    result = msg.getFormattedMessage(formats)
                } else {
                    result = msg.formattedMessage
                }
                if (result != null) {
                    toAppendTo.append(
                        if (config != null && result.contains("\${"))
                            config.strSubstitutor.replace(event, result)
                        else
                            result
                    )
                } else {
                    toAppendTo.append("null")
                }
            }

            val message = toAppendTo.toString()

            resultBuilder.append(wrapAndIndentString(message, padding, 110))
        }
    }

    companion object {


        private val NOLOOKUPS = "nolookups"


        /**
         * Obtains an instance of pattern converter.
         *
         * @param config  The Configuration.
         * @param options options, may be null.
         *
         * @return instance of pattern converter.
         */
        @JvmStatic
        fun newInstance(config: Configuration, options: Array<String>): EnhancedMessagePatternConverter {
            return EnhancedMessagePatternConverter(config, options)
        }

        /**
         * Indent and wrap multi-line strings.
         *
         * @param original the original string to wrap
         * @param width    the maximum width of lines
         *
         * @return the whole string with embedded newlines
         */
        private fun wrapAndIndentString(original: String, indent: String, width: Int): String {
            val breakIterator = BreakIterator.getWordInstance(Locale.GERMANY)
            val lines = wrapStringToArray(original, width, breakIterator)
            val resultBuilder = StringBuilder()


            val lineCount = lines.size
            for (i in 0 until lineCount) {
                val appendingLine = lines[i]
                resultBuilder.append(appendingLine.trim())
                if (i < lineCount - 1) {
                    resultBuilder.append(Strings.LINE_SEPARATOR)
                    resultBuilder.append(indent)
                }
            }
            return resultBuilder.toString()
        }


        /**
         * Wrap multi-line strings (and get the individual lines).
         *
         * @param original      the original string to wrap
         * @param width         the maximum width of lines
         * @param breakIterator breaks original to chars, words, sentences, depending on what instance you provide.
         *
         * @return the lines after wrapping
         */
        private fun wrapStringToArray(original: String, width: Int, breakIterator: BreakIterator): List<String> {
            if (original.isEmpty()) {
                return listOf(original)
            }
            val workingSet: MutableList<String> = mutableListOf()
            // substitute original newlines with spaces,
            // remove newlines from head and tail
            val tokens = StringTokenizer(original, "\n") // NOI18N
            val len = tokens.countTokens()
            for (i in 0 until len) {
                workingSet.add(tokens.nextToken())
            }

            val desiredLength = if (width < 1) 1 else width
            if (original.length <= desiredLength) {
                return workingSet
            }

            if (workingSet.none { it.length >= width }){
                return workingSet
            }

            val lines = ArrayList<String>()
            var lineStart = 0 // the position of start of currently processed line in
            // the original string
            for (aWorkingSet in workingSet) {
                if (aWorkingSet.length < width) {
                    lines.add(aWorkingSet)
                } else {
                    breakIterator.setText(aWorkingSet)
                    var nextStart = breakIterator.next()
                    var prevStart = 0
                    do {
                        while (nextStart - lineStart < width && nextStart != BreakIterator.DONE) {
                            prevStart = nextStart
                            nextStart = breakIterator.next()
                        }
                        if (nextStart == BreakIterator.DONE) {
                            prevStart = aWorkingSet.length
                            nextStart = prevStart
                        }
                        if (prevStart == 0) {
                            prevStart = nextStart
                        }
                        lines.add(aWorkingSet.substring(lineStart, prevStart))
                        lineStart = prevStart
                        prevStart = 0
                    } while (lineStart < aWorkingSet.length)
                    lineStart = 0
                }
            }
            return lines
        }
    }
}
