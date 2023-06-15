/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 * Original credit to chflood: https://github.com/openjdk/jdk/pull/14185#issuecomment-1579278537
 */
package com.amazon.corretto.benchmark.diluvian;

import picocli.CommandLine;
import picocli.CommandLine.Command;

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
                      description = "Height of the tree to create for each entry.")
  int treeHeight = 16;

  private TreeNode[] trees;

  private int getIndex(int i) {return i % cacheSize;}

  private static TreeNode makeTree(int h) {
    if (h == 0) { return null;}
    else {
      TreeNode res = new TreeNode();
      res.left = makeTree(h - 1);
      res.right = makeTree(h - 1);
      res.val = h;
      return res;
    }
  }

  public static void main(String[] args) {
    new CommandLine(new Diluvian()).execute(args);
  }

  @Override
  public void run() {

    trees = new TreeNode[cacheSize];

    long start = System.currentTimeMillis();
    for (int i = 0; i < reps; i++)
      trees[getIndex(i)] = makeTree(treeHeight);
    long end = System.currentTimeMillis();
    long ms = end - start;

    System.out.println("Took " + ms + "ms to allocate " + reps + " trees in a cache of " + cacheSize);
  }
}
