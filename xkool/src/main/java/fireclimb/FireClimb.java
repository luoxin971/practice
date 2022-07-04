package fireclimb;

import com.xkool.algo.util.geometry.XkCoordinateUtil;
import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import com.xkool.algo.util.geometry.XkGeometryUtil;
import com.xkool.algo.util.geometry.XkLineSegmentUtil;
import ga.constant.JtsConstant;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 消防登高，初步完成整个流程，需要进一步优化 <br>
 *
 * <ul>
 *   <li>两个可行线段相连的情况
 *   <li>重新梳理 ccw 相关的问题
 *   <li>在复杂楼型上验证
 * </ul>
 *
 * <p>TODO 1. 轮廓剔除短边 2. 凹角处两条线段的处理及后续逻辑调整
 *
 * @author luoxin
 * @since 2022/6/1
 */
public class FireClimb {

  private Polygon p;
  private Polygon buffer5;
  private Polygon buffer15;

  public static void main(String[] args) {
    FireClimb fireClimb = new FireClimb();
    List<Polygon> polygons = read();
    fireClimb.solution(polygons.get(1));
  }

  public void solution(Polygon polygon) {
    // 初始化三个 polygon
    p = (Polygon) JtsConstant.GEOMETRY_FACTORY_FLOATING.createGeometry(polygon);
    p = Orientation.isCCW(p.getCoordinates()) ? p : p.reverse();
    buffer15 = (Polygon) p.buffer(15, -100);
    buffer5 = (Polygon) buffer15.buffer(-10, -100);
    buffer15 = Orientation.isCCW(buffer15.getCoordinates()) ? buffer15 : buffer15.reverse();
    buffer5 = Orientation.isCCW(buffer5.getCoordinates()) ? buffer5 : buffer5.reverse();
    p = (Polygon) buffer5.buffer(-5, -100);
    p = Orientation.isCCW(p.getCoordinates()) ? p : p.reverse();

    List<LineString> faces = generateLines(p);
    List<Double> values = faces.stream().map(LineString::getLength).collect(Collectors.toList());
    List<Double> allBlankArea = getAllBlankArea(faces, buffer5, buffer15);
    Polygon m = (Polygon) new MinimumDiameter(p).getMinimumRectangle();
    LineSegment l1 = new LineSegment(m.getCoordinates()[0], m.getCoordinates()[1]);
    LineSegment l2 = new LineSegment(m.getCoordinates()[1], m.getCoordinates()[2]);
    Map<int[], Double> map =
        circleSlideWindow(values, allBlankArea, Math.max(l1.getLength(), l2.getLength()));
    System.out.println(map);
  }

