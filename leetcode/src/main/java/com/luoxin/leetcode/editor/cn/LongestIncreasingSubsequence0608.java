// 给你一个整数数组 nums ，找到其中最长严格递增子序列的长度。
//
// 子序列 是由数组派生而来的序列，删除（或不删除）数组中的元素而不改变其余元素的顺序。例如，[3,6,2,7] 是数组 [0,3,1,6,2,2,7] 的子
// 序列。
//
//
// 示例 1：
//
//
// 输入：nums = [10,9,2,5,3,7,101,18]
// 输出：4
// 解释：最长递增子序列是 [2,3,7,101]，因此长度为 4 。
//
//
// 示例 2：
//
//
// 输入：nums = [0,1,0,3,2,3]
// 输出：4
//
//
// 示例 3：
//
//
// 输入：nums = [7,7,7,7,7,7,7]
// 输出：1
//
//
//
//
// 提示：
//
//
// 1 <= nums.length <= 2500
// -10⁴ <= nums[i] <= 10⁴
//
//
//
//
// 进阶：
//
//
// 你能将算法的时间复杂度降低到 O(n log(n)) 吗?
//
// Related Topics 数组 二分查找 动态规划 👍 2562 👎 0

package com.luoxin.leetcode.editor.cn;

import java.util.ArrayList;
import java.util.List;

/**
 * @date: 2022-06-08 23:59:05
 * @qid: 300
 * @title: 最长递增子序列
 */
public class LongestIncreasingSubsequence0608 {
  public static void main(String[] args) {
    Solution solution = new LongestIncreasingSubsequence0608().new Solution();

    int[] a1 = {10, 9, 2, 5, 3, 7, 101, 18};
    int[] a2 = {0, 1, 0, 3, 2, 3};
    int[] a3 = {7, 7, 7, 7, 7, 7};
    assert solution.lengthOfLIS(a1) == lengthOfLIS(a1);
    assert solution.lengthOfLIS(a2) == lengthOfLIS(a2);
    assert solution.lengthOfLIS(a3) == lengthOfLIS(a3);
  }

  /**
   * {@link Solution} 里用的 dp，时间复杂度 O(n^2) <br>
   * 现在给一个 nlogn 时间复杂度的方法
   */
  public static int lengthOfLIS(int[] nums) {
    List<Integer> list = new ArrayList<>();
    list.add(nums[0]);
    for (int i = 1; i < nums.length; i++) {
      if (nums[i] > list.get(list.size() - 1)) {
        list.add(nums[i]);
        continue;
      }
      int index = binary_search(list, nums[i]);
      list.set(index, nums[i]);
    }

    return list.size();
  }
  /*
   * 二分
   */
  public static int binary_search(List<Integer> list, int target) {
    int left = 0;
    int right = list.size();
    while (left < right) {
      int mid = left + (right - left) / 2;
      if (list.get(mid) == target) {
        return mid;
      } else if (list.get(mid) > target) {
        right = mid;
      } else {
        left = mid + 1;
      }
    }
    return left;
  }
  // leetcode submit region begin(Prohibit modification and deletion)
  class Solution {
    public int lengthOfLIS(int[] nums) {
      int length = nums.length;
      int[] dp = new int[length];
      dp[0] = 1;
      int globalMax = 1;
      for (int i = 1; i < length; i++) {
        int preMax = 0;
        for (int j = 0; j < i; j++) {
          if (nums[j] < nums[i]) {
            preMax = Math.max(preMax, dp[j]);
          }
        }
        dp[i] = preMax + 1;
        globalMax = Math.max(globalMax, dp[i]);
      }
      return globalMax;
    }
  }
  // leetcode submit region end(Prohibit modification and deletion)

}
