package eu.europa.ted.efx.cli;

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
