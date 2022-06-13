package com.luoxin.leetcode.editor.cn;

import java.util.*;
import java.util.stream.IntStream;

/** 未完成 */
public class CountDifferentPalindromicSubsequences0610 {
  public static void main(String[] args) {
    Solution solution = new CountDifferentPalindromicSubsequences0610().new Solution();
    solution.countPalindromicSubsequences(
        "abcdabcdabcdabcdabcdabcdabcdabcddcbadcbadcbadcbadcbadcbadcbadcba");
  }

  // leetcode submit region begin(Prohibit modification and deletion)
  class Solution {

    private Map<Integer, Set<String>> map = new HashMap<>();

    public int countPalindromicSubsequences(String s) {
      int length = s.length();
      for (int i = 0; i < length; i++) {
        char center = s.charAt(i);
        Set<String> set = map.getOrDefault(1, new HashSet<>());
        set.add(String.valueOf(center));
        map.put(1, set);
        h(String.valueOf(center), s.substring(0, i), s.substring(i + 1));
      }

      Map<Character, List<Integer>> s2m = new HashMap<>();
      IntStream.range(0, s.length())
          .forEach(
              x -> {
                List<Integer> list = s2m.getOrDefault(s.charAt(x), new ArrayList<>());
                list.add(x);
                s2m.put(s.charAt(x), list);
              });
      s2m.forEach(
          (key, value) -> {
            int size = value.size();
            if (size >= 2) {
              String cs = key + String.valueOf(key);
              Set<String> sss = map.getOrDefault(2, new HashSet<>());
              sss.add(cs);
              map.put(2, sss);
              int mid = size / 2;
              h(cs, s.substring(0, value.get(mid - 1)), s.substring(mid + 1));
              if (size % 2 == 1) {
                h(cs, s.substring(0, mid), s.substring(mid + 2));
              }
            }
          });
      return (int) map.values().stream().mapToLong(Set::size).sum() % 1000000007;
    }

    public void h(String str, String left, String right) {
      Map<Character, Integer> m = helper(left, right);
      if (m.isEmpty()) {
        return;
      }
      Set<String> set = map.getOrDefault(str.length() + 2, new HashSet<>());
      m.forEach(
          (key, value) -> {
            String nextStr = key + str + key;
            set.add(nextStr);
            int x1 = value / 10000;
            int x2 = value % 10000;
            String sl = left.substring(0, x1);
            String sr = right.substring(right.length() - x2);
            h(nextStr, sl, sr);
          });
      map.put(str.length() + 2, set);
    }

    public Map<Character, Integer> helper(String left, String right) {
      int ll = left.length();
      Map<Character, Integer> res = new HashMap<>(ll);
      if (left.isEmpty() || right.isEmpty()) {
        return res;
      }
      Map<Character, Integer> map = new HashMap<>(ll);
      IntStream.range(0, ll).forEach(i -> map.put(left.charAt(i), i));
      int rl = right.length();
      IntStream.range(0, rl)
          .forEach(
              i -> {
                char ch = right.charAt(i);
                if (map.containsKey(ch)) {
                  res.put(ch, map.get(ch) * 10000 + rl - i);
                }
              });
      return res;
    }
  }
  // leetcode submit region end(Prohibit modification and deletion)

}
