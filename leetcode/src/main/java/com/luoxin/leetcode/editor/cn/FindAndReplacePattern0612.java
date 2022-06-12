// 你有一个单词列表 words 和一个模式 pattern，你想知道 words 中的哪些单词与模式匹配。
//
// 如果存在字母的排列 p ，使得将模式中的每个字母 x 替换为 p(x) 之后，我们就得到了所需的单词，那么单词与模式是匹配的。
//
// （回想一下，字母的排列是从字母到字母的双射：每个字母映射到另一个字母，没有两个字母映射到同一个字母。）
//
// 返回 words 中与给定模式匹配的单词列表。
//
// 你可以按任何顺序返回答案。
//
//
//
// 示例：
//
// 输入：words = ["abc","deq","mee","aqq","dkd","ccc"], pattern = "abb"
// 输出：["mee","aqq"]
// 解释：
// "mee" 与模式匹配，因为存在排列 {a -> m, b -> e, ...}。
// "ccc" 与模式不匹配，因为 {a -> c, b -> c, ...} 不是排列。
// 因为 a 和 b 映射到同一个字母。
//
//
//
// 提示：
//
//
// 1 <= words.length <= 50
// 1 <= pattern.length = words[i].length <= 20
//
// Related Topics 数组 哈希表 字符串 👍 129 👎 0

package com.luoxin.leetcode.editor.cn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 哈希表<br>
 * 简单到难以置信
 *
 * @date: 2022-06-12 00:07:33
 * @qid: 890
 * @title: 查找和替换模式
 */
public class FindAndReplacePattern0612 {
  public static void main(String[] args) {
    Solution solution = new FindAndReplacePattern0612().new Solution();
  }
  // leetcode submit region begin(Prohibit modification and deletion)
  class Solution {
    public List<String> findAndReplacePattern(String[] words, String pattern) {
      List<String> res = new ArrayList<>();
      for (String word : words) {
        if (isMatch(word, pattern)) {
          res.add(word);
        }
      }
      return res;
    }

    public boolean isMatch(String word, String pattern) {
      int length = word.length();
      Map<Character, Character> map = new HashMap<>(length);
      for (int i = 0; i < length; i++) {
        char wch = word.charAt(i);
        char pch = pattern.charAt(i);
        if (!map.containsKey(wch) && !map.containsValue(pch)) {
          map.put(wch, pch);
        } else if (map.containsKey(wch) && pch == map.get(wch)) {
          continue;
        } else {
          return false;
        }
      }
      return true;
    }
  }
  // leetcode submit region end(Prohibit modification and deletion)

}
