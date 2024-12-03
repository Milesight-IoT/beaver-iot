package com.milesight.beaveriot.base.utils;

/**
 * @author leon
 */
public class KeyPatternMatcher {

    private KeyPatternMatcher() {
    }
    public static boolean match(String keyPattern, String key) {
        if(keyPattern.equals(key)){
            return true;
        }
        return matchPattern(keyPattern, key);
    }

    private static boolean matchPattern(String pattern, String str) {
        return matchPattern(pattern, str, 0, 0);
    }

    private static boolean matchPattern(String pattern, String str, int pIndex, int sIndex) {
        // If the pattern has been processed, check if the string has also been processed
        if (pIndex == pattern.length()) {
            return sIndex == str.length();
        }
        // If the string is processed but the pattern is not, check to see if only * is left in the pattern
        if (sIndex == str.length()) {
            return allStars(pattern, pIndex);
        }

        if (pattern.charAt(pIndex) == '*' || pattern.charAt(pIndex) == str.charAt(sIndex)) {
            if (pattern.charAt(pIndex) == '*') {
                return matchPattern(pattern, str, pIndex + 1, sIndex) || matchPattern(pattern, str, pIndex, sIndex + 1);
            } else {
                return matchPattern(pattern, str, pIndex + 1, sIndex + 1);
            }
        }
        return false;
    }

    private static boolean allStars(String pattern, int pIndex) {
        for (int i = pIndex; i < pattern.length(); i++) {
            if (pattern.charAt(i) != '*') {
                return false;
            }
        }
        return true;
    }

}
