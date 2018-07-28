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
package de.chennemann.utils.logging.converter;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.util.Strings;




/**
 * Outputs the Throwable portion of the LoggingEvent as a full stack trace
 * unless this converter's option is 'short', where it just outputs the first line of the trace, or if
 * the number of lines to print is explicitly specified.
 */
@Plugin(name = "EnhancedThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "eEx", "eThrowable", "eException" })
public class EnhancedThrowablePatternConverter extends LogEventPatternConverter {

    /**
     * Lists {@link PatternFormatter}s for the suffix attribute.
     */
    private final List<PatternFormatter> formatters;
    private String rawOption;

    /**
     * Options.
     */
    private final ThrowableFormatOptions options;

    private final String padding = "\t\t";

    /**
     * Constructor.
     * @param name Name of converter.
     * @param style CSS style for output.
     * @param options options, may be null.
     */
    private EnhancedThrowablePatternConverter(final String name, final String style, final String[] options, final Configuration config) {
        super(name, style);
        this.options = ThrowableFormatOptions.newInstance(options);
        if (options != null && options.length > 0) {
            rawOption = options[0];
        }
        if (this.options.getSuffix() != null) {
            final PatternParser parser = PatternLayout.createPatternParser(config);
            final List<PatternFormatter> parsedSuffixFormatters = parser.parse(this.options.getSuffix());
            // filter out nested formatters that will handle throwable
            boolean hasThrowableSuffixFormatter = false;
            for (final PatternFormatter suffixFormatter : parsedSuffixFormatters) {
                if (suffixFormatter.handlesThrowable()) {
                    hasThrowableSuffixFormatter = true;
                }
            }
            if (!hasThrowableSuffixFormatter) {
                this.formatters = parsedSuffixFormatters;
            } else {
                final List<PatternFormatter> suffixFormatters = new ArrayList<>();
                for (final PatternFormatter suffixFormatter : parsedSuffixFormatters) {
                    if (!suffixFormatter.handlesThrowable()) {
                        suffixFormatters.add(suffixFormatter);
                    }
                }
                this.formatters = suffixFormatters;
            }
        } else {
            this.formatters = Collections.emptyList();
        }

    }

    /**
     * Gets an instance of the class.
     *
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static EnhancedThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
        return new EnhancedThrowablePatternConverter("Throwable", "throwable", options, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder buffer) {
        final Throwable t = event.getThrown();

        appendIfThrowableExists(t, buffer);

        if (isSubShortOption()) {
            formatSubShortOption(t, getSuffix(event), buffer);
        }
        else if (t != null && options.anyLines()) {
            formatOption(t, getSuffix(event), buffer);
        }

        appendIfThrowableExists(t, buffer);
    }

    private void appendIfThrowableExists(final Throwable t, final StringBuilder buffer) {
        if (t != null) {
            buffer.append(Strings.LINE_SEPARATOR);
        }
    }

    private boolean isSubShortOption() {
        return ThrowableFormatOptions.MESSAGE.equalsIgnoreCase(rawOption) ||
                ThrowableFormatOptions.LOCALIZED_MESSAGE.equalsIgnoreCase(rawOption) ||
                ThrowableFormatOptions.FILE_NAME.equalsIgnoreCase(rawOption) ||
                ThrowableFormatOptions.LINE_NUMBER.equalsIgnoreCase(rawOption) ||
                ThrowableFormatOptions.METHOD_NAME.equalsIgnoreCase(rawOption) ||
                ThrowableFormatOptions.CLASS_NAME.equalsIgnoreCase(rawOption);
    }

    private void formatSubShortOption(final Throwable t, final String suffix, final StringBuilder buffer) {
        StackTraceElement[] trace;
        StackTraceElement throwingMethod = null;
        int len;

        if (t != null) {
            trace = t.getStackTrace();
            if (trace !=null && trace.length > 0) {
                throwingMethod = trace[0];
            }
        }

        if (t != null && throwingMethod != null) {
            String toAppend = Strings.EMPTY;

            if (ThrowableFormatOptions.CLASS_NAME.equalsIgnoreCase(rawOption)) {
                toAppend = throwingMethod.getClassName();
            }
            else if (ThrowableFormatOptions.METHOD_NAME.equalsIgnoreCase(rawOption)) {
                toAppend = throwingMethod.getMethodName();
            }
            else if (ThrowableFormatOptions.LINE_NUMBER.equalsIgnoreCase(rawOption)) {
                toAppend = String.valueOf(throwingMethod.getLineNumber());
            }
            else if (ThrowableFormatOptions.MESSAGE.equalsIgnoreCase(rawOption)) {
                toAppend = t.getMessage();
            }
            else if (ThrowableFormatOptions.LOCALIZED_MESSAGE.equalsIgnoreCase(rawOption)) {
                toAppend = t.getLocalizedMessage();
            }
            else if (ThrowableFormatOptions.FILE_NAME.equalsIgnoreCase(rawOption)) {
                toAppend = throwingMethod.getFileName();
            }

            len = buffer.length();
            if (len > 0 && !Character.isWhitespace(buffer.charAt(len - 1))) {
                buffer.append(' ');
            }
            buffer.append(toAppend);

            if (Strings.isNotBlank(suffix)) {
                buffer.append(' ');
                buffer.append(suffix);
            }
        }
    }

    private void formatOption(final Throwable throwable, final String suffix, final StringBuilder buffer) {
        final StringWriter w = new StringWriter();

        throwable.printStackTrace(new PrintWriter(w));
        final int len = buffer.length();
        if (len > 0 && !Character.isWhitespace(buffer.charAt(len - 1))) {
            buffer.append(' ');
        }

        final String[] padded = Arrays
            .stream(w.toString().split(Strings.LINE_SEPARATOR))
            .map(it -> padding + it)
            .toArray(String[]::new);

        final StringBuilder resultBuilder = new StringBuilder();
        if (!options.allLines() || !Strings.LINE_SEPARATOR.equals(options.getSeparator()) || Strings.isNotBlank(suffix)) {
            final int limit = options.minLines(padded.length) - 1;
            final boolean suffixNotBlank = Strings.isNotBlank(suffix);
            for (int i = 0; i <= limit; ++i) {
                resultBuilder.append(padded[i]);
                if (suffixNotBlank) {
                    resultBuilder.append(' ');
                    resultBuilder.append(suffix);
                }
                if (i < limit) {
                    resultBuilder.append(options.getSeparator());
                }
            }
        } else {

            final int lineCount = padded.length;
            for (int i = 0; i < lineCount; i++) {
                final String appendingLine = padded[i];

                resultBuilder.append(appendingLine);
                if (i < lineCount - 1) {
                    resultBuilder.append(Strings.LINE_SEPARATOR);
                }
            }
        }
        resultBuilder.append(Strings.LINE_SEPARATOR);
        buffer.append(resultBuilder.toString());
    }

    /**
     * This converter obviously handles throwables.
     *
     * @return true.
     */
    @Override
    public boolean handlesThrowable() {
        return true;
    }

    private String getSuffix(final LogEvent event) {
        //noinspection ForLoopReplaceableByForEach
        final StringBuilder toAppendTo = new StringBuilder();
        for (PatternFormatter formatter : formatters) {
            formatter.format(event, toAppendTo);
        }
        return toAppendTo.toString();
    }

}