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
    double sum =
        map.values().stream()
            .mapToDouble(
                integerDoubleLinkedHashMap ->
                    integerDoubleLinkedHashMap.values().stream()
                        .mapToDouble(Double::doubleValue)
                        .limit(k)
                        .map(x -> x * x)
                        .sum())
            .sum();
    return sum / (k * points.size());
  }

  static Map<Integer, LinkedHashMap<Integer, Double>> getMapInPoints(List<Point> points) {
    processPoints(points);
    Map<Integer, LinkedHashMap<Integer, Double>> map = new HashMap<>();
    points.forEach(
        p -> {
          LinkedHashMap<Integer, Double> linkedHashMap = new LinkedHashMap<>();
          Map<Integer, Double> integerDoubleMap =
              points.stream()
                  .filter(x -> !p.equals(x))
                  .collect(Collectors.toMap(x -> (int) x.getUserData(), x -> x.distance(p)));
          integerDoubleMap.entrySet().stream()
              .sorted(Comparator.comparingDouble(Map.Entry::getValue))
              .forEach(entry -> linkedHashMap.put(entry.getKey(), entry.getValue()));
          map.put((int) p.getUserData(), linkedHashMap);
        });
    return map;
  }

  static void processPoints(List<Point> points) {
    AtomicInteger data = new AtomicInteger(MAX_POINT_NUM);
    points.stream()
        .filter(p -> Objects.nonNull(p.getUserData()))
        .forEach(p -> p.setUserData(data.incrementAndGet()));
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
    List<Point> points = generateRegularPoints();
    System.out.println(getQValue(points, 5));
    XkPlotter xkPlotter = new XkPlotter();
    points.forEach(x -> xkPlotter.addContent(x, Color.black));
    xkPlotter.plot();
    System.out.println(getQValue(generateRandomPoints(), 5));
  }

  private static List<Point> generateRegularPoints() {
    Point point = JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPoint(new Coordinate(0, 0));
    List<Point> list = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      for (int j = 0; j < 4; j++) {
        list.add(PointUtil.movePoint(point, i, j));
      }
    }
    return list;
  }

  private static List<Point> generateRandomPoints() {
    Point point = JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPoint(new Coordinate(0, 0));
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
