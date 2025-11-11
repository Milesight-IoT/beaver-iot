package com.milesight.beaveriot.base.utils

import spock.lang.Specification
import spock.lang.Unroll

/**
 * author: Luxb
 * create: 2025/11/11 16:10
 **/
class ValidationUtilsTest extends Specification {

    /**
     * Test isHex
     */
    @Unroll
    def "isHex returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isHex(text) == expected

        where:
        text        | expected
        "a"         | true
        "A"         | true
        "0"         | true
        "fF123"     | true
        "abc123DEF" | true
        "g"         | false
        "xyz"       | false
        "123G"      | false
        " "         | false
        "-1"        | false
        null        | false
    }

    /**
     * Test isURL
     */
    @Unroll
    def "isURL returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isURL(text) == expected

        where:
        text                                        | expected
        "https://example.com"                       | true
        "http://example.com"                        | true
        "https://sub.example.com/path?query=1#frag" | true
        "http://192.168.1.1"                        | true
        "https://[::1]"                             | true
        "http://user:pass@example.com"              | true
        "https://example.com:8080/path"             | true
        ""                                          | false
        "ftp://example.com"                         | false
        "httpx://example.com"                       | false
        "example.com"                               | false
        "https://"                                  | false
        "https://."                                 | false
        "not a url"                                 | false
        null                                        | false
    }

    /**
     * Test isImageBase64
     */
    @Unroll
    def "isImageBase64 returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isImageBase64(text) == expected

        where:
        text                                                                                  | expected
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA"                                      | true
        "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD"                                  | true
        "data:image/jpg;base64,abcd"                                                          | true
        "data:image/gif;base64,R0lGODdhAQABAPAAAP8AAAAAACwAAAAAAQABAAACAkQBADs="              | true
        "data:image/webp;base64,UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA=" | true
        "data:image/bmp;base64,abcd"                                                          | false
        "data:text/plain;base64,abcd"                                                         | false
        "data:image/png;base64,"                                                              | false
        "data:image/png;base64,abcd=="                                                        | true
        "data:image/png;base64,abcd==="                                                       | false
        "not base64"                                                                          | false
        ""                                                                                    | false
        null                                                                                  | false
    }

    /**
     * Test isNumber
     */
    @Unroll
    def "isNumber returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isNumber(text) == expected

        where:
        text       | expected
        "0"        | true
        "123"      | true
        "-456"     | true
        "12.34"    | true
        "-12.34"   | true
        "1e5"      | true
        "1E-3"     | true
        ".5"       | true
        "5."       | true
        ""         | false
        "abc"      | false
        "12.34.56" | false
        "NaN"      | true
        "Infinity" | true
        null       | false
    }

    /**
     * Test isInteger
     */
    @Unroll
    def "isInteger returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isInteger(text) == expected

        where:
        text                   | expected
        "0"                    | true
        "123"                  | true
        "-456"                 | true
        "+789"                 | false
        "12.0"                 | false
        "12.3"                 | false
        ""                     | false
        "abc"                  | false
        "9223372036854775807"  | true
        "-9223372036854775808" | true
        "9223372036854775808"  | false
        "-9223372036854775809" | false
        null                   | false
    }

    /**
     * Test isPositiveInteger
     */
    @Unroll
    def "isPositiveInteger returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isPositiveInteger(text) == expected

        where:
        text                  | expected
        "1"                   | true
        "123"                 | true
        "9223372036854775807" | true
        "0"                   | false
        "-1"                  | false
        "12.3"                | false
        "abc"                 | false
        ""                    | false
        null                  | false
    }

    /**
     * Test isNonNegativeInteger
     */
    @Unroll
    def "isNonNegativeInteger returns #expected for input '#text'"() {
        expect:
        ValidationUtils.isNonNegativeInteger(text) == expected

        where:
        text                  | expected
        "0"                   | true
        "1"                   | true
        "123"                 | true
        "9223372036854775807" | true
        "-1"                  | false
        "12.3"                | false
        "abc"                 | false
        ""                    | false
        null                  | false
    }
}