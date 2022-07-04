package ga.regular;

import ga.constant.JtsConstant;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/31
 */
public class RegularScore {
  public static final int MAX_POINT_NUM = 1000;
  List<Point> points;

  public List<Point> getPoints() {
    return points;
  }

  public void setPoints(List<Point> points) {
    processPoints(points);
    this.points = points;
  }

  void processPoints(List<Point> points) {
    AtomicInteger data = new AtomicInteger(MAX_POINT_NUM);
    points.stream()
        .filter(p -> Objects.nonNull(p.getUserData()))
        .forEach(p -> p.setUserData(data.incrementAndGet()));
  }

  Map<Integer, Double> getDistanceMap(List<Point> points) {
    processPoints(points);
    int size = points.size();
    Map<Integer, Double> distanceMap = new HashMap<>(size * size);
    IntStream.range(0, size)
        .forEach(
            i ->
                IntStream.range(i, size)
                    .forEach(
                        j -> {
                          int iData = (int) points.get(i).getUserData();
                          int jData = (int) points.get(j).getUserData();
                          int min = Integer.min(iData, jData);
                          int max = Integer.max(iData, jData);
                          int key = min * MAX_POINT_NUM + max;
                          distanceMap.put(key, points.get(i).distance(points.get(j)));
                        }));
    return distanceMap;
  }

  Map<Integer, Double> getOnePointDistanceMap(Map<Integer, Double> allMap, Point point) {
    Map<Integer, Double> map = new LinkedHashMap<>();
    int pointData = (int) point.getUserData();
    allMap.entrySet().stream()
        .filter(
            entry ->
                entry.getKey() % MAX_POINT_NUM == pointData
                    || entry.getKey() / MAX_POINT_NUM == pointData)
        .sorted(Comparator.comparingDouble(Map.Entry::getValue))
        .forEach(
            entry -> {
              int key =
                  entry.getKey() % MAX_POINT_NUM == pointData
                      ? entry.getKey() / MAX_POINT_NUM
                      : entry.getKey() % MAX_POINT_NUM;
              map.put(key, entry.getValue());
            });
    return map;
  }

  public static void main(String[] args) {
    Coordinate c1 = new Coordinate(0, 0);
    Point point = JtsConstant.GEOMETRY_FACTORY_FLOATING.createPoint(c1);
    Point point2 = JtsConstant.GEOMETRY_FACTORY_FLOATING.createPoint(c1);
    System.out.println(point.getSRID());
    System.out.println(point2.getSRID());
    System.out.println(point.getUserData());
    System.out.println(point2.getUserData());
  }
}
