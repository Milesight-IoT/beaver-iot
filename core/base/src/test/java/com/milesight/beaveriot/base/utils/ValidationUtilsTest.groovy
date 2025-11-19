package com.milesight.beaveriot.base.utils

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author Luxb
 * @date 2025/11/11 16:10
 **/
@SuppressWarnings("HttpUrlsUsage")
class ValidationUtilsTest extends Specification {

    // ==================== isHex tests ====================

    @Unroll
    def "isHex returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isHex(text) == expected

        where:
        text           | expected
        // Valid hex strings
        "0"            | true
        "1"            | true
        "9"            | true
        "a"            | true
        "A"            | true
        "f"            | true
        "F"            | true
        "fF123"        | true
        "abc123DEF"    | true
        "0123456789"   | true
        "abcdefABCDEF" | true
        "deadbeef"     | true
        "DEADBEEF"     | true
        "FfFfFf"       | true
        ""             | true

        // Invalid hex strings
        "g"            | false
        "G"            | false
        "xyz"          | false
        "123G"         | false
        "123g"         | false
        " "            | false
        "  "           | false
        "-1"           | false
        "+1"           | false
        "0x123"        | false // Hex prefix not allowed
        "0X123"        | false
        "12.34"        | false
        "12,34"        | false
        "hello"        | false
        "123 456"      | false // Space in middle
        " 123"         | false // Leading space
        "123 "         | false // Trailing space
        "!@#"          | false
        "abc-def"      | false
        "abc_def"      | false
        null           | false
    }

    // ==================== isURL tests ====================

    @Unroll
    def "isURL returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isURL(text) == expected

        where:
        text                                                               | expected
        // Valid HTTP/HTTPS URLs
        "http://example.com"                                               | true
        "https://example.com"                                              | true
        "https://www.example.com"                                          | true
        "https://sub.example.com"                                          | true
        "https://sub.sub.example.com"                                      | true
        "http://example.co.uk"                                             | true
        "https://example.museum"                                           | true

        // URLs with paths
        "https://example.com/"                                             | true
        "https://example.com/path"                                         | true
        "https://example.com/path/to/resource"                             | true
        "https://example.com/path-with-dashes"                             | true
        "https://example.com/path_with_underscores"                        | true
        "https://example.com/path.with.dots"                               | true

        // URLs with query strings
        "https://example.com?query=1"                                      | true
        "https://example.com?query=1&param=2"                              | true
        "https://example.com/path?query=1"                                 | true
        "https://example.com/path?query=value&foo=bar"                     | true

        // URLs with fragments
        "https://example.com#fragment"                                     | true
        "https://example.com/#fragment"                                    | true
        "https://example.com/path#fragment"                                | true
        "https://example.com/path?query=1#fragment"                        | true

        // URLs with ports
        "http://example.com:80"                                            | true
        "https://example.com:443"                                          | true
        "https://example.com:8080"                                         | true
        "https://example.com:8080/path"                                    | true
        "http://example.com:1"                                             | true
        "http://example.com:65535"                                         | true

        // URLs with authentication
        "http://user@example.com"                                          | true
        "http://user:pass@example.com"                                     | true
        "https://user:password123@example.com/path"                        | true

        // IPv4 URLs
        "http://127.0.0.1"                                                 | true
        "http://192.168.1.1"                                               | true
        "http://192.168.1.1:8080"                                          | true
        "https://255.255.255.255"                                          | true
        "http://0.0.0.0"                                                   | true
        "http://10.0.0.1/path?query=1"                                     | true

        // IPv6 URLs
        "https://[::1]"                                                    | true
        "https://[::1]:8080"                                               | true
        "https://[2001:db8::1]"                                            | true
        "https://[2001:0db8:0000:0000:0000:0000:0000:0001]"                | true
        "https://[fe80::1]"                                                | true
        "https://[::ffff:192.0.2.1]"                                       | true

        // Complex valid URLs
        "https://sub.example.com/path/to/resource?query=1&foo=bar#section" | true
        "http://user:pass@example.com:8080/path?q=1#frag"                  | true

        // Invalid URLs
        ""                                                                 | false
        " "                                                                | false
        null                                                               | false
        "example.com"                                                      | false
        "www.example.com"                                                  | false
        "ftp://example.com"                                                | false
        "file://example.com"                                               | false
        "httpx://example.com"                                              | false
        "https://"                                                         | false
        "https://."                                                        | false
        "https://.com"                                                     | false
        "https://example"                                                  | false // No TLD
        "https://example."                                                 | false
        "not a url"                                                        | false
        "http:/example.com"                                                | false // Single slash
        "http:///example.com"                                              | false
        "https://example..com"                                             | false
        "https://-example.com"                                             | false
        "https://example-.com"                                             | false
        "http://256.1.1.1"                                                 | false // Invalid IP
        "http://192.168.1.256"                                             | false
        "http://example.com:70000"                                         | false // Invalid port
        "http://example.com:-1"                                            | false
    }

    // ==================== isImageBase64 tests ====================

    @Unroll
    def "isImageBase64 returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isImageBase64(text) == expected

        where:
        text                                                                                     | expected
        // PNG base64
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"                                         | true
        "data:image/png;base64,ABC"                                                              | false  // Invalid Base64
        "data:image/png;base64,ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/" | true

        // JPEG/JPG base64
        "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD"                                     | true
        "data:image/jpg;base64,abcd"                                                             | true
        "data:image/jpeg;base64,ABC123"                                                          | true

        // GIF base64
        "data:image/gif;base64,R0lGODdhAQABAPAAAP8AAAAAACwAAAAAAQABAAACAkQBADs="                 | true
        "data:image/gif;base64,abcd1234"                                                         | true

        // WebP base64
        "data:image/webp;base64,UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA="    | true
        "data:image/webp;base64,xyz789"                                                          | true

        // Base64 with different padding
        "data:image/png;base64,abc"                                                              | false  // Incorrect padding
        "data:image/png;base64,abcd"                                                             | true
        "data:image/png;base64,abcde"                                                            | false  // Incorrect padding
        "data:image/png;base64,ab"                                                               | false  // Incorrect padding
        "data:image/jpeg;base64,a"                                                               | false  // Incorrect padding
        "data:image/gif;base64,abcd=="                                                           | false  // Incorrect padding
        "data:image/webp;base64,abc="                                                            | false  // Incorrect padding

        // Invalid: unsupported image formats
        "data:image/bmp;base64,abcd"                                                             | false
        "data:image/svg;base64,abcd"                                                             | false
        "data:image/tiff;base64,abcd"                                                            | false
        "data:image/ico;base64,abcd"                                                             | false

        // Invalid: non-image types
        "data:text/plain;base64,abcd"                                                            | false
        "data:application/json;base64,abcd"                                                      | false
        "data:audio/mp3;base64,abcd"                                                             | false

        // Invalid: wrong format
        "data:image/png;base64,"                                                                 | false  // No data
        "data:image/png;base64"                                                                  | false  // Missing comma
        "data:image/png,abcd"                                                                    | false  // Missing :base64
        "image/png;base64,abcd"                                                                  | false  // Missing data:
        "data:image/png;abcd"                                                                    | false  // Missing base64,

        // Invalid: incorrect padding
        "data:image/png;base64,abcd==="                                                          | false  // Too much padding
        "data:image/png;base64,abcd===="                                                         | false
        "data:image/jpeg;base64,a="                                                              | false  // Incorrect padding position

        // Invalid: invalid base64 characters
        "data:image/png;base64,abc@"                                                             | false
        "data:image/png;base64,abc#"                                                             | false
        "data:image/png;base64,abc def"                                                          | false  // Space not allowed
        "data:image/png;base64,abc-def"                                                          | false  // Dash not allowed in this context

        // Edge cases
        ""                                                                                       | false
        " "                                                                                      | false
        null                                                                                     | false
        "not base64"                                                                             | false
        "data:image/png;base64"                                                                  | false
        "data:image/png;"                                                                        | false
    }

    // ==================== isNumber tests ====================

    @Unroll
    def "isNumber returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isNumber(text) == expected

        where:
        text          | expected
        // Positive integers
        "0"           | true
        "1"           | true
        "123"         | true
        "999999"      | true

        // Negative integers
        "-1"          | true
        "-123"        | true
        "-456"        | true

        // Decimal numbers
        "0.0"         | true
        "0.1"         | true
        "1.0"         | true
        "12.34"       | true
        "-12.34"      | true
        "123.456789"  | true
        "-123.456789" | true

        // Numbers with leading/trailing decimal point
        ".5"          | true
        ".123"        | true
        "5."          | true
        "123."        | true
        "-.5"         | true

        // Scientific notation
        "1e5"         | true
        "1E5"         | true
        "1e-5"        | true
        "1E-5"        | true
        "1.23e10"     | true
        "1.23E10"     | true
        "1.23e-10"    | true
        "-1.23e10"    | true

        // Special values (depends on NumberUtils implementation)
        "NaN"         | true
        "Infinity"    | true
        "-Infinity"   | true

        // Leading zeros
        "00"          | true
        "01"          | true
        "0123"        | true

        // Invalid numbers
        ""            | false
        " "           | false
        null          | false
        "abc"         | false
        "12abc"       | false
        "abc12"       | false
        "12.34.56"    | false
        "12..34"      | false
        "12,34"       | false
        "1 2"         | false
        " 12"         | false
        "12 "         | false
        "1e"          | false
        "e5"          | false
        "1.2.3e4"     | false
        "--1"         | false
        "+-1"         | false
        "1-2"         | false
    }

    // ==================== isInteger tests ====================

    @Unroll
    def "isInteger returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isInteger(text) == expected

        where:
        text                    | expected
        // Valid integers
        "0"                     | true
        "1"                     | true
        "123"                   | true
        "-1"                    | true
        "-123"                  | true
        "-456"                  | true

        // Leading zeros
        "00"                    | true
        "01"                    | true
        "0123"                  | true
        "-00"                   | true
        "-01"                   | true

        // Long boundaries (Long.MAX_VALUE and Long.MIN_VALUE)
        "9223372036854775807"   | true   // Long.MAX_VALUE
        "-9223372036854775808"  | true   // Long.MIN_VALUE
        "9223372036854775806"   | true
        "-9223372036854775807"  | true

        // Beyond Long boundaries
        "9223372036854775808"   | false  // Long.MAX_VALUE + 1
        "-9223372036854775809"  | false  // Long.MIN_VALUE - 1
        "99999999999999999999"  | false
        "-99999999999999999999" | false

        // Invalid formats
        "+1"                    | false  // Plus sign not supported
        "+123"                  | false
        "+789"                  | false
        "1.0"                   | false  // Decimal not allowed
        "12.0"                  | false
        "12.3"                  | false
        "1.23"                  | false
        "-1.0"                  | false
        ".5"                    | false
        "5."                    | false
        "1e5"                   | false  // Scientific notation
        "1E5"                   | false
        ""                      | false
        " "                     | false
        null                    | false
        "abc"                   | false
        "12abc"                 | false
        "abc12"                 | false
        " 123"                  | false  // Leading space
        "123 "                  | false  // Trailing space
        "1 2 3"                 | false  // Space in middle
        "1,234"                 | false  // Comma separator
        "1_234"                 | false  // Underscore separator
    }

    // ==================== isPositiveInteger tests ====================

    @Unroll
    def "isPositiveInteger returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isPositiveInteger(text) == expected

        where:
        text                   | expected
        // Valid positive integers
        "1"                    | true
        "2"                    | true
        "123"                  | true
        "456789"               | true
        "9223372036854775807"  | true  // Long.MAX_VALUE

        // Leading zeros (still positive)
        "01"                   | true
        "0123"                 | true

        // Zero (not positive)
        "0"                    | false
        "00"                   | false
        "-0"                   | false

        // Negative integers (not positive)
        "-1"                   | false
        "-2"                   | false
        "-123"                 | false
        "-9223372036854775808" | false  // Long.MIN_VALUE

        // Invalid formats
        "+1"                   | false  // Plus sign not supported
        "1.0"                  | false  // Decimal
        "12.3"                 | false
        ".5"                   | false
        "1e5"                  | false  // Scientific notation
        "abc"                  | false
        "12abc"                | false
        ""                     | false
        " "                    | false
        null                   | false
        " 1"                   | false
        "1 "                   | false
        "9223372036854775808"  | false  // Beyond Long.MAX_VALUE
    }

    // ==================== isNonNegativeInteger tests ====================

    @Unroll
    def "isNonNegativeInteger returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isNonNegativeInteger(text) == expected

        where:
        text                   | expected
        // Zero (valid non-negative)
        "0"                    | true
        "00"                   | true
        "-0"                   | true  // -0 is still 0

        // Valid positive integers (non-negative)
        "1"                    | true
        "2"                    | true
        "123"                  | true
        "456789"               | true
        "9223372036854775807"  | true  // Long.MAX_VALUE

        // Leading zeros
        "01"                   | true
        "0123"                 | true

        // Negative integers (not non-negative)
        "-1"                   | false
        "-2"                   | false
        "-123"                 | false
        "-9223372036854775808" | false  // Long.MIN_VALUE

        // Invalid formats
        "+1"                   | false  // Plus sign not supported
        "+0"                   | false
        "1.0"                  | false  // Decimal
        "12.3"                 | false
        ".5"                   | false
        "1e5"                  | false  // Scientific notation
        "abc"                  | false
        "12abc"                | false
        ""                     | false
        " "                    | false
        null                   | false
        " 0"                   | false
        "0 "                   | false
        "9223372036854775808"  | false  // Beyond Long.MAX_VALUE
    }

    // ==================== matches tests ====================

    @Unroll
    def "matches returns #expected for text '#text' and regex '#regex'"() {
        expect:
        ValidationUtils.matches(text, regex) == expected

        where:
        text               | regex                                               | expected
        // Valid matches
        'abc'              | 'abc'                                               | true
        'abc'              | '^abc$'                                             | true
        'abc'              | 'a.c'                                               | true
        'abc'              | '[a-z]+'                                            | true
        '123'              | '\\d+'                                              | true
        '123'              | '[0-9]{3}'                                          | true
        'test123'          | 'test\\d+'                                          | true
        'hello'            | 'h.*o'                                              | true
        'ABC'              | '[A-Z]+'                                            | true

        // Case sensitivity
        'abc'              | 'ABC'                                               | false
        'ABC'              | 'abc'                                               | false
        'abc'              | '(?i)ABC'                                           | true   // Case insensitive

        // Pattern variations
        'test'             | 'test|demo'                                         | true
        'demo'             | 'test|demo'                                         | true
        'other'            | 'test|demo'                                         | false
        'a'                | 'a*'                                                | true
        ''                 | 'a*'                                                | true   // Zero or more
        'aaa'              | 'a*'                                                | true
        'a'                | 'a+'                                                | true
        ''                 | 'a+'                                                | false  // One or more
        'a'                | 'a?'                                                | true
        ''                 | 'a?'                                                | true   // Zero or one

        // Special characters
        'a.b'              | 'a\\.b'                                             | true
        'a.b'              | 'a.b'                                               | true   // . matches any char
        'a*b'              | 'a\\*b'                                             | true
        'a+b'              | 'a\\+b'                                             | true
        'a?b'              | 'a\\?b'                                             | true

        // Empty and null handling
        ''                 | '.*'                                                | true
        ''                 | '.+'                                                | false
        ''                 | ''                                                  | true
        null               | '.*'                                                | false  // null text
        'abc'              | null                                                | false  // null regex
        null               | null                                                | false  // both null
        ''                 | null                                                | false
        null               | ''                                                  | false

        // Whitespace handling
        ' '                | ' '                                                 | true
        ' '                | '\\s'                                               | true
        '  '               | '\\s+'                                              | true
        'a b'              | 'a\\sb'                                             | true
        'a b'              | 'a b'                                               | true
        ' abc '            | '\\s*abc\\s*'                                       | true

        // Complex patterns
        'test@example.com' | '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$' | true
        'invalid.email'    | '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$' | false
        '192.168.1.1'      | '^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$'                  | true
        '256.1.1.1'        | '^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$'                  | true  // Matches pattern but not valid IP
    }

    def "matches handles empty string matching correctly"() {
        expect:
        ValidationUtils.matches('', '')
        !ValidationUtils.matches('a', '')
        !ValidationUtils.matches('', 'a')
    }

    // ==================== edge case tests ====================

    def "validation methods handle very long strings"() {
        given:
        def longHexString = 'a' * 10000
        def longNonHexString = 'g' * 10000
        def longNumberString = '1' * 1000

        expect:
        ValidationUtils.isHex(longHexString)
        !ValidationUtils.isHex(longNonHexString)
        ValidationUtils.isNumber(longNumberString)
    }

    // ==================== boundary tests ====================

    def "isInteger correctly handles values near Long boundaries"() {
        expect:
        ValidationUtils.isInteger('9223372036854775807')   // MAX_VALUE
        ValidationUtils.isInteger('-9223372036854775808')  // MIN_VALUE
        !ValidationUtils.isInteger('9223372036854775808')  // MAX_VALUE + 1
        !ValidationUtils.isInteger('-9223372036854775809') // MIN_VALUE - 1
    }

    def "isPositiveInteger correctly handles zero boundary"() {
        expect:
        !ValidationUtils.isPositiveInteger('0')
        ValidationUtils.isPositiveInteger('1')
        !ValidationUtils.isPositiveInteger('-1')
    }

    def "isNonNegativeInteger correctly handles zero boundary"() {
        expect:
        ValidationUtils.isNonNegativeInteger('0')
        ValidationUtils.isNonNegativeInteger('1')
        !ValidationUtils.isNonNegativeInteger('-1')
    }
}
