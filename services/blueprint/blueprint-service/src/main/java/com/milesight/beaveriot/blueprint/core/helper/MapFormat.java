package com.milesight.beaveriot.blueprint.core.helper;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
public class MapFormat {

    public static String format(String template, Map<Object, Object> params) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        var result = new StringBuilder();
        var length = template.length();
        var inEscape = false;
        var braceDepth = 0;
        var currentPlaceholder = new StringBuilder();
        var inPlaceholder = false;

        for (var i = 0; i < length; i++) {
            var c = template.charAt(i);

            if (c == '\'') {
                if (inEscape) {
                    if (i + 1 < length && template.charAt(i + 1) == '\'') {
                        result.append('\'');
                        i++; //NOSONAR
                    } else {
                        inEscape = false;
                    }
                } else {
                    inEscape = true;
                }
                continue;
            }

            if (inEscape) {
                result.append(c);
                continue;
            }

            if (c == '{') {
                if (inPlaceholder) {
                    braceDepth++;
                    currentPlaceholder.append(c);
                } else {
                    inPlaceholder = true;
                    braceDepth = 0;
                    currentPlaceholder.setLength(0);
                }
            } else if (c == '}') {
                if (!inPlaceholder) {
                    throw new IllegalArgumentException("Unmatched closing brace '}' at position " + i);
                }

                if (braceDepth > 0) {
                    braceDepth--;
                    currentPlaceholder.append(c);
                } else {
                    inPlaceholder = false;
                    var placeholderContent = currentPlaceholder.toString();
                    var replacement = processPlaceholder(placeholderContent, params);
                    result.append(replacement);
                }
            } else {
                if (inPlaceholder) {
                    currentPlaceholder.append(c);
                } else {
                    result.append(c);
                }
            }
        }

        if (inPlaceholder) {
            throw new IllegalArgumentException("Unclosed placeholder starting with '{'");
        }

        return result.toString();
    }


    /**
     * Parse the placeholder: {argumentName,formatType,formatStyle}
     */
    public static String processPlaceholder(String placeholderContent, Map<Object, Object> params) {
        if (placeholderContent == null || placeholderContent.trim().isEmpty()) {
            return "";
        }

        var parts = placeholderContent.split(",", 3);
        var argumentName = parts[0].trim();

        var value = params.get(argumentName);
        if (value == null) {
            log.debug("No value found for placeholder: {}", argumentName);
            return "{" + placeholderContent + "}";
        }

        if (parts.length == 1) {
            return String.valueOf(value);
        }

        var formatType = parts[1].trim();
        var formatStyle = parts.length > 2 ? parts[2].trim() : null;

        try {
            return formatValue(value, formatType, formatStyle);
        } catch (Exception e) {
            log.warn("Error formatting value for placeholder {}: {}", argumentName, e.getMessage());
            return String.valueOf(value);
        }
    }


    public static String formatValue(Object value, String formatType, String formatStyle) {
        return switch (formatType.toLowerCase()) {
            case "number" -> formatNumber(value, formatStyle);
            case "date", "time" -> formatDateTime(value, formatType, formatStyle);
            case "choice" -> formatChoice(value, formatStyle);
            default -> String.valueOf(value);
        };
    }

    public static String formatNumber(Object value, String formatStyle) {
        if (!(value instanceof Number number)) {
            return String.valueOf(value);
        }

        if (formatStyle == null || formatStyle.isEmpty()) {
            return String.valueOf(number);
        }

        try {
            var formatter = switch (formatStyle.toLowerCase()) {
                case "currency" -> NumberFormat.getCurrencyInstance();
                case "percent" -> NumberFormat.getPercentInstance();
                case "integer" -> NumberFormat.getIntegerInstance();
                default -> new DecimalFormat(formatStyle);
            };
            return formatter.format(number);
        } catch (Exception e) {
            log.warn("Error formatting number {} with style {}: {}", value, formatStyle, e.getMessage());
            return String.valueOf(number);
        }
    }

    public static String formatDateTime(Object value, String formatType, String formatStyle) {
        if (!(value instanceof Date date)) {
            return String.valueOf(value);
        }

        if (formatStyle == null || formatStyle.isEmpty()) {
            return String.valueOf(date);
        }

        try {
            var formatter = switch (formatStyle.toLowerCase()) {
                case "short" -> formatType.equals("date") ?
                        DateFormat.getDateInstance(DateFormat.SHORT) :
                        DateFormat.getTimeInstance(DateFormat.SHORT);
                case "medium" -> formatType.equals("date") ?
                        DateFormat.getDateInstance(DateFormat.MEDIUM) :
                        DateFormat.getTimeInstance(DateFormat.MEDIUM);
                case "long" -> formatType.equals("date") ?
                        DateFormat.getDateInstance(DateFormat.LONG) :
                        DateFormat.getTimeInstance(DateFormat.LONG);
                case "full" -> formatType.equals("date") ?
                        DateFormat.getDateInstance(DateFormat.FULL) :
                        DateFormat.getTimeInstance(DateFormat.FULL);
                default -> new java.text.SimpleDateFormat(formatStyle);
            };
            return formatter.format(date);
        } catch (Exception e) {
            log.warn("Error formatting date {} with style {}: {}", value, formatStyle, e.getMessage());
            return String.valueOf(date);
        }
    }

    /**
     * Parse the choice format: value1#message1|value2#message2
     */
    public static String formatChoice(Object value, String formatStyle) {
        if (formatStyle == null || formatStyle.isEmpty()) {
            return String.valueOf(value);
        }

        try {
            var choices = formatStyle.split("\\|");
            for (String choice : choices) {
                var parts = choice.split("#", 2);
                if (parts.length == 2 && String.valueOf(value).equals(parts[0].trim())) {
                    return parts[1].trim();
                }
            }
            return String.valueOf(value);
        } catch (Exception e) {
            log.warn("Error formatting choice {} with style {}: {}", value, formatStyle, e.getMessage());
            return String.valueOf(value);
        }
    }

    private MapFormat() {
        throw new UnsupportedOperationException();
    }

}
