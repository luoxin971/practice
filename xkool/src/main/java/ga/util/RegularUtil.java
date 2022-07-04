package ga.util;

import com.xkool.algo.util.plotter.XkPlotter;
import ga.constant.JtsConstant;
import org.apache.commons.lang.math.RandomUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/31
 */
public class RegularUtil {
  public static final int MAX_POINT_NUM = 1000;

  public static double getRegularScore(List<Point> points) {
    Polygon hull = PointUtil.createConvexHullFromPoints(points);
    return 0;
  }

  private static double getQValue(List<Point> points, int k) {
    Map<Integer, LinkedHashMap<Integer, Double>> map = getMapInPoints(points);
    double theta = getTheta(points.size());
    double sum =
        map.values().stream()
            .mapToDouble(
                integerDoubleLinkedHashMap ->
                    integerDoubleLinkedHashMap.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .limit(k)
                        .map(x -> 1 - getPE(x, theta))
                        .sum())
            .sum();
    return sum / (k * points.size());
  }

  static Map<Integer, LinkedHashMap<Integer, Double>> getMapInPoints(List<Point> points) {
    List<Point> ps = processPoints(points);
    Map<Integer, LinkedHashMap<Integer, Double>> map = new HashMap<>();
    ps.forEach(
        p -> {
          LinkedHashMap<Integer, Double> linkedHashMap = new LinkedHashMap<>();
          Map<Integer, Double> integerDoubleMap =
              ps.stream()
                  .filter(x -> !p.equals(x))
                  .collect(Collectors.toMap(x -> (int) x.getUserData(), x -> x.distance(p)));
          integerDoubleMap.entrySet().stream()
              .sorted(Comparator.comparingDouble(Map.Entry::getValue))
              .forEach(entry -> linkedHashMap.put(entry.getKey(), entry.getValue()));
          map.put((int) p.getUserData(), linkedHashMap);
        });
    return map;
  }

  static List<Point> processPoints(List<Point> points) {
    AtomicInteger data = new AtomicInteger(MAX_POINT_NUM);
    return points.stream()
        .filter(p -> Objects.isNull(p.getUserData()))
        .map(
            p -> {
              Point point = (Point) p.copy();
              point.setUserData(data.incrementAndGet());
              return point;
            })
        .collect(Collectors.toList());
  }

  private static double getPE(double distance, double theta) {
    return theta / (theta + distance * distance);
  }

  private static double getPE(Point p1, Point p2, double theta) {
    double distance = p1.distance(p2);
    return theta / (theta + distance * distance);
  }

  private static double getTheta(int count) {
    double rc2 = 1.0 / count;
    return 3 * rc2;
  }

  public static void main(String[] args) {
    List<Point> regularPoints = generateRegularPoints();
    List<Point> randomPoints = generateRandomPoints();
    XkPlotter xkPlotter = new XkPlotter();
    randomPoints.forEach(x -> xkPlotter.addContent(x, Color.black));
    xkPlotter.plot();
    XkPlotter xkPlotter2 = new XkPlotter();
    regularPoints.forEach(x -> xkPlotter2.addContent(x, Color.black));
    xkPlotter2.plot();
    System.out.println("k==2");
    System.out.println(getQValue(regularPoints, 2));
    System.out.println(getQValue(randomPoints, 2));
    System.out.println("k==3");
    System.out.println(getQValue(regularPoints, 3));
    System.out.println(getQValue(randomPoints, 3));
    System.out.println("k==5");
    System.out.println(getQValue(regularPoints, 5));
    System.out.println(getQValue(randomPoints, 5));
    System.out.println("k==8");
    System.out.println(getQValue(regularPoints, 8));
    System.out.println(getQValue(randomPoints, 8));
  }

  private static List<Point> generateRegularPoints() {
    Point point = JtsConstant.GEOMETRY_FACTORY_FLOATING.createPoint(new Coordinate(0, 0));
    List<Point> list = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        list.add(PointUtil.movePoint(point, i, j));
      }
    }
    return list;
  }

  private static List<Point> generateRandomPoints() {
    Point point = JtsConstant.GEOMETRY_FACTORY_FLOATING.createPoint(new Coordinate(0, 0));
    List<Point> list = new ArrayList<>();
    IntStream.range(0, 20)
        .forEach(
            i ->
                list.add(
                    PointUtil.movePoint(
                        point,
                        ((double) RandomUtils.nextInt(1000)) / 100.0,
                        ((double) RandomUtils.nextInt(800)) / 100.0)));
    return list;
  }
}
