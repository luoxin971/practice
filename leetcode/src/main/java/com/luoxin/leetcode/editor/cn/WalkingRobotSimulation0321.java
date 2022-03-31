//机器人在一个无限大小的 XY 网格平面上行走，从点 (0, 0) 处开始出发，面向北方。该机器人可以接收以下三种类型的命令 commands ： 
//
// 
// -2 ：向左转 90 度 
// -1 ：向右转 90 度 
// 1 <= x <= 9 ：向前移动 x 个单位长度 
// 
//
// 在网格上有一些格子被视为障碍物 obstacles 。第 i 个障碍物位于网格点 obstacles[i] = (xi, yi) 。 
//
// 机器人无法走到障碍物上，它将会停留在障碍物的前一个网格方块上，但仍然可以继续尝试进行该路线的其余部分。 
//
// 返回从原点到机器人所有经过的路径点（坐标为整数）的最大欧式距离的平方。（即，如果距离为 5 ，则返回 25 ） 
//
// 
// 
// 
// 
// 
// 
//
// 
// 注意： 
//
// 
// 北表示 +Y 方向。 
// 东表示 +X 方向。 
// 南表示 -Y 方向。 
// 西表示 -X 方向。 
// 
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
//输入：commands = [4,-1,3], obstacles = []
//输出：25
//解释：
//机器人开始位于 (0, 0)：
//1. 向北移动 4 个单位，到达 (0, 4)
//2. 右转
//3. 向东移动 3 个单位，到达 (3, 4)
//距离原点最远的是 (3, 4) ，距离为 3² + 4² = 25 
//
// 示例 2： 
//
// 
//输入：commands = [4,-1,4,-2,4], obstacles = [[2,4]]
//输出：65
//解释：机器人开始位于 (0, 0)：
//1. 向北移动 4 个单位，到达 (0, 4)
//2. 右转
//3. 向东移动 1 个单位，然后被位于 (2, 4) 的障碍物阻挡，机器人停在 (1, 4)
//4. 左转
//5. 向北走 4 个单位，到达 (1, 8)
//距离原点最远的是 (1, 8) ，距离为 1² + 8² = 65 
//
// 
//
// 提示： 
//
// 
// 1 <= commands.length <= 10⁴ 
// commands[i] is one of the values in the list [-2,-1,1,2,3,4,5,6,7,8,9]. 
// 0 <= obstacles.length <= 10⁴ 
// -3 * 10⁴ <= xi, yi <= 3 * 10⁴ 
// 答案保证小于 2³¹ 
// 
// Related Topics 数组 模拟 👍 162 👎 0


package com.luoxin.leetcode.editor.cn;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @date: 2022-03-21 21:50:23
 * @qid: 874
 * @title: 模拟行走机器人
 */
public class WalkingRobotSimulation0321 {
    public static void main(String[] args) {
        Solution solution = new WalkingRobotSimulation0321().new Solution();
        int[] c = new int[]{4, -1, 4, -2, 4};
        int[][] ob = new int[][]{{2, 4}};
        System.out.println(solution.robotSim(c, ob));
    }

    //leetcode submit region begin(Prohibit modification and deletion)
    class Solution {
        Set<List<Integer>> ob = new HashSet<>();
        Set<int[]> path = new HashSet<>();

        public int robotSim(int[] commands, int[][] obstacles) {
            Arrays.stream(obstacles).forEach(x -> ob.add(Arrays.stream(x).boxed().collect(Collectors.toList())));
            int direction = 400000003;
            int[] cur = new int[]{0, 0};
            for (int command : commands) {
                // 左转
                if (command == -2) {
                    direction -= 1;
                } else if (command == -1) {
                    // 右转
                    direction += 1;
                } else {
                    cur = move(cur, command, direction);
                }
            }
            return path.stream().mapToInt(x -> x[0] * x[0] + x[1] * x[1]).max().orElse(0);
        }

        public int[] move(int[] current, int length, int direction) {
            int[] next = new int[2];
            for (int i = 0; i < length; i++) {
                switch (direction % 4) {
                    case 0:
                        next = new int[]{current[0] + 1, current[1]};
                        break;
                    case 2:
                        next = new int[]{current[0] - 1, current[1]};
                        break;
                    case 1:
                        next = new int[]{current[0], current[1] - 1};
                        break;
                    case 3:
                        next = new int[]{current[0], current[1] + 1};
                        break;
                }
                if (ob.contains(Arrays.stream(next).boxed().collect(Collectors.toList()))) {
                    return current;
                } else {
                    path.add(next);
                    current = next;
                }
            }
            return current;
        }

    }
//leetcode submit region end(Prohibit modification and deletion)

}