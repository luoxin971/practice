// 给定一个由非重叠的轴对齐矩形的数组 rects ，其中 rects[i] = [ai, bi, xi, yi] 表示 (ai, bi) 是第 i 个矩形的左
// 下角点，(xi, yi) 是第 i 个矩形的右上角点。设计一个算法来随机挑选一个被某一矩形覆盖的整数点。矩形周长上的点也算做是被矩形覆盖。所有满足要求的点必须等
// 概率被返回。
//
// 在给定的矩形覆盖的空间内的任何整数点都有可能被返回。
//
// 请注意 ，整数点是具有整数坐标的点。
//
// 实现 Solution 类:
//
//
// Solution(int[][] rects) 用给定的矩形数组 rects 初始化对象。
// int[] pick() 返回一个随机的整数点 [u, v] 在给定的矩形所覆盖的空间内。
//
//
//
//
//
//
//
// 示例 1：
//
//
//
//
// 输入:
// ["Solution", "pick", "pick", "pick", "pick", "pick"]
// [[[[-2, -2, 1, 1], [2, 2, 4, 6]]], [], [], [], [], []]
// 输出:
// [null, [1, -2], [1, -1], [-1, -2], [-2, -2], [0, 0]]
//
// 解释：
// Solution solution = new Solution([[-2, -2, 1, 1], [2, 2, 4, 6]]);
// solution.pick(); // 返回 [1, -2]
// solution.pick(); // 返回 [1, -1]
// solution.pick(); // 返回 [-1, -2]
// solution.pick(); // 返回 [-2, -2]
// solution.pick(); // 返回 [0, 0]
//
//
//
// 提示：
//
//
// 1 <= rects.length <= 100
// rects[i].length == 4
// -10⁹ <= ai < xi <= 10⁹
// -10⁹ <= bi < yi <= 10⁹
// xi - ai <= 2000
// yi - bi <= 2000
// 所有的矩形不重叠。
// pick 最多被调用 10⁴ 次。
//
// Related Topics 水塘抽样 数学 二分查找 有序集合 前缀和 随机化 👍 114 👎 0

package com.luoxin.leetcode.editor.cn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 这 jier 题 debug 不了，难受
 *
 * @date: 2022-06-09 21:34:28
 * @qid: 497
 * @title: 非重叠矩形中的随机点
 */
public class RandomPointInNonOverlappingRectangles0609 {
  public static void main(String[] args) {
    Solution solution = new RandomPointInNonOverlappingRectangles0609().new Solution(new int[1][]);
  }
  // leetcode submit region begin(Prohibit modification and deletion)
  class Solution {
    int[][] r;
    int size;
    List<Integer> pre = new ArrayList<>();

    public Solution(int[][] rects) {
      r = rects;
      size = rects.length;
      pre.add(0);
      for (int i = 0; i < rects.length; i++) {
        pre.add(pre.get(pre.size() - 1) + (r[i][2] - r[i][0] + 1) * (r[i][3] - r[i][1] + 1));
      }
    }

    public int[] pick() {
      int i = 0;
      int random = new Random().nextInt(pre.get(pre.size() - 1));
      for (int x = 0; x < pre.size() - 1; x++) {
        if (random >= pre.get(x) && random < pre.get(x + 1)) {
          i = x;
        }
      }

      int length = r[i][2] - r[i][0] + 1;
      int width = r[i][3] - r[i][1] + 1;

      return new int[] {
        r[i][0] + new Random().nextInt(length), r[i][1] + new Random().nextInt(width)
      };
    }
  }

  /**
   * Your Solution object will be instantiated and called as such: Solution obj = new
   * Solution(rects); int[] param_1 = obj.pick();
   */
  // leetcode submit region end(Prohibit modification and deletion)

}
