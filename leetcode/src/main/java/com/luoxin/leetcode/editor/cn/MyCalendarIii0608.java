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
 * 线段树，不带懒惰标记<br>
 * leetcode 显示超时，但根据下面 {@link #testValidate()} 结果显示，结果都是一样的<br>
 * 常规解法，参考 {@link MyCalendarIii0606}
 *
 * @date: 2022-06-08 20:37:12
 * @qid: 732
 * @title: 我的日程安排表 III
 */
public class MyCalendarIii0608 {
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
                        int first = random.nextInt(100000000) * 10;
                        int second = random.nextInt(1000000000);
                        int l = Math.min(first, second);
                        int r = Math.max(first, second);
                        assert sCurent.book(l, r) == sNormal.book(l, r);
                      });
              System.out.println(abjdskl + " " + times);
            });
  }
}
// leetcode submit region begin(Prohibit modification and deletion)
class MyCalendarThree {

  private Map<Integer, Integer> map = new HashMap<>();

  public MyCalendarThree() {}

  public int update(int start, int end, int left, int right, int index) {
    if (end < left || start > right) {
      return map.getOrDefault(index, 0);
    }
    if (left == right) {
      map.put(index, map.getOrDefault(index, 0) + 1);
      return map.get(index);
    }
    int mid = left + (right - left) / 2;
    int lc = update(start, end, left, mid, index * 2);
    int rc = update(start, end, mid + 1, right, index * 2 + 1);
    int max = Math.max(lc, rc);
    map.put(index, max);
    return max;
  }

  public int book(int start, int end) {
    return update(start, end - 1, 0, 1000000000, 1);
  }
}

  /**
   * Your MyCalendarThree object will be instantiated and called as such: MyCalendarThree obj = new
   * MyCalendarThree(); int param_1 = obj.book(start,end);
   */
  // leetcode submit region end(Prohibit modification and deletion)
