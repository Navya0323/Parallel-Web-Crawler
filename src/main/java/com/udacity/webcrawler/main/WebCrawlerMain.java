package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Entry point for running the web crawler.
 * Loads configuration, performs crawling, and writes results and profiling data.
 */
public final class WebCrawlerMain {

    private final CrawlerConfiguration config;

    private WebCrawlerMain(CrawlerConfiguration config) {
        this.config = Objects.requireNonNull(config);
    }

    @Inject
    private WebCrawler crawler;

    @Inject
    private Profiler profiler;

    /**
     * Executes the crawl and handles output writing.
     */
    private void run() throws Exception {
        // Inject dependencies using Guice
        Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

        // Perform the crawl
        CrawlResult result = crawler.crawl(config.getStartPages());
        CrawlResultWriter resultWriter = new CrawlResultWriter(result);

        // Write crawl results to file or console
        if (!config.getResultPath().isEmpty()) {
            Path resultPath = Path.of(config.getResultPath());
            resultWriter.write(resultPath);
        } else {
            // Avoid closing System.out — use flush instead of close
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
            resultWriter.write(writer);
            writer.flush();
        }

        // Write profiling data to file or console
        if (!config.getProfileOutputPath().isEmpty()) {
            Path profilePath = Path.of(config.getProfileOutputPath());
            profiler.writeData(profilePath);
        } else {
            // Avoid closing System.out — use flush instead of close
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(System.out));
            profiler.writeData(writer);
            writer.flush();
        }
    }

    /**
     * Main method to launch the crawler with a config file path.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: WebCrawlerMain [config-file-path]");
            return;
        }

        CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
        new WebCrawlerMain(config).run();
    }
}