  /**
   *
   *
   * <ol>
   *   <li>找到 lines[i] 的终点 li，在 bufferMore 上的垂足 vi，连接 li vi
   *   <li>找到 lines[i+1] 的起点 lj，在 bufferMore 上的垂足 vj，连接 vj lj
   *   <li>bufferMore 从 vi 到 vj 的一段 折线 vi vj
   *   <li>buffer5 从 lj 到 li 的一段 折线 lj li
   *   <li>polygon = li vi + 折 vi vj + vj lj + 折 lj li
   * </ol>
   *
   * @param lines 需要都在 bufferLittle 上，且顺序按照逆时针
   * @param bufferLittle
   * @param bufferMore
   * @return
   */
  public List<Double> getAllBlankArea(
      List<LineString> lines, Polygon bufferLittle, Polygon bufferMore) {
    lines = lines.stream().filter(x -> !x.isEmpty()).collect(Collectors.toList());
    List<Double> res = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      LineString firstLine = lines.get(i);
      LineString secondLine = lines.get((i + 1) % lines.size());

      // bufferLittle 上的第一个点
      Point firstPointInL = firstLine.getEndPoint();
      // 垂直方向
      LineSegment direction =
          XkLineSegmentUtil.rotateLineSegment(
              XkLineSegmentUtil.getLineSegmentOfLineSegmentString(firstLine), firstPointInL, -90);
      // firstPointInL 在 bufferMore 上的垂足
      Point firstPointInM =
          pointIntersectionGeometryAlongDirection(
              firstPointInL, direction, bufferMore.getExteriorRing(), 30);
      // l2m
      LineString l2m =
          JtsConstant.GEOMETRY_FACTORY_FLOATING.createLineString(
              new Coordinate[] {firstPointInL.getCoordinate(), firstPointInM.getCoordinate()});

      // bufferLittle 上的第二个点
      Point secondPointInL = secondLine.getStartPoint();
      // 垂直方向
      LineSegment d2 =
          XkLineSegmentUtil.rotateLineSegment(
              XkLineSegmentUtil.getLineSegmentOfLineSegmentString(secondLine), secondPointInL, -90);
      // secondPointInL 在 bufferMore 上的垂足
      Point secondPointInM =
          pointIntersectionGeometryAlongDirection(
              secondPointInL, d2, bufferMore.getExteriorRing(), 30);
      // ml2
      LineString m2l =
          JtsConstant.GEOMETRY_FACTORY_FLOATING.createLineString(
              new Coordinate[] {secondPointInM.getCoordinate(), secondPointInL.getCoordinate()});

      LineString mFragment =
          getFragmentFromLinearRing(bufferMore.getExteriorRing(), firstPointInM, secondPointInM);
      bufferLittle =
          Orientation.isCCW(bufferLittle.getCoordinates()) ? bufferLittle.reverse() : bufferLittle;
      LineString lFragment =
          getFragmentFromLinearRing(bufferLittle.getExteriorRing(), secondPointInL, firstPointInL);

      double area =
          mergeLineStringsToPolygon(Arrays.asList(l2m, mFragment, m2l, lFragment)).getArea();
      res.add(area);
    }
    return res;
  }

  /**
   * TODO 未完成<br>
   * 处理 {@link #generateLines(Polygon)} 中线段相连的情况 <br>
   * 可能情况有
   * <li>成直角，分别做buffer，然后在后续步骤中，判断使用哪个
   * <li>成钝角，直接buffer，距离太短的话（端点作垂线后在相交了），就舍弃？TODO 跟产品确认
   * <li>成锐角，跟钝角类似
   */
  public void handleConnectedLine(
      LineSegment first, LineSegment second, Polygon bufferLess, Polygon bufferMore) {
    LineString firstLineString = first.toGeometry(JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT);
    LineString secondLineString = second.toGeometry(JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT);
    assert first.getCoordinate(1).distance(second.getCoordinate(0)) < 1e-3;
    double angle =
        Angle.angleBetween(first.getCoordinate(0), first.getCoordinate(1), second.getCoordinate(1));
    if (Math.abs(angle - Math.PI / 2) < 0.05) {}
  }

  /** 从一点出发，沿着某个方向延长一定距离，与 geometry 相交的第一个点 */
  public Point pointIntersectionGeometryAlongDirection(
      Point point, LineSegment direction, Geometry geometry, double maxAlongDistance) {
    double dx = direction.getCoordinate(1).getX() - direction.getCoordinate(0).getX();
    double dy = direction.getCoordinate(1).getY() - direction.getCoordinate(0).getY();
    double multipleTimes = maxAlongDistance / (Math.sqrt(dx * dx + dy * dy));
    Coordinate end =
        new Coordinate(point.getX() + dx * multipleTimes, point.getY() + dy * multipleTimes);
    return (Point)
        JtsConstant.GEOMETRY_FACTORY_FLOATING
            .createLineString(new Coordinate[] {point.getCoordinate(), end})
            .intersection(geometry)
            .getGeometryN(0);
  }

  /**
   * 获取 ring 由 first second 两点截成的片段
   *
   * @param ring LinearRing
   * @param first 第一个点，需要在 ring 上
   * @param second 第二个点，需要在 ring 上
   * @return 首尾为 first second，方向与 ring 的方向一致
   */
  public LineString getFragmentFromLinearRing(LinearRing ring, Point first, Point second) {
    Coordinate[] coordinates = ring.getCoordinates();
    List<Coordinate> res = new ArrayList<>();
    res.add(first.getCoordinate());
    int size = coordinates.length - 1;
    boolean findFirst = true;
    for (int i = 0; i < 2 * size; ++i) {
      LineSegment ls = new LineSegment(coordinates[i % size], coordinates[(i + 1) % size]);
      if (findFirst && ls.distance(first.getCoordinate()) < 1e-3) {
        res.add(coordinates[(i + 1) % size]);
        findFirst = false;
      } else if (!findFirst && ls.distance(second.getCoordinate()) < 1e-3) {
        res.add(second.getCoordinate());
        break;
      } else if (!findFirst) {
        res.add(coordinates[(i + 1) % size]);
      }
    }
    LineString lineString =
        JtsConstant.GEOMETRY_FACTORY_FLOATING.createLineString(res.toArray(new Coordinate[0]));
    return lineString;
    // List<LineSegment> lineSegments =
    // XkLineSegmentUtil.generateLineSegmentListFromLineString(ring);
    // LineSegment fl =
    //     lineSegments.stream()
    //         .max(Comparator.comparingDouble(x -> x.distance(first.getCoordinate())))
    //         .orElse(lineSegments.get(0));
    // int firstIndex = lineSegments.indexOf(fl);
    // LineSegment sl =
    //     lineSegments.stream()
    //         .max(Comparator.comparingDouble(x -> x.distance(second.getCoordinate())))
    //         .orElse(lineSegments.get(0));
    // int secondIndex = lineSegments.indexOf(sl);
    // Geometry difference = ring.difference(first);
    // System.out.println(difference);

    // return ring;
  }

  /**
   * 将 list 里的 LineString 围成一个 Polygon
   *
   * @param list 需要以此排列，且成环
   * @return 围成的 Polygon
   */
  public Polygon mergeLineStringsToPolygon(List<LineString> list) {
    List<Coordinate> all = new ArrayList<>();
    for (LineString ls : list) {
      Coordinate[] subCoordinates = ls.getCoordinates();
      List<Coordinate> sc = Arrays.stream(subCoordinates).collect(Collectors.toList());
      if (!all.isEmpty() && sc.get(0).distance(all.get(all.size() - 1)) < 1e-3) {
        sc.remove(0);
      }
      all.addAll(sc);
    }
    Polygon polygon =
        JtsConstant.GEOMETRY_FACTORY_FLOATING.createPolygon(all.toArray(new Coordinate[0]));
    return polygon;
  }

  /**
   * 找出所有的建筑消防轮廓线<br>
   *
   * <ol>
   *   基本思想是
   *   <li>将polygon向外buffer 5，得到一个轮廓线 bufferBoundary
   *   <li>然后根据polygon轮廓上 lineString 找出其在 bufferBoundary 上对应的线段
   * </ol>
   *
   * 关于 buffer 的信息具体可参考 {@link fireclimb.JTSBufferTest#main} <br>
   * 存在问题:<br>
   * <li>斜面、曲面 的生成结果有点奇怪
   * <li>需要再多考虑一些细节
   *
   * @param polygon 建筑外轮廓
   * @return
   */
  public List<LineString> generateLines(Polygon polygon) {
    Geometry boundary = buffer5.getBoundary();
    Geometry bufferBoundary =
        Orientation.isCCW(boundary.getCoordinates()) ? boundary : boundary.reverse();
    List<LineString> res = new ArrayList<>();
    XkLineSegmentUtil.generateLineSegmentListFromGeometry(p).stream()
        .map(
            x ->
                XkGeometryUtil.generateSingleSideBufferedGeometry(
                        x.toGeometry(JtsConstant.GEOMETRY_FACTORY_FLOATING), -5.00)
                    .intersection(bufferBoundary)
                    // p 是逆时针，buffer之后 intersection 的线段又变成了顺时针
                    .reverse())
        .forEach(
            x -> {
              if (x instanceof LineString) {
                IntStream.range(0, x.getCoordinates().length - 1)
                    .forEach(
                        i ->
                            res.add(
                                JtsConstant.GEOMETRY_FACTORY_FLOATING.createLineString(
                                    new Coordinate[] {
                                      x.getCoordinates()[i], x.getCoordinates()[i + 1]
                                    })));
              }
            });
    return res;
  }

  /**
   * 已知一些成环状的节点，节点有各自的 value，节点间有 distance<br>
   * 找出一个连续子序列，使得 valueSum >= minvalue, 且 distanceSum 最小<br>
   *
   * @param values 节点的 value > 0
   * @param distance 节点间的 distance, distance[i] 表示节点i -> i+1 的 distance > 0
   * @param minValue valueSum 需满足的最小值
   * @return 满足条件的子序列的首尾的序数
   */
  public Map<int[], Double> circleSlideWindow(
      List<Double> values, List<Double> distance, double minValue) {
    if (values.stream().mapToDouble(Double::doubleValue).sum() < minValue) {
      return Collections.emptyMap();
    }
    List<Double> valuesTemp = new ArrayList<>(values);
    valuesTemp.addAll(values);
    List<Double> distanceTemp = new ArrayList<>(distance);
    distanceTemp.addAll(distance);
    double currentValues = values.get(0);
    int size = values.size();
    Map<int[], Double> map = new HashMap<>(size * 2);
    double currentDistance = 0;
    int left = 0;
    int right = 0;
    // TODO  考虑 left >= right 的情况
    while (currentValues < minValue && left < size) {
      currentDistance += distanceTemp.get(right);
      currentValues += valuesTemp.get(++right);
      while (currentValues >= minValue) {
        map.put(new int[] {left, right % size}, currentDistance);
        currentDistance -= distanceTemp.get(left);
        currentValues -= valuesTemp.get(left++);
      }
    }

    // 校验有效性
    System.out.println(validate(values, distance, minValue, map));

    return map;
  }

  /**
   * 校验 {@link fireclimb.FireClimb#circleSlideWindow(List, List, double)} 的结果是否正确
   *
   * @return true 没毛病，否则 false
   */
  private boolean validate(
      List<Double> values, List<Double> distance, double minValue, Map<int[], Double> map) {
    int size = values.size();
    List<Double> valuesTemp = new ArrayList<>(values);
    valuesTemp.addAll(values);
    List<Double> distanceTemp = new ArrayList<>(distance);
    distanceTemp.addAll(distance);
    return map.entrySet().stream()
        .allMatch(
            entry -> {
              int l = entry.getKey()[0];
              int r = entry.getKey()[1];
              r = l > r ? r + size : r;
              double sum =
                  valuesTemp.subList(l, r + 1).stream().mapToDouble(Double::doubleValue).sum();
              boolean s1 = sum >= minValue;
              boolean s2 = sum - valuesTemp.get(r) < minValue;
              boolean s3 =
                  Math.abs(
                          distanceTemp.subList(l, r).stream().mapToDouble(Double::doubleValue).sum()
                              - entry.getValue())
                      < 1e-3;
              return s1 && s2 && s3;
            });
  }

  void testSlideWindows() {
    FireClimb fireClimb = new FireClimb();
    List<Double> values = Arrays.asList(6.0, 2.0, 7.0, 5.0, 12.0, 13.0, 1.0, 9.0);
    List<Double> distance = Arrays.asList(5.6, 2.7, 9.34, 6.9, 10.2, 9.9, 15.1, 2.1);
    IntStream.range(0, 100).forEach(i -> fireClimb.circleSlideWindow(values, distance, i));
  }

  void testGetFragmentFromLinearRing() {
    IntStream.range(0, 100)
        .forEach(
            djslafj -> {
              Polygon geometry =
                  (Polygon)
                      XkCoordinateUtil.generateConvexHullOfCoordinateList(
                          IntStream.range(0, 10)
                              .mapToObj(
                                  djslf ->
                                      new Coordinate(
                                          new Random().nextInt(100), new Random().nextInt(100)))
                              .collect(Collectors.toList()));
              Coordinate[] coordinates = geometry.getCoordinates();
              int index = new Random().nextInt(coordinates.length - 1);
              LineSegment lineSegment = new LineSegment(coordinates[index], coordinates[index + 1]);
              Coordinate c = lineSegment.pointAlong(new Random().nextDouble());
              int i2 = new Random().nextInt(coordinates.length - 1);
              LineSegment l2 = new LineSegment(coordinates[i2], coordinates[i2 + 1]);
              Coordinate c2 = l2.pointAlong(new Random().nextDouble());
              LineString fragmentFromLinearRing =
                  getFragmentFromLinearRing(
                      geometry.getExteriorRing(),
                      XkGeometryUtil.geometryFactory.createPoint(c),
                      XkGeometryUtil.geometryFactory.createPoint(c2));
              boolean s1 = fragmentFromLinearRing.getCoordinateN(0).distance(c) < 1e-3;
              boolean s2 =
                  fragmentFromLinearRing
                          .getCoordinateN(fragmentFromLinearRing.getCoordinates().length - 1)
                          .distance(c2)
                      < 1e-3;
              boolean b = fragmentFromLinearRing.distance(geometry.getExteriorRing()) < 1e-3;
              assert s1 && s2 && b;
              System.out.println(fragmentFromLinearRing.getCoordinates().length);
            });
  }

  // todo 删除短边的作用是啥

  // Polygon deleteShortEdge(Polygon polygon) {
  //   List<LineString> lineStrings =
  // XkLineSegmentUtil.generateLineSegmentListFromLineString((LineString)
  // polygon.getBoundary()).stream().map(x ->
  // x.toGeometry(JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT)).collect(Collectors.toList());
  //   for (int i = 1; i < lineStrings.size(); i++) {
  //     lineStrings.get(i)
  //   }
  //   polygon.getBoundary()
  // }

  // List<LineString> deleteLineString(List<LineString> lineStrings) {
  //   if(lineStrings.size()<=4) {
  //     return lineStrings;
  //   }
  //   Optional<LineString> filter = lineStrings.stream().filter(x -> x.getLength() <
  // 2).findFirst();
  //   if(!filter.isPresent()) {
  //     return lineStrings;
  //   }
  //   if(lineStrings.indexOf(filter.get()) == 0) {
  //
  //   }
  //
  // }

  static List<Polygon> read() {
    Polygon polygon =
        (Polygon)
            XkGeometryIOUtil.fromGeoJson(
                "{\"coordinates\": [[[7.3, -6.35], [7.3, -10.1], [7.3, -10.3], [7.1, -10.3], [3.65, -10.3], [3.65, -11.45], [3.65, -11.55], [3.55, -11.55], [-0.1, -11.55], [-0.1, -11.5], [-3.55, -11.5], [-3.65, -11.5], [-3.65, -11.4], [-3.65, -10.3], [-7.1, -10.3], [-7.3, -10.3], [-7.3, -10.1], [-7.3, -6.35], [-7.3, -4.7], [-7.3, -4.5], [-7.1, -4.5], [-6.8, -4.5], [-6.8, -1.6], [-6.8, 0.5], [-9.3, 0.5], [-12.25, 0.5], [-12.25, -1.05], [-12.25, -1.15], [-12.35, -1.15], [-15.8, -1.15], [-15.9, -1.15], [-15.9, -1.05], [-15.9, 0.6], [-15.9, 5.5], [-15.9, 8.5], [-15.9, 8.7], [-15.7, 8.7], [-13.8, 8.7], [-10.6, 8.7], [-8.8, 8.7], [-8.8, 9.7], [-8.8, 11.55], [-8.7, 11.55], [-4.4, 11.55], [-4.3, 11.55], [-4.3, 11.45], [-4.3, 9.7], [-4.3, 5.9], [-4.2, 5.9], [-1.8, 5.9], [-1.4, 5.9], [-1.4, 11.3], [-1.4, 11.5], [-1.2, 11.5], [1.2, 11.5], [1.4, 11.5], [1.4, 11.3], [1.4, 5.9], [1.6, 5.9], [1.8, 5.9], [4.2, 5.9], [4.3, 5.9], [4.3, 9.7], [4.3, 11.55], [4.4, 11.55], [8.7, 11.55], [8.8, 11.55], [8.8, 11.45], [8.8, 9.7], [8.8, 8.7], [10.6, 8.7], [13.8, 8.7], [15.7, 8.7], [15.9, 8.7], [15.9, 8.5], [15.9, 5.5], [15.9, 0.6], [15.9, -1.15], [15.8, -1.15], [12.35, -1.15], [12.25, -1.15], [12.25, -1.05], [12.25, 0.5], [9.3, 0.5], [6.8, 0.5], [6.8, -1.6], [6.8, -4.5], [7.1, -4.5], [7.3, -4.5], [7.3, -4.7], [7.3, -6.35]]], \"type\": \"Polygon\"}");
    Polygon polygon1 =
        (Polygon)
            XkGeometryIOUtil.fromGeoJson(
                "{\"coordinates\": [[[-0.1, -12.5], [-3.85, -12.5], [-3.95, -12.5], [-3.95, -12.4], [-3.95, -11.95], [-7.2, -11.95], [-7.4, -11.95], [-7.4, -11.75], [-7.4, -8.35], [-7.4, -6.45], [-7.4, -3.65], [-7.4, -3.45], [-7.2, -3.45], [-6.45, -3.45], [-6.45, -1.95], [-6.45, -1.85], [-6.35, -1.85], [-4.5, -1.85], [-2.6, -1.85], [-2.6, -0.1], [-2.6, 0.1], [-2.6, 0.75], [-3.7, 0.75], [-3.9, 0.75], [-3.9, 0.8], [-5.7, 0.8], [-5.8, 0.8], [-5.8, 0.9], [-5.8, 2.25], [-8.8, 2.25], [-10.45, 2.25], [-13.5, 2.25], [-16.6, 2.25], [-16.8, 2.25], [-16.8, 2.45], [-16.8, 6.25], [-16.8, 10.85], [-16.8, 11.05], [-16.6, 11.05], [-12.85, 11.05], [-11.05, 11.05], [-11.05, 12.4], [-11.05, 12.5], [-10.95, 12.5], [-3.4, 12.5], [-3.3, 12.5], [-3.3, 12.4], [-3.3, 10.95], [-3.3, 9.55], [-1.3, 9.55], [-1.1, 9.55], [-1.1, 9.35], [-1.1, 7.85], [1.1, 7.85], [1.1, 9.35], [1.1, 9.55], [1.3, 9.55], [3.3, 9.55], [3.3, 10.95], [3.3, 12.5], [3.4, 12.5], [10.95, 12.5], [11.05, 12.5], [11.05, 12.4], [11.05, 11.05], [12.85, 11.05], [16.6, 11.05], [16.8, 11.05], [16.8, 10.85], [16.8, 6.25], [16.8, 2.45], [16.8, 2.25], [16.6, 2.25], [13.5, 2.25], [10.45, 2.25], [8.8, 2.25], [5.8, 2.25], [5.8, 0.9], [5.8, 0.8], [5.7, 0.8], [3.9, 0.8], [3.9, 0.75], [3.7, 0.75], [2.6, 0.75], [2.6, 0.1], [2.6, -0.1], [2.6, -1.85], [4.5, -1.85], [6.45, -1.85], [6.45, -1.95], [6.45, -3.45], [7.2, -3.45], [7.4, -3.45], [7.4, -3.65], [7.4, -6.45], [7.4, -8.35], [7.4, -11.75], [7.4, -11.95], [7.2, -11.95], [3.95, -11.95], [3.95, -12.4], [3.95, -12.5], [3.85, -12.5], [0.1, -12.5], [0, -12.5], [-0.1, -12.5]]], \"type\": \"Polygon\"}");
    Polygon polygon2 =
        (Polygon)
            XkGeometryIOUtil.fromGeoJson(
                "{\"coordinates\": [[[-11.3120145, -17.548906], [-11.3135815, -17.557264], [-11.3581575, -17.548906], [-11.4035105, -17.548906], [-11.4035105, -17.540402], [-12.1190425, -17.40624], [-12.1207005, -17.411544], [-12.1656205, -17.397507], [-12.2118685, -17.388835], [-12.2108445, -17.383374], [-12.9266915, -17.159671], [-12.9293455, -17.165359], [-12.9716185, -17.145632], [-13.0161485, -17.131716], [-13.0142765, -17.125725], [-13.6816995, -16.814261], [-13.6829225, -16.816553], [-13.7258655, -16.79365], [-13.7699635, -16.773071], [-13.7688645, -16.770717], [-14.4396885, -16.412944], [-14.4443645, -16.419023], [-14.4807055, -16.391068], [-14.5211575, -16.369494], [-14.5175485, -16.362728], [-15.0976445, -15.916501], [-15.1004395, -15.91955], [-15.1357055, -15.887223], [-15.1736275, -15.858052], [-15.1711055, -15.854773], [-15.7101395, -15.360658], [-15.7215055, -15.367288], [-15.7428805, -15.330644], [-15.7741545, -15.301977], [-15.7652625, -15.292277], [-16.0966995, -14.7241], [-16.4252655, -14.160844], [-16.4322285, -14.163522], [-16.4487295, -14.120618], [-16.4718925, -14.080911], [-16.4654475, -14.077152], [-16.6834205, -13.510422], [-16.6865955, -13.511399], [-16.7007955, -13.465247], [-16.7181265, -13.420188], [-16.7150275, -13.418996], [-16.8885975, -12.854892], [-16.8953265, -12.855927], [-16.9023745, -12.810116], [-16.9160035, -12.765821], [-16.9094965, -12.763819], [-16.9954075, -12.205399], [-16.9959365, -12.205475], [-17.0029715, -12.156229], [-17.0105315, -12.10709], [-17.0100035, -12.107009], [-17.0968755, -11.498906], [-17.1035105, -11.498906], [-17.1035105, -11.452463], [-17.1100795, -11.40648], [-17.1035105, -11.405542], [-17.1035105, -10.795462], [-17.1069455, -10.795217], [-17.1035105, -10.747126], [-17.1035105, -10.698906], [-17.1000665, -10.698906], [-17.0566675, -10.091316], [-17.0605315, -10.090722], [-17.0532395, -10.043324], [-17.0498215, -9.995471], [-17.0459205, -9.99575], [-16.9594965, -9.433993], [-16.9660035, -9.431991], [-16.9523745, -9.387696], [-16.9453265, -9.341885], [-16.9385975, -9.34292], [-16.7512995, -8.734202], [-16.5641555, -8.125984], [-16.5698615, -8.123351], [-16.5503135, -8.080998], [-16.5365955, -8.036413], [-16.5305895, -8.038261], [-16.2679465, -7.469203], [-16.2718925, -7.466901], [-16.2479065, -7.425782], [-16.2279555, -7.382555], [-16.2238075, -7.38447], [-15.9194745, -6.862757], [-15.9233555, -6.859934], [-15.8954275, -6.821532], [-15.8715055, -6.780524], [-15.8673615, -6.782942], [-15.4939475, -6.269497], [-15.1035105, -5.732647], [-15.1035105, -5.698906], [-15.0789715, -5.698906], [-15.0645385, -5.679061], [-15.0372515, -5.698906], [-15.0035105, -5.698906], [-15.0035105, -1.548906], [-15.0035105, -1.348906], [-14.8035105, -1.348906], [-11.3535105, -1.348906], [-9.0535105, -1.348906], [-5.5035105, -1.348906], [-5.5035105, -1.298906], [-5.5035105, 1.701094], [-7.7535105, 1.701094], [-11.3535105, 1.701094], [-17.5035105, 1.701094], [-17.7035105, 1.701094], [-17.7035105, 1.901094], [-17.7035105, 5.351094], [-19.8035105, 5.351094], [-20.0035105, 5.351094], [-20.0035105, 5.551094], [-20.0035105, 14.301094], [-20.0035105, 14.501094], [-19.8035105, 14.501094], [-14.3535105, 14.501094], [-11.3535105, 14.501094], [-7.8035105, 14.501094], [-7.8035105, 16.701094], [-7.8035105, 16.801094], [-7.7035105, 16.801094], [-0.5535105, 16.801094], [2.9964895, 16.801094], [3.1964895, 16.801094], [3.1964895, 16.601094], [3.1964895, 12.001094], [3.1964895, 9.251094], [5.6464895, 9.251094], [5.6464895, 12.601094], [5.6464895, 14.451094], [5.7464895, 14.451094], [9.4464895, 14.451094], [9.4464895, 14.524334], [9.4271515, 14.537226], [9.4464895, 14.566233], [9.4464895, 14.601094], [9.4697305, 14.601094], [9.8321495, 15.144723], [9.8257785, 15.151094], [9.8576315, 15.182947], [9.8826215, 15.220432], [9.8901185, 15.215434], [10.3611345, 15.686449], [10.8321495, 16.157465], [10.8271515, 16.164962], [10.8646365, 16.189952], [10.8964895, 16.221805], [10.9028605, 16.215434], [11.4295735, 16.566576], [11.4281075, 16.569089], [11.4699915, 16.593521], [11.5103575, 16.620432], [11.5119725, 16.61801], [12.0320535, 16.921391], [12.0301385, 16.925539], [12.0733655, 16.94549], [12.1144845, 16.969476], [12.1167865, 16.96553], [12.6892175, 17.22973], [12.6871225, 17.239508], [12.7305815, 17.248821], [12.7709345, 17.267445], [12.7751245, 17.258366], [13.3903935, 17.390209], [13.3899205, 17.39352], [13.4376965, 17.400345], [13.4849035, 17.410461], [13.4856045, 17.407189], [14.0932975, 17.494003], [14.0930545, 17.497405], [14.1411645, 17.500841], [14.1889155, 17.507663], [14.1893975, 17.504287], [14.8023095, 17.548065], [14.8035365, 17.557264], [14.8480315, 17.551331], [14.8928005, 17.554529], [14.8934615, 17.545274], [15.5572835, 17.456764], [15.5580755, 17.460461], [15.6050505, 17.450395], [15.6526595, 17.444047], [15.6521595, 17.4403], [16.2651685, 17.308941], [16.2677715, 17.31571], [16.3107855, 17.299166], [16.3558565, 17.289508], [16.3543365, 17.282416], [16.9207205, 17.064576], [16.9220445, 17.067445], [16.9659685, 17.047173], [17.0111055, 17.029812], [17.0099715, 17.026863], [17.5786095, 16.764415], [17.5826215, 16.770432], [17.6209825, 16.744858], [17.6628405, 16.725539], [17.6598105, 16.718973], [18.1865015, 16.367846], [18.1894535, 16.371454], [18.2262555, 16.341343], [18.2658275, 16.314962], [18.2632415, 16.311082], [18.7488405, 15.913773], [18.7587775, 15.920729], [18.7834925, 15.885422], [18.8168495, 15.85813], [18.8091685, 15.848742], [19.1100125, 15.418965], [19.1118285, 15.420121], [19.1380745, 15.378877], [19.1661245, 15.338806], [19.1643605, 15.337571], [19.4671235, 14.861801], [19.4763925, 14.865171], [19.4916105, 14.823322], [19.5155165, 14.785755], [19.5071965, 14.78046], [19.6768475, 14.313918], [19.6781125, 14.31434], [19.6937145, 14.267535], [19.7105665, 14.221191], [19.7093135, 14.220736], [19.8805835, 13.706927], [19.8883055, 13.708115], [19.8952635, 13.662886], [19.9097355, 13.619471], [19.9023235, 13.617], [19.9893855, 13.051094], [19.9964895, 13.051094], [19.9964895, 13.004915], [20.0035105, 12.959278], [19.9964895, 12.958198], [19.9964895, 12.447396], [20.0001775, 12.447112], [19.9964895, 12.399168], [19.9964895, 12.351094], [19.9927915, 12.351094], [19.9495805, 11.78936], [19.9571235, 11.787474], [19.9460195, 11.743057], [19.9425075, 11.697406], [19.9347555, 11.698002], [19.8059255, 11.182685], [19.8105665, 11.180997], [19.7943655, 11.136445], [19.7828695, 11.09046], [19.7780795, 11.091658], [19.6097975, 10.628882], [19.6118745, 10.628017], [19.5930875, 10.582928], [19.5763925, 10.537017], [19.5742785, 10.537786], [19.3581135, 10.01899], [19.3661245, 10.013382], [19.3405585, 9.976859], [19.3234125, 9.935709], [19.3143865, 9.93947], [19.0115075, 9.506787], [19.0172005, 9.501094], [18.9849395, 9.468833], [18.9587775, 9.431459], [18.9521825, 9.436076], [18.5318445, 9.015739], [18.1115075, 8.595401], [18.1161245, 8.588806], [18.0787505, 8.562644], [18.0464895, 8.530383], [18.0407965, 8.536076], [17.6081135, 8.233197], [17.6118745, 8.224171], [17.5707245, 8.207025], [17.5342015, 8.181459], [17.5285935, 8.18947], [16.9964895, 7.96776], [16.9964895, 7.951094], [16.9564905, 7.951094], [16.9195665, 7.935709], [16.9131565, 7.951094], [16.8464895, 7.951094], [16.8464895, 4.451094], [16.8964895, 4.451094], [16.8964895, 4.251094], [16.8964895, 1.601094], [16.8964895, -2.048906], [16.8964895, -5.748906], [16.8964895, -8.848906], [16.8964895, -14.348906], [16.8964895, -14.548906], [16.6964895, -14.548906], [11.8464895, -14.548906], [8.3464895, -14.548906], [8.1464895, -14.548906], [3.0964895, -14.548906], [-0.5535105, -14.548906], [-4.4535105, -14.548906], [-4.7535105, -14.548906], [-4.7535105, -14.574286], [-4.7333935, -14.58976], [-4.7535105, -14.615912], [-4.7535105, -14.648906], [-4.7788905, -14.648906], [-5.2371115, -15.244594], [-5.2327995, -15.248906], [-5.2658575, -15.281964], [-5.2943645, -15.319023], [-5.2991985, -15.315305], [-5.8382045, -15.854311], [-5.8337615, -15.860531], [-5.8710815, -15.887188], [-5.9035105, -15.919617], [-5.9089155, -15.914212], [-6.5376805, -16.36333], [-6.5352915, -16.367511], [-6.5765035, -16.391061], [-6.6151355, -16.418655], [-6.6179345, -16.414736], [-7.2387345, -16.769478], [-7.2370575, -16.773071], [-7.2804945, -16.793342], [-7.3221155, -16.817125], [-7.3240825, -16.813683], [-7.9927445, -17.125725], [-7.9908725, -17.131716], [-8.0354025, -17.145632], [-8.0776755, -17.165359], [-8.0803295, -17.159671], [-8.7990065, -17.384258], [-8.7980105, -17.392721], [-8.8430395, -17.398019], [-8.8863205, -17.411544], [-8.8888625, -17.40341], [-9.6506545, -17.493033], [-9.6504885, -17.49569], [-9.6990245, -17.498724], [-9.7473255, -17.504406], [-9.7476365, -17.501762], [-10.4535105, -17.545879], [-10.4535105, -17.548906], [-10.5019425, -17.548906], [-10.5502945, -17.551928], [-10.5504835, -17.548906], [-11.3120145, -17.548906]]], \"type\": \"Polygon\"}");
    Polygon polygon3 =
        (Polygon)
            XkGeometryIOUtil.fromGeoJson(
                "{\"coordinates\": [[[-0.1, 4.764991], [2.6, 4.764991], [2.8, 4.764991], [3.4, 4.764991], [3.4, 8.064991], [3.4, 10.014991], [3.5, 10.014991], [3.5, 9.964991], [3.512883, 10.013303], [6.506554, 9.214991], [6.55, 9.214991], [6.55, 9.203405], [6.561195, 9.20042], [6.55, 9.158438], [6.55, 8.164991], [8.3, 8.164991], [11.2, 8.164991], [11.4, 8.164991], [11.4, 7.964991], [11.4, 5.764991], [12.3, 5.764991], [12.5, 5.764991], [12.5, 5.564991], [12.5, 3.164991], [12.5, -0.235009], [12.5, -0.435009], [12.3, -0.435009], [7.95, -0.435009], [7.95, -1.735009], [9.3, -1.735009], [9.5, -1.735009], [9.5, -1.935009], [9.5, -4.135009], [9.5, -6.035009], [9.5, -8.735009], [9.5, -8.935009], [9.3, -8.935009], [6.15, -8.935009], [6.15, -9.582884], [6.15406, -9.630589], [6.15, -9.630935], [6.15, -9.635009], [6.102125, -9.635009], [1.45, -10.030935], [1.45, -10.035009], [1.402125, -10.035009], [1.35442, -10.039069], [1.354074, -10.035009], [1.3, -10.035009], [1.2, -10.035009], [1.2, -9.935009], [1.2, -8.235009], [1.2, -5.535009], [-1.2, -5.535009], [-1.2, -8.235009], [-1.2, -10.035009], [-1.3, -10.035009], [-1.354074, -10.035009], [-1.35442, -10.039069], [-1.402125, -10.035009], [-1.45, -10.035009], [-1.45, -10.030935], [-6.102125, -9.635009], [-6.15, -9.635009], [-6.15, -9.630935], [-6.15406, -9.630589], [-6.15, -9.582884], [-6.15, -8.935009], [-9.3, -8.935009], [-9.5, -8.935009], [-9.5, -8.735009], [-9.5, -6.035009], [-9.5, -4.135009], [-9.5, -1.935009], [-9.5, -1.735009], [-9.3, -1.735009], [-7.95, -1.735009], [-7.95, -0.435009], [-12.3, -0.435009], [-12.5, -0.435009], [-12.5, -0.235009], [-12.5, 3.164991], [-12.5, 5.564991], [-12.5, 5.764991], [-12.3, 5.764991], [-11.4, 5.764991], [-11.4, 7.964991], [-11.4, 8.164991], [-11.2, 8.164991], [-8.3, 8.164991], [-6.55, 8.164991], [-6.55, 9.158438], [-6.561195, 9.20042], [-6.55, 9.203405], [-6.55, 9.214991], [-6.506554, 9.214991], [-3.41626, 10.039069], [-3.409839, 10.014991], [-3.4, 10.014991], [-3.4, 9.978096], [-3.390493, 9.942446], [-3.4, 9.939911], [-3.4, 8.064991], [-3.4, 4.764991], [-2.8, 4.764991], [-2.6, 4.764991], [-0.1, 4.764991]]], \"type\": \"Polygon\"}");
    Polygon polygon4 =
        (Polygon)
            XkGeometryIOUtil.fromGeoJson(
                "{\"coordinates\": [[[2.2, -10.5], [0.7, -10.5], [-2.9, -10.5], [-4.4, -10.5], [-4.6, -10.5], [-4.6, -10.3], [-4.6, -9.3], [-7.2, -9.3], [-11.6, -9.3], [-11.8, -9.3], [-11.8, -9.1], [-11.8, -5.4], [-11.8, -1.15], [-11.7, -1.15], [-10.95, -1.15], [-10.95, -0.3], [-10.95, -0.2], [-10.85, -0.2], [-8.45, -0.2], [-8.35, -0.2], [-8.35, -0.3], [-8.35, -1.1], [-5.9, -1.1], [-5.7, -1.1], [-5.7, -1.3], [-5.7, -1.7], [-3, -1.7], [-3, -0.2], [-3, -6.661338147750939e-16], [-3, 2.6], [-3, 4.35], [-3.75, 4.35], [-3.85, 4.35], [-3.85, 4.45], [-3.85, 8.35], [-3.85, 8.45], [-3.75, 8.45], [-3, 8.45], [-3, 9.4], [-3, 9.5], [-2.9, 9.5], [-0.05, 9.5], [0.05, 9.5], [0.05, 9.4], [0.05, 8.5], [2.7, 8.5], [2.9, 8.5], [2.9, 8.3], [2.9, 7.9], [5.2, 7.9], [5.2, 10.3], [5.2, 10.5], [5.4, 10.5], [9.7, 10.5], [11.6, 10.5], [11.8, 10.5], [11.8, 10.3], [11.8, 6.1], [11.8, 2.65], [11.8, 1.75], [11.7, 1.75], [10, 1.75], [10, -1.1], [10, -1.3], [9.8, -1.3], [7.1, -1.3], [5, -1.3], [5, -3.4], [5, -5.6], [5, -10.3], [5, -10.5], [4.8, -10.5], [2.4, -10.5], [2.3, -10.5], [2.2, -10.5]]], \"type\": \"Polygon\"}");
    Polygon polygon5 =
        (Polygon)
            XkGeometryIOUtil.fromGeoJson(
                "{\"coordinates\": [[[6.25, -7.875], [3.6, -7.875], [3.6, -8.375], [3.6, -9.425], [3.5, -9.425], [0.1, -9.425], [-1.0685896612017132e-15, -9.425], [-0.1, -9.425], [-3.5, -9.425], [-3.6, -9.425], [-3.6, -9.325], [-3.6, -8.375], [-3.6, -7.875], [-6.25, -7.875], [-6.25, -9.325], [-6.25, -9.425], [-6.35, -9.425], [-9.9, -9.425], [-10, -9.425], [-10, -9.325], [-10, -7.775], [-10, -5.625], [-13.6, -5.625], [-13.7, -5.625], [-13.7, -5.525], [-13.7, -3.575], [-14.35, -3.575], [-17.3, -3.575], [-17.3, -4.875], [-17.3, -5.075], [-17.5, -5.075], [-21.4, -5.075], [-21.6, -5.075], [-21.6, -4.875], [-21.6, -0.825], [-21.6, 1.075], [-21.6, 2.825], [-21.6, 3.025], [-21.4, 3.025], [-20.5, 3.025], [-20.5, 6.825], [-20.5, 7.025], [-20.3, 7.025], [-17.5, 7.025], [-17.3, 7.025], [-17.3, 6.825], [-17.3, 5.525], [-14.4, 5.525], [-10.3, 5.525], [-8, 5.525], [-8, 9.225], [-8, 9.425], [-7.8, 9.425], [-5.2, 9.425], [-2.7, 9.425], [-2.5, 9.425], [-2.5, 9.225], [-2.5, 7.025], [-2.3, 7.025], [-2.1, 7.025], [-2.1, 6.825], [-2.1, 4.225], [2.1, 4.225], [2.1, 6.825], [2.1, 7.025], [2.3, 7.025], [2.5, 7.025], [2.5, 9.225], [2.5, 9.425], [2.7, 9.425], [5.2, 9.425], [7.8, 9.425], [8, 9.425], [8, 9.225], [8, 5.525], [10.3, 5.525], [14.4, 5.525], [17.3, 5.525], [17.3, 6.825], [17.3, 7.025], [17.5, 7.025], [20.3, 7.025], [20.5, 7.025], [20.5, 6.825], [20.5, 3.025], [21.4, 3.025], [21.6, 3.025], [21.6, 2.825], [21.6, 1.075], [21.6, -0.825], [21.6, -4.875], [21.6, -5.075], [21.4, -5.075], [17.5, -5.075], [17.3, -5.075], [17.3, -4.875], [17.3, -3.575], [14.35, -3.575], [13.7, -3.575], [13.7, -5.525], [13.7, -5.625], [13.6, -5.625], [10, -5.625], [10, -7.775], [10, -9.425], [9.9, -9.425], [6.35, -9.425], [6.25, -9.425], [6.25, -9.325], [6.25, -7.875]], [[2.6, -0.975], [2.6, -0.275], [2.6, -0.075], [2.8, -0.075], [4.45, -0.075], [6.2, -0.075], [6.2, 2.525], [5.6, 2.525], [5.6, 1.875], [5.6, 1.675], [5.4, 1.675], [2.45, 1.675], [2.25, 1.675], [2.25, 1.875], [2.25, 2.525], [-2.25, 2.525], [-2.25, 1.875], [-2.25, 1.675], [-2.45, 1.675], [-5.4, 1.675], [-5.6, 1.675], [-5.6, 1.875], [-5.6, 2.525], [-6.2, 2.525], [-6.2, -0.075], [-4.45, -0.075], [-2.8, -0.075], [-2.6, -0.075], [-2.6, -0.275], [-2.6, -0.975], [-0.1, -0.975], [0.1, -0.975], [2.6, -0.975]]], \"type\": \"Polygon\"}");

    Polygon polygon6 =
        (Polygon)
            XkGeometryIOUtil.fromWkt(
                "POLYGON((-3.8 -6.825,-3.9 -6.825,-3.9 -6.725,-3.9 -6.075,-4.2 -6.075,-7.6 -6.075,-7.8 -6.075,-7.8 -5.875,-7.8 -2.525,-7.8 -0.825,-7.8 0.225,-7.8 1.475,-7.8 3.125,-7.7 3.125,-6.7 3.125,-6.7 6.625,-6.7 6.825,-6.5 6.825,-3.7 6.825,-1.4 6.825,-1.2 6.825,1.2 6.825,1.4 6.825,3.7 6.825,6.5 6.825,6.7 6.825,6.7 6.625,6.7 3.125,7.7 3.125,7.8 3.125,7.8 3.025,7.8 1.475,7.8 0.225,7.8 -0.825,7.8 -2.525,7.8 -5.875,7.8 -6.075,7.6 -6.075,4.2 -6.075,3.9 -6.075,3.9 -6.725,3.9 -6.825,3.8 -6.825,1.3 -6.825,1.2 -6.825,1.2 -6.725,1.2 -4.925,0.1 -4.925,-0.1 -4.925,-1.2 -4.925,-1.2 -6.725,-1.2 -6.825,-1.3 -6.825,-3.8 -6.825))");

    return Arrays.asList(polygon, polygon1, polygon2, polygon3, polygon4, polygon5, polygon6);
  }
}
