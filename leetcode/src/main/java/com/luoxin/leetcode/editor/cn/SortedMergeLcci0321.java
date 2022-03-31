//给定两个排序后的数组 A 和 B，其中 A 的末端有足够的缓冲空间容纳 B。 编写一个方法，将 B 合并入 A 并排序。 
//
// 初始化 A 和 B 的元素数量分别为 m 和 n。 
//
// 示例: 
//
// 输入:
//A = [1,2,3,0,0,0], m = 3
//B = [2,5,6],       n = 3
//
//输出: [1,2,2,3,5,6] 
//
// 说明: 
//
// 
// A.length == n + m 
// 
// Related Topics 数组 双指针 排序 👍 135 👎 0


package com.luoxin.leetcode.editor.cn;

import java.util.stream.IntStream;

/**
 * @date: 2022-03-21 00:09:37
 * 
 */
public class SortedMergeLcci0321 {
  public static void main(String[] args) {
       Solution solution = new SortedMergeLcci0321 ().new Solution();
  }
    //leetcode submit region begin(Prohibit modification and deletion)
class Solution {
      public void merge(int[] A, int m, int[] B, int n) {
        int index = A.length;
        while(m>0 && n>0) {
          if(A[m-1]> B[n-1]) {
            A[--index] = A[--m];
          }else {
            A[--index] = B[--n];
            
          }
        }
        while(n>0) {
          A[--index] = B[--n];
        }
      }
}
//leetcode submit region end(Prohibit modification and deletion)

}