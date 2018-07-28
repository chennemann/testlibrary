package de.chennemann.utils.logging.converter;


import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.HtmlTextRenderer;
import org.apache.logging.log4j.core.pattern.JAnsiTextRenderer;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.core.util.ArrayUtils;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ProcessIdUtil;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;




/**
 * Returns the event's rendered message in a StringBuilder.
 */
@Plugin(name = "EnhancedMessagePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "wMessage" })
@PerformanceSensitive("allocation")
public final class EnhancedMessagePatternConverter extends LogEventPatternConverter {


    private static final String NOLOOKUPS = "nolookups";

    private final String[] formats;
    private final Configuration config;
    private final TextRenderer textRenderer;
    private final boolean noLookups;

    private final String padding;


    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private EnhancedMessagePatternConverter(final Configuration config, final String[] options) {
        super("Message", "message");
        this.formats = options;
        this.config = config;
        final int noLookupsIdx = loadNoLookups(options);
        this.noLookups = Constants.FORMAT_MESSAGES_PATTERN_DISABLE_LOOKUPS || noLookupsIdx >= 0;
        this.textRenderer = loadMessageRenderer(noLookupsIdx >= 0 ? ArrayUtils.remove(options, noLookupsIdx) : options);

        String discoveredPid = ProcessIdUtil.getProcessId();
        final int paddingSize = 75 + discoveredPid.length();
        final StringBuilder paddingBuilder = new StringBuilder(paddingSize);
        for (int i = 0; i < paddingSize; i++) {
            paddingBuilder.append(" ");
        }
        padding = paddingBuilder.toString();
    }


    private int loadNoLookups(final String[] options) {
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                final String option = options[i];
                if (NOLOOKUPS.equalsIgnoreCase(option)) {
                    return i;
                }
            }
        }
        return -1;
    }


    private TextRenderer loadMessageRenderer(final String[] options) {
        if (options != null) {
            for (final String option : options) {
                switch (option.toUpperCase(Locale.ROOT)) {
                    case "ANSI":
                        if (Loader.isJansiAvailable()) {
                            return new JAnsiTextRenderer(options, new HashMap<>());
                        }
                        StatusLogger.getLogger()
                                    .warn("You requested ANSI message rendering but JANSI is not on the classpath.");
                        return null;
                    case "HTML":
                        return new HtmlTextRenderer(options);
                }
            }
        }
        return null;
    }


    /**
     * Obtains an instance of pattern converter.
     *
     * @param config  The Configuration.
     * @param options options, may be null.
     *
     * @return instance of pattern converter.
     */
    public static EnhancedMessagePatternConverter newInstance(final Configuration config, final String[] options) {
        return new EnhancedMessagePatternConverter(config, options);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder resultBuilder) {


        final Message msg = event.getMessage();
        if (msg != null) {
            final StringBuilder toAppendTo = new StringBuilder();

            if (msg instanceof StringBuilderFormattable) {

                final boolean doRender = textRenderer != null;
                final StringBuilder workingBuilder = doRender ? new StringBuilder(80) : toAppendTo;

                final int offset = workingBuilder.length();
                if (msg instanceof MultiFormatStringBuilderFormattable) {
                    ( (MultiFormatStringBuilderFormattable) msg ).formatTo(formats, workingBuilder);
                } else {
                    ( (StringBuilderFormattable) msg ).formatTo(workingBuilder);
                }

                // TODO can we optimize this?
                if (config != null && !noLookups) {
                    for (int i = offset; i < workingBuilder.length() - 1; i++) {
                        if (workingBuilder.charAt(i) == '$' && workingBuilder.charAt(i + 1) == '{') {
                            final String value = workingBuilder.substring(offset, workingBuilder.length());
                            workingBuilder.setLength(offset);
                            workingBuilder.append(config.getStrSubstitutor().replace(event, value));
                        }
                    }
                }
                if (doRender) {
                    textRenderer.render(workingBuilder, toAppendTo);
                }
            } else {
                String result;
                if (msg instanceof MultiformatMessage) {
                    result = ( (MultiformatMessage) msg ).getFormattedMessage(formats);
                } else {
                    result = msg.getFormattedMessage();
                }
                if (result != null) {
                    toAppendTo.append(config != null && result.contains("${")
                                          ? config.getStrSubstitutor().replace(event, result) : result);
                } else {
                    toAppendTo.append("null");
                }
            }

            final String message = toAppendTo.toString();

            resultBuilder.append(wrapAndIndentString(message, padding, 120));
        }
    }

    /**
     * Indent and wrap multi-line strings.
     *
     * @param original the original string to wrap
     * @param width    the maximum width of lines
     *
     * @return the whole string with embedded newlines
     */
    private static String wrapAndIndentString(String original, String indent, int width) {
        BreakIterator breakIterator = BreakIterator.getWordInstance(Locale.GERMANY);
        List<String> lines = wrapStringToArray(original, width, breakIterator);
        StringBuilder resultBuilder = new StringBuilder();


        final int lineCount = lines.size();
        for (int i = 0; i < lineCount; i++) {
            final String appendingLine = lines.get(i);
            resultBuilder.append(appendingLine);
            if (i < lineCount - 1) {
                resultBuilder.append(Strings.LINE_SEPARATOR);
                resultBuilder.append(indent);
            }
        }
        return resultBuilder.toString();
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
    private static List<String> wrapStringToArray(String original, int width, BreakIterator breakIterator) {
        if (original.length() == 0) {
            return Collections.singletonList(original);
        }
        String[] workingSet;
        // substitute original newlines with spaces,
        // remove newlines from head and tail
        StringTokenizer tokens = new StringTokenizer(original, "\n"); // NOI18N
        int len = tokens.countTokens();
        workingSet = new String[len];
        for (int i = 0; i < len; i++) {
            workingSet[i] = tokens.nextToken();
        }
        if (width < 1) {
            width = 1;
        }
        if (original.length() <= width) {
            return Arrays.asList(workingSet);
        }
        widthcheck:
        {
            for (final String aWorkingSet : workingSet) {
                if (!( aWorkingSet.length() < width )) {
                    break widthcheck;
                }
            }
            return Arrays.asList(workingSet);
        }
        ArrayList<String> lines = new ArrayList<>();
        int lineStart = 0; // the position of start of currently processed line in
        // the original string
        for (final String aWorkingSet : workingSet) {
            if (aWorkingSet.length() < width) {
                lines.add(aWorkingSet);
            } else {
                breakIterator.setText(aWorkingSet);
                int nextStart = breakIterator.next();
                int prevStart = 0;
                do {
                    while (( ( nextStart - lineStart ) < width ) && ( nextStart != BreakIterator.DONE )) {
                        prevStart = nextStart;
                        nextStart = breakIterator.next();
                    }
                    if (nextStart == BreakIterator.DONE) {
                        nextStart = prevStart = aWorkingSet.length();
                    }
                    if (prevStart == 0) {
                        prevStart = nextStart;
                    }
                    lines.add(aWorkingSet.substring(lineStart, prevStart));
                    lineStart = prevStart;
                    prevStart = 0;
                } while (lineStart < aWorkingSet.length());
                lineStart = 0;
            }
        }
        return lines;
    }
}
