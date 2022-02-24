package practice;

import java.util.Arrays;

/**
 * leetcode 45
 *
 * @author luoxin
 * @since 2022/1/27
 */
public class Jump {
  public int jump(int[] nums) {
    int length = nums.length;
    int[] dp = new int[length];
    dp[length-1] = 0;
    for (int i = length - 2; i >= 0; i--) {
      int min = 99999999;
      for (int j = 1; j <= nums[i] && i+j<length; j++) {
        min = Math.min(min, dp[i+j] + 1);
      }
      dp[i] = min;
    }
    return dp[0];
  }
  
  public int referenceAnswer(int[] nums) {
    int step = 0;
    int length = nums.length;
    int maxPosition = 0;
    int curEnd = 0;
    for (int i = 0; i < length-1; i++) {
      maxPosition = Math.max(maxPosition, i+nums[i]);
      if(curEnd == i) {
        step++;
        curEnd = maxPosition;
      }
    }
    return step;
  }
  
  public static void main(String[] args) {
    Jump jump = new Jump();
    System.out.println(jump.jump(new int[]{2,3,1,1,4}));
    System.out.println(jump.jump(new int[]{2,3,0,1,4}));
    System.out.println(jump.jump(new int[]{2,4}));
    System.out.println(jump.jump(new int[]{0}));
    System.out.println(jump.referenceAnswer(new int[]{2,3,1,1,4}));
    System.out.println(jump.referenceAnswer(new int[]{2,3,0,1,4}));
    System.out.println(jump.referenceAnswer(new int[]{2,4}));
    System.out.println(jump.referenceAnswer(new int[]{0}));
  }
}
