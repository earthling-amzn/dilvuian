/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 * Original credit to chflood: https://github.com/openjdk/jdk/pull/14185#issuecomment-1579278537
 */
package com.amazon.corretto.benchmark.diluvian;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Command
public class Diluvian implements Runnable {

  static class TreeNode {
    public TreeNode left, right;
    public int val;
  }

  @CommandLine.Option(names = {"-s", "--size"},
                      description = "The size of the cache")
  int cacheSize = 100;

  @CommandLine.Option(names = {"-r", "--repetitions"},
                      description = "Total number of cache entries to update")
  int reps = 100;

  @CommandLine.Option(names = {"-h", "--height"},
                      description = "Height of the tree to create for each entry")
  int treeHeight = 16;

  @CommandLine.Option(names = {"-a", "--random"},
                      description = "Randomize cache access (default is sequential/round robin)")
  boolean random = false;

  @CommandLine.Option(names = {"-t", "--threads"},
                      description = "Use this number of threads to update the cache")
  int threads = 1;

  private TreeNode[] trees;

  private int getIndex(int i) {
    if (random) {
      return ThreadLocalRandom.current().nextInt(0, cacheSize);
    }
    return i % cacheSize;
  }

  private static TreeNode makeTree(int h) {
    if (h == 0) {
      return null;
    }

    TreeNode res = new TreeNode();
    res.left = makeTree(h - 1);
    res.right = makeTree(h - 1);
    res.val = h;
    return res;
  }

  public static void main(String[] args) {
    new CommandLine(new Diluvian()).execute(args);
  }

  private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);
  private static Thread threadFactory(Runnable r) {
    Thread t = new Thread(r);
    t.setName("T" + THREAD_COUNTER.getAndIncrement());
    return t;
  }

  @Override
  public void run() {
    // Other things we could try:
    // 1. Make the threads countdown a shared reps, so the total number of updates remains the same
    // 2. Add some contention with a lock
    // 3. Set a target update-rate and/or fixed time for benchmark to run (throughput mode)
    trees = new TreeNode[cacheSize];

    long start = System.currentTimeMillis();
    try (ExecutorService executor = Executors.newFixedThreadPool(threads, Diluvian::threadFactory)) {
      for (int i = 0; i < threads; i++) {
        executor.execute(this::updateCache);
      }
      executor.shutdown();
      boolean terminated = executor.awaitTermination(1, TimeUnit.HOURS);
      if (!terminated) {
        System.err.println("Benchmark did not terminate");
        throw new RuntimeException("Benchmark did not terminate");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    long end = System.currentTimeMillis();
    System.out.println("Total time: " + (end - start) + "ms");
  }

  private void updateCache() {
    long start = System.currentTimeMillis();
    for (int i = 0; i < reps; i++) {
      trees[getIndex(i)] = makeTree(treeHeight);
    }
    long end = System.currentTimeMillis();
    long ms = end - start;

    System.out.println(Thread.currentThread().getName() + " took " + ms + "ms to allocate " + reps + " trees in a cache of " + cacheSize);
  }
}
