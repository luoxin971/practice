//给定一个二叉搜索树 root 和一个目标结果 k，如果 BST 中存在两个元素且它们的和等于给定的目标结果，则返回 true。 
//
// 
//
// 示例 1： 
//
// 
//输入: root = [5,3,6,2,4,null,7], k = 9
//输出: true
// 
//
// 示例 2： 
//
// 
//输入: root = [5,3,6,2,4,null,7], k = 28
//输出: false
// 
//
// 
//
// 提示: 
//
// 
// 二叉树的节点个数的范围是 [1, 10⁴]. 
// -10⁴ <= Node.val <= 10⁴ 
// root 为二叉搜索树 
// -10⁵ <= k <= 10⁵ 
// 
// Related Topics 树 深度优先搜索 广度优先搜索 二叉搜索树 哈希表 双指针 二叉树 👍 323 👎 0


package com.luoxin.leetcode.editor.cn;

import sun.reflect.generics.tree.Tree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * @date: 2022-03-21 00:16:06
 * 
 */
public class TwoSumIvInputIsABst0321 {
 
 public class TreeNode {
     int val;
     TreeNode left;
     TreeNode right;
     TreeNode() {}
     TreeNode(int val) { this.val = val; }
     TreeNode(int val, TreeNode left, TreeNode right) {
         this.val = val;
         this.left = left;
         this.right = right;
     }
 }
 
 public static void main(String[] args) {
       Solution solution = new TwoSumIvInputIsABst0321 ().new Solution();
  }
    //leetcode submit region begin(Prohibit modification and deletion)
/**
 * Definition for a binary tree node.
 * public class TreeNode {
 *     int val;
 *     TreeNode left;
 *     TreeNode right;
 *     TreeNode() {}
 *     TreeNode(int val) { this.val = val; }
 *     TreeNode(int val, TreeNode left, TreeNode right) {
 *         this.val = val;
 *         this.left = left;
 *         this.right = right;
 *     }
 * }
 */
class Solution {
    public boolean findTarget(TreeNode root, int k) {
      if(root == null) {
        return false;
      }
      if( k > 2* root.val) {
        return findTarget(root.right, k);
      }else if( k < root.val) {
        return findTarget(root.left, k);
      } else {
        Map<Integer, Boolean> map = new HashMap<>();
        Deque<TreeNode> deque = new ArrayDeque<>();
        deque.add(root);
        while (!deque.isEmpty()) {
          int size = deque.size();
          for (int i = 0; i < size; i++) {
            TreeNode pop = deque.pop();
            if(map.containsKey(pop.val)) {
              return true;
            } else {
              map.put(k - pop.val, true);
            }
            if(pop.left != null) {
              deque.add(pop.left);
            }
            if(pop.right !=null) {
              deque.add(pop.right);
            }
          }
        }
      }
      return false;
    }
}
//leetcode submit region end(Prohibit modification and deletion)

}