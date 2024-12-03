package com.milesight.beaveriot.base.constants;

/**
 * Commonly used string constant definitions
 * Copy to mybatis-plus
 *
 * @author leon
 */
public interface StringConstant {

    /**
     * string constant dot {@code "."}
     */
    String DOT = ".";

    /**
     * String constant: double dot {@code ".."} <br>
     * Purpose: as a path to the superior folder, such as: {@code "../path"}
     */
    String DOUBLE_DOT = "..";

    /**
     * String constant: slash {@code "/"}
     */
    String SLASH = "/";

    /**
     * String constant: backslash {@code "\\"}
     */
    String BACKSLASH = "\\";

    /**
     * String constant: carriage return {@code "\r"} <br>
     * Explanation: This character is often used to represent text line breaks under Linux systems and MacOS systems.
     */
    String CR = "\r";

    /**
     * String constant: newline {@code "\n"}
     */
    String LF = "\n";

    /**
     * String constants: Windows newline {@code "\r\n"} <br>
     * Explanation: This string is often used to represent text line breaks under Windows systems
     */
    String CRLF = "\r\n";

    /**
     * String constant: underscore {@code "_"}
     */
    String UNDERLINE = "_";

    /**
     * String constant: minus sign (connector) {@code "-"}
     */
    String DASHED = "-";

    /**
     * String constant: comma {@code ","}
     */
    String COMMA = ",";

    /**
     * String constants: curly braces (left) <code>"{"</code>
     */
    String DELIM_START = "{";

    /**
     * String constants: curly braces (right) <code>"}"</code>
     */
    String DELIM_END = "}";

    /**
     * String constant: square bracket (left) {@code "["}
     */
    String BRACKET_START = "[";

    /**
     * String constant: square bracket (right) {@code "]"}
     */
    String BRACKET_END = "]";

    /**
     * String constant: colon {@code ":"}
     */
    String COLON = ":";

    /**
     * String constant: Aite {@code "@"}
     */
    String AT = "@";

    /**
     * String constant: empty JSON {@code "{}"}
     */
    String EMPTY_JSON = "{}";

    String FORMAT_PLACE_HOLDER = "{0}";

    String STAR = "*";
}
