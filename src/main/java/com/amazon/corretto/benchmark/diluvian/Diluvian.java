/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 * Original credit to chflood: https://github.com/openjdk/jdk/pull/14185#issuecomment-1579278537
 */
package com.amazon.corretto.benchmark.diluvian;

public class Diluvian {

  static class TreeNode {
    public TreeNode left, right;
    public int val;
  }

  static int cache_size;
  static int reps;
  static int tree_height=16;

  private static TreeNode[] trees;

  private static int getIndex(int i) {return i % cache_size;}

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
    if (args.length != 2) {
      System.err.println("LRU requires args: cache_size reps");
      return;
    }
    cache_size = Integer.parseInt(args[0]);
    reps = Integer.parseInt(args[1]) * cache_size;
    trees = new TreeNode[cache_size];

    long start = System.currentTimeMillis();
    for (int i = 0; i < reps; i++)
      trees[getIndex(i)] = makeTree(tree_height);
    long end = System.currentTimeMillis();
    long ms = end - start;

    System.out.println("Took " + ms + "ms to allocate " + reps + " trees in a cache of " + cache_size);
  }
}
