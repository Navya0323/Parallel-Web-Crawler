package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A parallelized implementation of {@link WebCrawler} that uses {@link ForkJoinPool}
 * to concurrently scan and process multiple web pages.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock systemClock;
    private final Duration crawlDuration;
    private final int wordLimit;
    private final ForkJoinPool threadPool;
    private final List<Pattern> excludedPatterns;
    private final int crawlDepth;
    private final PageParserFactory parserProvider;

    @Inject
    ParallelWebCrawler(
            Clock systemClock,
            @Timeout Duration crawlDuration,
            @PopularWordCount int wordLimit,
            @TargetParallelism int threadCount,
            @IgnoredUrls List<Pattern> excludedPatterns,
            @MaxDepth int crawlDepth,
            PageParserFactory parserProvider) {
        this.systemClock = systemClock;
        this.crawlDuration = crawlDuration;
        this.wordLimit = wordLimit;
        this.threadPool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.excludedPatterns = excludedPatterns;
        this.crawlDepth = crawlDepth;
        this.parserProvider = parserProvider;
    }

    @Override
    public CrawlResult crawl(List<String> seedUrls) {
        Instant deadline = systemClock.instant().plus(crawlDuration);
        ConcurrentMap<String, Integer> wordCounts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visited = new ConcurrentSkipListSet<>();

        for (String url : seedUrls) {
            threadPool.invoke(new CrawlTask(url, deadline, crawlDepth, wordCounts, visited));
        }

        CrawlResult.Builder resultBuilder = new CrawlResult.Builder()
                .setUrlsVisited(visited.size());

        if (wordCounts.isEmpty()) {
            return resultBuilder.setWordCounts(wordCounts).build();
        }

        return resultBuilder
                .setWordCounts(WordCounts.sort(wordCounts, wordLimit))
                .build();
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    private class CrawlTask extends RecursiveTask<Boolean> {
        private final String currentUrl;
        private final Instant deadline;
        private final int depthRemaining;
        private final ConcurrentMap<String, Integer> wordCounts;
        private final ConcurrentSkipListSet<String> visited;

        CrawlTask(
                String currentUrl,
                Instant deadline,
                int depthRemaining,
                ConcurrentMap<String, Integer> wordCounts,
                ConcurrentSkipListSet<String> visited) {
            this.currentUrl = currentUrl;
            this.deadline = deadline;
            this.depthRemaining = depthRemaining;
            this.wordCounts = wordCounts;
            this.visited = visited;
        }

        @Override
        protected Boolean compute() {
            if (depthRemaining == 0 || systemClock.instant().isAfter(deadline)) {
                return false;
            }

            for (Pattern pattern : excludedPatterns) {
                if (pattern.matcher(currentUrl).matches()) {
                    return false;
                }
            }

            if (!visited.add(currentUrl)) {
                return false;
            }

            PageParser.Result parsed = parserProvider.get(currentUrl).parse();

            for (Map.Entry<String, Integer> entry : parsed.getWordCounts().entrySet()) {
                wordCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }

            List<CrawlTask> subtasks = parsed.getLinks().stream()
                    .map(link -> new CrawlTask(link, deadline, depthRemaining - 1, wordCounts, visited))
                    .toList();

            invokeAll(subtasks);
            return true;
        }
    }
}
