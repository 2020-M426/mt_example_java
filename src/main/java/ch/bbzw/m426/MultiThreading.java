package ch.bbzw.m426;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiThreading {
    public static void main(final String[] args) throws Exception {
        calc();
        http();
    }

    private static void calc() {
        final IntStream range = IntStream.range(30, 42);
        final IntStream rangeParallel = IntStream.range(30, 42).parallel();

        System.out.println("== Calc serial ==");
        Instant start = Instant.now();
        final int sum = range.map(MultiThreading::fib).sum();
        Instant end = Instant.now();
        System.out.printf("Duration: %s%n", Duration.between(start, end).toMillis());
        System.out.printf("isParallel? %b%n", range.isParallel());
        assert sum == 432148168;

        System.out.println();
        System.out.println("== Calc parallel ==");
        start = Instant.now();
        final int sumParallel = rangeParallel.map(MultiThreading::fib).sum();
        end = Instant.now();
        System.out.printf("Duration: %s%n", Duration.between(start, end).toMillis());
        System.out.printf("isParallel? %b%n", rangeParallel.isParallel());
        assert sumParallel == 432148168;
    }

    private static void http() throws Exception {
        final List<CompletableFuture<HttpResponse<Void>>> futures = new ArrayList<>();
        final List<HttpResponse<Void>> responseList = new ArrayList<>();
        final HttpClient client = HttpClient.newBuilder().executor(ForkJoinPool.commonPool()).build();
        final int httpRequests = 50;

        for (int i = 0; i < httpRequests; i++) {
            final String url = String.format("https://httpbin.org/status/%d", 200 + i);
            futures.add(client.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.discarding()));
        }

        System.out.println();
        System.out.println("== HTTP parallel ==");
        Instant start = Instant.now();
        final Set<Integer> codes = futures.stream().map(CompletableFuture::join).map(HttpResponse::statusCode).collect(Collectors.toUnmodifiableSet());
        Instant end = Instant.now();
        System.out.printf("Duration: %s%n", Duration.between(start, end).toMillis());
        assert codes.size() == 50;

        System.out.println();
        System.out.println("== HTTP serial ==");
        start = Instant.now();
        for (int i = 0; i < httpRequests; i++) {
            final String url = String.format("https://httpbin.org/status/%d", 200 + i);
            responseList.add(client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.discarding()));
        }
        final Set<Integer> statusCodes = responseList.stream().map(HttpResponse::statusCode).collect(Collectors.toUnmodifiableSet());
        end = Instant.now();
        System.out.printf("Duration: %s%n", Duration.between(start, end).toMillis());
        assert statusCodes.size() == 50;
    }

    private static int fib(final int n) {
        if (n <= 1) {
            return n;
        }
        return fib(n - 1) + fib(n - 2);
    }
}
