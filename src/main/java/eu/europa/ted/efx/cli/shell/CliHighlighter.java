/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.cli.shell;

import java.util.Set;
import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Syntax highlighter for the interactive shell.
 * Colors commands, options, and values as the user types.
 */
public class CliHighlighter implements Highlighter {

    private static final Pattern OPTION_PATTERN = Pattern.compile("^--?[a-zA-Z][a-zA-Z0-9-]*");

    private final Set<String> commands;

    public CliHighlighter(final Set<String> commands) {
        this.commands = commands;
    }

    @Override
    public AttributedString highlight(final LineReader reader, final String buffer) {
        final AttributedStringBuilder sb = new AttributedStringBuilder();
        final String[] tokens = buffer.split("(?<=\\s)|(?=\\s)");

        boolean firstWord = true;
        for (final String token : tokens) {
            if (token.isBlank()) {
                sb.append(token);
                continue;
            }

            if (firstWord) {
                firstWord = false;
                if (this.commands.contains(token)) {
                    sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.CYAN), token);
                } else {
                    sb.styled(AttributedStyle.BOLD.foreground(AttributedStyle.RED), token);
                }
            } else if (OPTION_PATTERN.matcher(token).find()) {
                sb.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW), token);
            } else {
                sb.append(token);
            }
        }
        return sb.toAttributedString();
    }

    @Override
    public void setErrorPattern(final Pattern errorPattern) {
        // not used
    }

    @Override
    public void setErrorIndex(final int errorIndex) {
        // not used
    }
}
