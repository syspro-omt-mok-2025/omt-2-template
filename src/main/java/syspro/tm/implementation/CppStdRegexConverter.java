package syspro.tm.implementation;

import syspro.tm.regex.Regex;
import syspro.tm.regex.RegexCharacter;
import syspro.tm.regex.RegexCharacterVisitor;
import syspro.tm.regex.RegexVisitor;

import java.util.ArrayList;
import java.util.List;

import static syspro.tm.Utils.isMetaCharacter;
import static syspro.tm.Utils.toLiteralPatternCharacter;

/**
 * C++ std::regex has a few quirks, so we need to transform the regular expression.
 * <p>
 * This is a copy of {@link Regex#toString()} and {@link RegexCharacter#toString()} with a few changes
 * to make DOT include newlines, and for being placed into C string literal.
 */
final class CppStdRegexConverter implements RegexVisitor<String>, RegexCharacterVisitor<String> {
    private static String formatLiteralByte(byte value) {
        return toLiteralPatternCharacter(value, '"');
    }

    @Override
    public String visitConstant(RegexCharacter.Constant node) {
        return formatLiteralByte(node.value);
    }

    @Override
    public String visitRange(RegexCharacter.Range node) {
        return '[' + toShortString(node) + ']';
    }

    private static String toShortString(RegexCharacter.Range node) {
        return formatLiteralByte(node.start) + "-" + formatLiteralByte(node.end);
    }

    private void toRegexString(List<String> strings, RegexCharacter... characters) {
        for (final var character : characters) {
            if (character instanceof RegexCharacter.Union union) {
                toRegexString(strings, union.characters());
                continue;
            }

            if (character instanceof RegexCharacter.Range range) {
                strings.add(toShortString(range));
                continue;
            }

            if (character == RegexCharacter.PredefinedCharacterClass.WILDCARD) {
                // We can't use (.|\n) because we are inside character class brackets.
                // We can't use [.\n] because . has no special meaning inside character class brackets.
                strings.add("\\\\s");
                strings.add("\\\\S");
                continue;
            }

            strings.add(character.accept(this));
        }
    }

    @Override
    public String visitUnion(RegexCharacter.Union node) {
        final var sb = new StringBuilder();
        final var parts = new ArrayList<String>();
        toRegexString(parts, node.characters());
        sb.append('[');
        for (final var part : parts) {
            sb.append(part);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public String visitNegation(RegexCharacter.Negation node) {
        final var sb = new StringBuilder();
        final var parts = new ArrayList<String>();
        if (node.argument instanceof RegexCharacter.Union union) {
            toRegexString(parts, union.characters());
        } else {
            toRegexString(parts, node.argument);
        }
        sb.append("[^");
        for (final var part : parts) {
            sb.append(part);
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public String visitPredefinedCharacterClass(RegexCharacter.PredefinedCharacterClass node) {
        if (node == RegexCharacter.PredefinedCharacterClass.WILDCARD) {
            // C++ regex: \n
            // C++ string literal: \\n
            // Java string literal: \\\\n
            return "(.|\\\\n)";
        }

        // C++ regex: \d
        // C++ string literal: \\d
        // Java string literal: \\\\d
        // The following line replaces \ with \\.
        return node.mnemonic.replace("\\", "\\\\");
    }

    @Override
    public String visitSingleCharacter(Regex.SingleCharacter node) {
        return node.character.accept(this);
    }

    @Override
    public String visitAlternation(Regex.Alternation node) {
        final var sb = new StringBuilder();
        for (final var part : node) {
            if (!sb.isEmpty()) {
                sb.append('|');
            }

            if (part != null) {
                sb.append(part.accept(this));
            }
        }
        return sb.toString();
    }

    @Override
    public String visitConcatenation(Regex.Concatenation node) {
        final var sb = new StringBuilder();
        for (final var part : node) {
            if (part != null) {
                final var parenthesized = part instanceof Regex.Alternation;
                if (parenthesized) {
                    sb.append('(');
                }
                sb.append(part.accept(this));
                if (parenthesized) {
                    sb.append(')');
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String visitRepetition(Regex.Repetition node) {
        final var sb = new StringBuilder();
        sb.append('(');
        sb.append(node.argument.accept(this));
        sb.append(')');
        final var min = node.min;
        final var max = node.max;
        if (min == null) {
            return sb.append("{,").append(max).append('}').toString();
        }
        if (max == null) {
            if (min == 0) {
                sb.append('*');
            } else if (min == 1) {
                sb.append('+');
            } else {
                sb.append('{').append(min).append(",}");
            }
            return sb.toString();
        }
        if (min == 0 && max == 1) {
            return sb.append('?').toString();
        }
        return sb.append('{').append(min).append(',').append(max).append('}').toString();
    }
}
