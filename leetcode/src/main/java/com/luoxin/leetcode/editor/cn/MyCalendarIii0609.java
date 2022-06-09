// 当 k 个日程安排有一些时间上的交叉时（例如 k 个日程安排都在同一时间内），就会产生 k 次预订。
//
// 给你一些日程安排 [start, end) ，请你在每个日程安排添加后，返回一个整数 k ，表示所有先前日程安排会产生的最大 k 次预订。
//
// 实现一个 MyCalendarThree 类来存放你的日程安排，你可以一直添加新的日程安排。
//
//
// MyCalendarThree() 初始化对象。
// int book(int start, int end) 返回一个整数 k ，表示日历中存在的 k 次预订的最大值。
//
//
//
//
// 示例：
//
//
// 输入：
// ["MyCalendarThree", "book", "book", "book", "book", "book", "book"]
// [[], [10, 20], [50, 60], [10, 40], [5, 15], [5, 10], [25, 55]]
// 输出：
// [null, 1, 1, 2, 3, 3, 3]
//
// 解释：
// MyCalendarThree myCalendarThree = new MyCalendarThree();
// myCalendarThree.book(10, 20); // 返回 1 ，第一个日程安排可以预订并且不存在相交，所以最大 k 次预订是 1 次预订。
// myCalendarThree.book(50, 60); // 返回 1 ，第二个日程安排可以预订并且不存在相交，所以最大 k 次预订是 1 次预订。
// myCalendarThree.book(10, 40); // 返回 2 ，第三个日程安排 [10, 40) 与第一个日程安排相交，所以最大 k 次预订是
// 2 次预订。
// myCalendarThree.book(5, 15); // 返回 3 ，剩下的日程安排的最大 k 次预订是 3 次预订。
// myCalendarThree.book(5, 10); // 返回 3
// myCalendarThree.book(25, 55); // 返回 3
//
//
//
//
// 提示：
//
//
// 0 <= start < end <= 10⁹
// 每个测试用例，调用 book 函数最多不超过 400次
//
// Related Topics 设计 线段树 有序集合 👍 166 👎 0

package com.luoxin.leetcode.editor.cn;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 线段树，带懒惰标记<br>
 * 还是没能独立写完，之后可以在回头继续看看 <br>
 * 常规解法，参考 {@link MyCalendarIii0606}
 *
 * @date: 2022-06-09 20:10:41
 * @qid: 732
 * @title: 我的日程安排表 III
 */
public class MyCalendarIii0609 {
  public static void main(String[] args) {
    testValidate();
    MyCalendarThree mc = new MyCalendarThree();

    // 本地跑会内 OutOfMemoryError
    // System.out.println(mc.book(10, 1000000000));

    IntStream.range(100, 300).forEach(i -> mc.book(0, i));
    System.out.println(mc.book(10, 20));
    System.out.println(mc.book(50, 60));
    System.out.println(mc.book(10, 40));
    System.out.println(mc.book(5, 15));
    System.out.println(mc.book(5, 10));
    System.out.println(mc.book(25, 55));
  }

  /** 跟 {@link MyCalendarIii0606} 对比，测试本方法是否有效 */
  static void testValidate() {
    // 测试 1000 次
    IntStream.range(0, 1000)
        .forEach(
            abjdskl -> {
              // 当前的 solution
              MyCalendarThree sCurent = new MyCalendarThree();
              // 0606 的 solution
              MyCalendarIii0606.MyCalendarThree sNormal = new MyCalendarIii0606.MyCalendarThree();
              // book 调用的次数，400 次以内
              int times = new Random().nextInt(400);
              IntStream.range(0, times)
                  .forEach(
                      djsflasjdfl -> {
                        Random random = new Random();
                        int first = random.nextInt(10000000) * 100;
                        int second = random.nextInt(1000000000);
                        int l = Math.min(first, second);
                        int r = Math.max(first, second);
                        int res1 = sCurent.book(l, r);
                        int res2 = sNormal.book(l, r);
                        if (res1 != res2) {
                          System.out.println("-----------------------");
                        }
                        assert res1 == res2;
                      });
              System.out.println(abjdskl + " " + times);
            });
  }
  // leetcode submit region begin(Prohibit modification and deletion)
  static class MyCalendarThree {

    Map<Integer, Integer> segmentMap = new HashMap<>();
    Map<Integer, Integer> lazyMap = new HashMap<>();

    public MyCalendarThree() {}

    public void update(int start, int end, int left, int right, int index) {
      if (end < left || start > right) {
        return;
      }
      if (start <= left && right <= end) {
        segmentMap.put(index, segmentMap.getOrDefault(index, 0) + 1);
        // 懒惰标记只在这一个地方 put，始终没有更新 child 的值，而是为了服务于父节点的
        // 这种方式依情况而定，本题中，不需要求解某个区间的值，而是总是求最大值
        lazyMap.put(index, lazyMap.getOrDefault(index, 0) + 1);
        return;
      }
      int mid = left + (right - left) / 2;
      update(start, end, left, mid, 2 * index);
      update(start, end, mid + 1, right, 2 * index + 1);
      // 懒惰标记与当前节点无关
      segmentMap.put(
          index,
          lazyMap.getOrDefault(index, 0)
              + Math.max(
                  segmentMap.getOrDefault(2 * index, 0),
                  segmentMap.getOrDefault(2 * index + 1, 0)));
    }

    public int book(int start, int end) {

      update(start, end - 1, 0, 1000000000, 1);
      return segmentMap.getOrDefault(1, 0);
    }
  }

  /**
   * Your MyCalendarThree object will be instantiated and called as such: MyCalendarThree obj = new
   * MyCalendarThree(); int param_1 = obj.book(start,end);
   */
  // leetcode submit region end(Prohibit modification and deletion)

}
