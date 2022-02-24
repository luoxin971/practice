package practice;

/**
 * content
 *
 * @author luoxin
 * @since 2022/2/10
 */
public class MaxContinueBool {
  public int getMaxContinue(boolean[] array) {
    int current = 0;
    int max = 0;
    for (boolean b : array) {
      if (b) {
        current++;
        max = Math.max(max, current);
      } else {
        current = 0;
      }
    }
    return max;
  }

  public static void main(String[] args) {
    boolean[] array = {
      true, true, true, true, true, true, true, true, false, false, false, false, false, true, true,
      true, true, true, true, true, false, false
    };
    System.out.println(new MaxContinueBool().getMaxContinue(array));
  }
}
