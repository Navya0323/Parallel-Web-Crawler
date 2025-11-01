package com.udacity.webcrawler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 *
 * <p>TODO: Reimplement the sort() method using only the Stream API and lambdas and/or method
 *          references.
 */
final class WordCounts {

    /**
     * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
     * to the provided {@link WordCountComparator}, and includes only the top
     * {@param popularWordCount} words and counts.
     *
     * <p>TODO: Reimplement this method using only the Stream API and lambdas and/or method
     *          references.
     *
     * @param wordCounts       the unsorted map of word counts.
     * @param popularWordCount the number of popular words to include in the result map.
     * @return a map containing the top {@param popularWordCount} words and counts in the right order.
     */
    static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {

        // Functional implementation using streams:
        return wordCounts.entrySet().stream()
                .sorted((a, b) -> {
                    // First: descending by word frequency
                    int countCmp = b.getValue().compareTo(a.getValue());
                    if (countCmp != 0) return countCmp;
                    // Second: descending by word length
                    int lengthCmp = Integer.compare(b.getKey().length(), a.getKey().length());
                    if (lengthCmp != 0) return lengthCmp;
                    // Third: alphabetical order for ties
                    return a.getKey().compareTo(b.getKey());
                })
                // Limit to top 'popularWordCount' entries
                .limit(popularWordCount)
                // Collect to LinkedHashMap to preserve order
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,       // no merging needed
                        LinkedHashMap::new     // maintain insertion order
                ));
    }

    /**
     * A {@link Comparator} that sorts word count pairs correctly:
     *
     * <p>
     * <ol>
     *   <li>First sorting by word count, ranking more frequent words higher.</li>
     *   <li>Then sorting by word length, ranking longer words higher.</li>
     *   <li>Finally, breaking ties using alphabetical order.</li>
     * </ol>
     */
    private static final class WordCountComparator implements java.util.Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
            if (!a.getValue().equals(b.getValue())) {
                return b.getValue() - a.getValue();
            }
            if (a.getKey().length() != b.getKey().length()) {
                return b.getKey().length() - a.getKey().length();
            }
            return a.getKey().compareTo(b.getKey());
        }
    }

    private WordCounts() {
        // This class cannot be instantiated
    }
}
