package fireclimb;

import cn.hutool.core.collection.CollUtil;
import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import com.xkool.algo.util.geometry.XkGeometryUtil;
import com.xkool.algo.util.geometry.XkLineSegmentUtil;
import com.xkool.algo.util.plotter.XkPlotter;
import ga.constant.JtsConstant;
import lombok.ToString;
import org.locationtech.jts.algorithm.Angle;
import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.awt.*;
import java.util.List;
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
 * <p>FIXME 1. 原本轮廓内凹过多如何处理 2. 部分结果有不合理的凹陷（buffer的原因）
 *
 * @author luoxin
 * @since 2022/6/1
 */
@ToString
public class FireClimb {

  private static final double PRECISION = 1e-2;

  private static final double POLYGON_SIMPLIFY_LENGTH_TOLERANCE = 5.0;
  private static final double TOTAL_LENGTH_TOLERANCE = 1.0;
  private static final double POLYGON_SIMPLIFY_ANGLE_TOLERANCE = Angle.toRadians(10);
  private Polygon originPolygon;

  private Polygon simplifiedPolygon;
  private Polygon buffer5;
  private Polygon buffer15;

  private List<FireClimbPlatformInfo> result;

  public FireClimb(Polygon originPolygon) {
    this.originPolygon = originPolygon;
    initialize(originPolygon);
  }

  public static void main(String[] args) {
    List<Polygon> read = FireClimb.read();
    read.forEach(
        x -> {
          FireClimb fireClimb = new FireClimb(x);
          FireClimbPlatformInfo handle = fireClimb.handle();
          fireClimb.showAllOutcome(Collections.singletonList(handle));
        });
  }

  public void showAllOutcome(List<FireClimbPlatformInfo> list) {
    if (CollUtil.isEmpty(list) || CollUtil.hasNull(list)) {
      System.out.println("Invalid: " + this.originPolygon);
      return;
    }
    list.forEach(x -> System.out.println(x.getPlatform()));
    list.forEach(
        x -> {
          XkPlotter xkPlotter = new XkPlotter();
          xkPlotter.addContent(this.simplifiedPolygon, Color.blue);
          xkPlotter.addContent(buffer5, Color.blue);
          xkPlotter.addContent(buffer15, Color.blue);
          xkPlotter.addContent(x.getPlatform().getBoundary().buffer(1), Color.red);
          xkPlotter.plot();
        });
    System.out.println("end");
  }

  public void showAllOutcome(List<FireClimbPlatformInfo> list, String title) {
    if (CollUtil.isEmpty(list) || CollUtil.hasNull(list)) {
      System.out.println("Invalid: " + this.originPolygon);
      return;
    }
    list.forEach(x -> System.out.println(x.getPlatform()));
    list.forEach(
        x -> {
          XkPlotter xkPlotter = new XkPlotter(title);
          xkPlotter.addContent(this.simplifiedPolygon, Color.blue);
          xkPlotter.addContent(buffer5, Color.blue);
          xkPlotter.addContent(buffer15, Color.blue);
          xkPlotter.addContent(x.getPlatform().getBoundary(), Color.red);
          xkPlotter.plot();
        });
    System.out.println("end");
  }

  /** 全局入口 */
  public List<FireClimbPlatformInfo> getFireClimbFaces() {
    if (!CollUtil.isEmpty(result)) {
      return result;
    }
    // 建筑上所有有效的登高面
    List<LineSegment> faces = generateLines();
    // 面宽
    double width = getBuildingWidth();
    System.out.println("----------------------");
    // 获取所有的登高面
    List<FireClimbPlatformInfo> fireClimbPlatformInfos = generatePlans(faces, width);
    this.result = fireClimbPlatformInfos;
    return fireClimbPlatformInfos;
  }

  /** 获取建筑面宽 */
  private double getBuildingWidth() {
    Polygon m = (Polygon) new MinimumDiameter(simplifiedPolygon).getMinimumRectangle();
    LineSegment l1 = new LineSegment(m.getCoordinates()[0], m.getCoordinates()[1]);
    LineSegment l2 = new LineSegment(m.getCoordinates()[1], m.getCoordinates()[2]);
    return Math.max(l1.getLength(), l2.getLength());
  }

  /**
   * 初始化 simplifiedPolygon, buffer5, buffer15
   *
   * @param polygon 建筑轮廓
   */
  public void initialize(Polygon polygon) {
    originPolygon = (Polygon) JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createGeometry(polygon);
    // 简化后的 polygon
    Polygon simplify = (Polygon) TopologyPreservingSimplifier.simplify(originPolygon, 0.05);
    simplifiedPolygon =
        // removePoint(originPolygon);
        Orientation.isCCW(simplify.getCoordinates()) ? simplify.reverse() : simplify;

    buffer15 = (Polygon) simplifiedPolygon.buffer(15, -100);
    buffer15 = removeShortEdge(buffer15);
    buffer5 = (Polygon) buffer15.buffer(-10, -100);
    buffer15 = Orientation.isCCW(buffer15.getCoordinates()) ? buffer15.reverse() : buffer15;
    buffer5 = Orientation.isCCW(buffer5.getCoordinates()) ? buffer5.reverse() : buffer5;
  }

  /** 处理 polygon 中较短的边，即填补部分楼型直角处的轻微凹陷 */
  public Polygon removeShortEdge(Polygon polygon) {
    // 先删掉冗余的点
    Polygon p = removePoint(polygon);
    Coordinate[] coordinates = p.getCoordinates();
    int length = coordinates.length;
    if (length == 4) {
      return polygon;
    }
    List<Coordinate> list = new ArrayList<>(Arrays.asList(coordinates));
    if (list.get(0).equals(list.get(length - 1))) {
      list.remove(list.size() - 1);
      --length;
    }
    for (int i = 0; i < length; i++) {
      // i -> i+1
      LineSegment curLine = new LineSegment(list.get(i), list.get((i + 1) % length));
      // 长度较小直接跳过
      if (curLine.getLength() > POLYGON_SIMPLIFY_LENGTH_TOLERANCE) {
        continue;
      }
      // i-1 -> i
      LineSegment preLine = new LineSegment(list.get((i + length - 1) % length), list.get(i));
      // i+1 -> i+2
      LineSegment postLine =
          new LineSegment(list.get((i + 1) % length), list.get((i + 2) % length));
      // 如果 pre，post 平行，就移动（应该要判断 pre post 与 cur 的夹角都为 90 度，此处或许可以改进
      if (Angle.diff(Angle.diff(preLine.angle(), curLine.angle()), Math.PI / 2)
              < POLYGON_SIMPLIFY_ANGLE_TOLERANCE
          && Angle.diff(Angle.diff(postLine.angle(), curLine.angle()), Math.PI / 2)
              < POLYGON_SIMPLIFY_ANGLE_TOLERANCE) {
        // 移动前一条边， 更新 i-1，移除 i
        Coordinate preNewCoordinate =
            moveCoordinate(
                list.get((i + length - 1) % length),
                new Vector2D(list.get(i), list.get((i + 1) % length)));
        List<Coordinate> preList = new ArrayList<>(list);
        preList.set((i + length - 1) % length, preNewCoordinate);
        preList.remove(i);
        preList.add(preList.get(0));
        Polygon p1 =
            JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(
                preList.toArray(new Coordinate[0]));

        // 移动后一条边，更新 i+2，移除 i+1
        Coordinate postNewCoordinate =
            moveCoordinate(
                list.get((i + 2) % length), new Vector2D(list.get((i + 1) % length), list.get(i)));
        List<Coordinate> postList = new ArrayList<>(list);
        postList.set((i + 2) % length, postNewCoordinate);
        postList.remove((i + 1) % length);
        postList.add(postList.get(0));
        Polygon p2 =
            JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(
                postList.toArray(new Coordinate[0]));
        // 哪种移动让面积更大，就选哪种
        return p1.getArea() > p2.getArea() ? removeShortEdge(p1) : removeShortEdge(p2);
      }
    }
    return JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(coordinates);
  }

  /** 去除polygon中线段上的冗余点 */
  public Polygon removePoint(Polygon polygon) {
    Coordinate[] coordinates = polygon.getCoordinates();
    List<Coordinate> list = new ArrayList<>(Arrays.asList(coordinates));
    if (list.get(0).equals(list.get(list.size() - 1))) {
      list.remove(list.size() - 1);
    }
    // 去除同一线段上冗余的点
    for (int i = 0; i < list.size() && list.size() >= 4; i++) {
      LineSegment curLine = new LineSegment(list.get(i), list.get((i + 1) % list.size()));
      LineSegment preLine =
          new LineSegment(list.get((i + list.size() - 1) % list.size()), list.get(i));
      // 两条线段夹角在 3 度以内就删掉点
      if (Angle.diff(curLine.angle(), preLine.angle()) < Math.toRadians(3.0)) {
        list.remove(i--);
      }
    }
    list.add(list.get(0));
    return JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(list.toArray(new Coordinate[0]));
  }

  /**
   * 找出所有有效的建筑消防轮廓线<br>
   *
   * <ol>
   *   基本思想是
   *   <li>将polygon向外buffer 5，得到一个轮廓线 bufferBoundary
   *   <li>然后根据polygon轮廓上 lineString 找出其在 bufferBoundary 上对应的线段
   * </ol>
   *
   * <p>关于 buffer 的信息具体可参考 {@link fireclimb.JTSBufferTest#main} <br>
   * 存在问题:<br>
   * <li>斜面、曲面 的生成结果有点奇怪
   * <li>需要再多考虑一些细节
   *
   * @return 外轮廓上可以登高的面
   */
  List<LineSegment> generateLines() {
    Geometry boundary = buffer5.getBoundary();
    Geometry buffer5Boundary =
        Orientation.isCCW(boundary.getCoordinates()) ? boundary.reverse() : boundary;
    List<LineSegment> buffer5BoundaryLineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(buffer5Boundary);
    List<LineSegment> res = new ArrayList<>();
    // 对每一条边单独做 buffer，判断是否与 boundary 有交集
    XkLineSegmentUtil.generateLineSegmentListFromGeometry(simplifiedPolygon).stream()
        .flatMap(
            x -> {
              Geometry intersection =
                  XkGeometryUtil.generateSingleSideBufferedGeometry(
                          x.toGeometry(JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT), 10)
                      .intersection(buffer5Boundary);
              List<LineSegment> lineSegments =
                  XkLineSegmentUtil.generateLineSegmentListFromGeometry(intersection);
              return lineSegments.stream()
                  .filter(
                      ls ->
                          ls.getLength() > 0.02
                              && Angle.diff(Angle.diff(x.angle(), ls.angle()), Math.PI / 2)
                                  > POLYGON_SIMPLIFY_ANGLE_TOLERANCE);
            })
        .forEach(
            lineSegment -> {
              if (lineSegment.getLength() < 0.02) {
                return;
              }
              // 未知原因使得 intersection 的 lineSegment 与 buffer5 的方向不符，下面进行调整
              // 找出 lineSegment 在 buffer5 上所对应的边
              Optional<LineSegment> segmentOnBuffer5 =
                  buffer5BoundaryLineSegments.stream()
                      .filter(l -> l.distance(lineSegment.midPoint()) < PRECISION)
                      .findFirst();
              // 肯定会有的
              if (segmentOnBuffer5.isPresent()) {
                // 如果不平行就要 reverse
                if (Angle.diff(lineSegment.angle(), segmentOnBuffer5.get().angle()) > PRECISION) {
                  lineSegment.reverse();
                }
              } else {
                System.out.println("bug");
              }
              res.add(lineSegment);
            });
    // 修建部分 lineSegment（主要是成锐角的部分）
    deleteInvalidPartInLineSegment(buffer5Boundary, res);
    res.sort(
        Comparator.comparingDouble(
            x -> getDistanceAlongGeometry(buffer5Boundary, x.getCoordinate(0))));
    return res;
  }

  /** 部分线段（主要存在与锐角的情况）buffer 5-15 后的 geometry 在 buffer5 里面，不符合要求，要剔除部分 */
  private List<LineSegment> deleteInvalidPartInLineSegment(
      Geometry buffer5Boundary, List<LineSegment> res) {
    for (int i = 0; i < res.size(); i++) {
      // 沿着逆时针90度方向，向外移动 10m
      LineSegment lineSegment = moveLineSegmentAlongVerticalDirection(res.get(i), 10);
      LineString moveLine = lineSegment.toGeometry(JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT);
      // 移动之后与 buffer5 是否有交集
      Geometry intersection1 = moveLine.intersection(buffer5Boundary);
      // 没有交集，说明不存在上述情况
      if (intersection1.isEmpty()) {
        continue;
      }
      // 否则就要进行截取片段
      Point intersection = (Point) intersection1;
      Coordinate co = new Coordinate(lineSegment.project(intersection.getCoordinate()));

      // 验证一下 difference 与这个的差异
      // System.out.println(moveLine.difference(buffer5));
      if (buffer5.contains(moveLine.getStartPoint())) {
        res.set(i, new LineSegment(co, res.get(i).getCoordinate(1)));
      } else if (buffer5.contains(moveLine.getEndPoint())) {
        res.set(i, new LineSegment(res.get(i).getCoordinate(1), co));
      }
    }
    return res.stream().filter(x -> x.getLength() > 0.02).collect(Collectors.toList());
  }

  /**
   * 将 res 中的 LineSegment 按照其起始点在 buffer5Boundary 中的顺序排序
   *
   * @param buffer5Boundary
   * @param res
   * @return
   */
  public List<LineSegment> sortByStartPointInBuffer5(
      Geometry buffer5Boundary, List<LineSegment> res) {
    res.sort(
        Comparator.comparingDouble(
            x -> getDistanceAlongGeometry(buffer5Boundary, x.getCoordinate(0))));
    return res;
  }

  public double getDistanceAlongGeometry(Geometry geometry, Coordinate coordinate) {
    List<LineSegment> lineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(geometry);
    List<Double> lens = new ArrayList<>();
    lens.add(0.0);
    IntStream.range(0, lineSegments.size() - 1)
        .forEach(i -> lens.add(lens.get(lens.size() - 1) + lineSegments.get(i).getLength()));
    LineSegment lineSegment =
        lineSegments.stream()
            .min(Comparator.comparingDouble(x -> x.distance(coordinate)))
            .orElseThrow(() -> new RuntimeException("geometry 不合法"));

    double distanceInCurrentLine = lineSegment.closestPoint(coordinate).distance(lineSegment.p0);
    return lens.get(lineSegments.indexOf(lineSegment)) + distanceInCurrentLine;
  }

  /** 按逆时针 90 度方向，移动一定距离 */
  public LineSegment moveLineSegmentAlongVerticalDirection(
      LineSegment lineSegment, double distance) {
    Vector2D vector2D = new Vector2D(lineSegment.getCoordinate(0), lineSegment.getCoordinate(1));
    Vector2D rotate = vector2D.rotate(Math.PI / 2);
    Vector2D multiply = rotate.normalize().multiply(distance);
    return moveLineSegment(lineSegment, multiply);
  }

  /** 移动点 */
  public static Coordinate moveCoordinate(Coordinate coordinate, Vector2D move) {
    return new Coordinate(coordinate.getX() + move.getX(), coordinate.getY() + move.getY());
  }

  /** 移动线 */
  public static LineSegment moveLineSegment(LineSegment lineSegment, Vector2D move) {
    return new LineSegment(
        moveCoordinate(lineSegment.getCoordinate(0), move),
        moveCoordinate(lineSegment.getCoordinate(1), move));
  }

  /**
   * 生成多种方案<br>
   * TODO 要考虑其他因素（红线、不可建区域）影响 buffer5 的情况
   *
   * <p>具体方法：
   * <li>用滑动窗口，找到 ls 中所有满足条件的数量最少的线段
   * <li>计算差值，在首尾两条线段上分别截取一部分，使有效长度刚好满足
   * <li>计算其登高面
   *
   * @param ls buffer5 上的有效线段
   * @param targetLength 目标的有效线段长度之和
   * @return 返回所有找到的可行的消防登高面
   */
  public List<FireClimbPlatformInfo> generatePlans(List<LineSegment> ls, double targetLength) {
    // 结果
    List<FireClimbPlatformInfo> result = new ArrayList<>();
    // 保存滑动窗口的结果，key: lastIndex, value: firstIndex
    Map<Integer, Integer> map = generateValidLineStringInBuffer5(ls, targetLength);
    int size = ls.size();

    map.forEach(
        (r, l) -> {
          int end = r >= l ? r + 1 : r + size + 1;
          List<LineSegment> lsInUse =
              IntStream.range(l, end)
                  .boxed()
                  .map(i -> ls.get(i % size))
                  .collect(Collectors.toList());
          List<List<LineSegment>> lists = trimLineString(lsInUse, targetLength);
          lists.forEach(
              x ->
                  result.add(
                      new FireClimbPlatformInfo(
                          calculateGeometryWithCoordinates(
                              buffer5,
                              x.get(0).getCoordinate(0),
                              x.get(x.size() - 1).getCoordinate(1)),
                          x)));
        });
    return result;
  }

  /**
   * lsInUse 实际长度大于 targetLength，在此修剪首尾 lineSegment 使其刚好满足要求
   *
   * @param lsInUse
   * @param targetLength
   */
  private List<List<LineSegment>> trimLineString(List<LineSegment> lsInUse, double targetLength) {
    double currentSum = lsInUse.stream().mapToDouble(LineSegment::getLength).sum();

    // 这一块要加一点误差容错
    double gap = currentSum - targetLength;
    LineSegment first = lsInUse.get(0);
    LineSegment last = lsInUse.get(lsInUse.size() - 1);
    // 对最后一条 lineSegment 进行修剪
    Coordinate end1 = findPointAlongLineSegment(last, last.getLength() - gap);
    List<LineSegment> trimLastLineSegments = new ArrayList<>(lsInUse);
    LineSegment trimLast = new LineSegment(last.getCoordinate(0), end1);
    // 此举是为了避免由于一定误差内，整个登高面增加了转角的面积
    // distance > 1 的判断只是粗略的
    if (trimLast.getLength() < 1
        && trimLast.distance(trimLastLineSegments.get(trimLastLineSegments.size() - 2)) > 1) {
      trimLastLineSegments.remove(trimLastLineSegments.size() - 1);
    } else {
      trimLastLineSegments.set(trimLastLineSegments.size() - 1, trimLast);
    }
    // 对第一条 lineSegment 进行修剪
    Coordinate start1 = findPointAlongLineSegment(first, gap);
    LineSegment trimFirst = new LineSegment(start1, first.getCoordinate(1));
    List<LineSegment> trimFirstLineSegments = new ArrayList<>(lsInUse);
    if (trimFirst.getLength() < 1
        && trimFirst.distance(trimFirstLineSegments.get(trimFirstLineSegments.size() - 2)) > 1) {
      trimFirstLineSegments.remove(0);
    } else {
      trimFirstLineSegments.set(trimFirstLineSegments.size() - 1, trimLast);
    }
    return Arrays.asList(trimFirstLineSegments, trimLastLineSegments);
  }

  /**
   * 获取多个包含若干有效线段的集合，每个集合满足，所有线段在 ls 上的 index 是连续的，且长度之和 >= targetLength（一定误差内）
   *
   * @param ls 所有的有效方案
   * @param targetLength 目标长度
   * @return key: index 区间右界 value: index 区间左界
   */
  public Map<Integer, Integer> generateValidLineStringInBuffer5(
      List<LineSegment> ls, double targetLength) {
    Map<Integer, Integer> map = new HashMap<>();
    List<Double> lens = ls.stream().map(LineSegment::getLength).collect(Collectors.toList());
    if (lens.stream().mapToDouble(Double::doubleValue).sum() < targetLength) {
      return Collections.emptyMap();
    }
    int size = lens.size();
    // 已有的长度
    double currentLength = lens.get(0);
    // index
    int left = 0;
    int right = 0;
    while (currentLength < targetLength - 0.02 && left < size && right < left + size) {
      // 长度不够，right++
      currentLength += lens.get((++right) % size);
      while (currentLength >= targetLength - 0.02) {
        // 长度多了，left++
        map.put(right % size, left % size);
        currentLength -= lens.get(left % size);
        ++left;
      }
    }
    return map;
  }

  /** 根据 buffer5 上的起始两点，得到消防登高面 */
  public Polygon calculateGeometryWithCoordinates(
      Polygon polygon, Coordinate start, Coordinate end) {
    LineString fragmentFromLinearRing =
        getFragmentFromLinearRing(polygon.getExteriorRing(), start, end);
    List<LineSegment> lineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromLineString(fragmentFromLinearRing);
    List<Polygon> polygons =
        lineSegments.stream()
            .filter(x -> x.getLength() > 1)
            .map(
                ls ->
                    lineStringSingleSizeBuffer(
                        ls.toGeometry(JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT), 10))
            .collect(Collectors.toList());
    if (1 != 1) {
      return lineStringSingleSizeBuffer(fragmentFromLinearRing, 10);
    }
    polygons.add(lineStringSingleSizeBuffer(fragmentFromLinearRing, 10));
    return (Polygon) XkGeometryUtil.generateUnionGeometryFromGeometryList(polygons);
  }

  /** 在给定参数下，对 lineString 做单侧 buffer */
  public Polygon lineStringSingleSizeBuffer(LineString lineString, double distance) {
    BufferParameters bufferParameters = new BufferParameters();
    bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
    bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
    bufferParameters.setSingleSided(true);
    bufferParameters.setMitreLimit(1.414);
    BufferOp bufferOp = new BufferOp(lineString, bufferParameters);
    return (Polygon) bufferOp.getResultGeometry(distance);
  }

  /** 找到沿 lineSegment 方向，距离为 distance 的点 */
  public Coordinate findPointAlongLineSegment(LineSegment lineSegment, double distance) {
    return lineSegment.pointAlong(distance / lineSegment.getLength());
  }

  /** 选择其中一个方案 */
  public FireClimbPlatformInfo handle() {
    return getFireClimbFaces().stream()
        .min(Comparator.comparingDouble(x -> x.getPlatform().getArea()))
        .orElse(null);
  }

  /**
   * 获取 ring 由 firstClosest secondClosest 两点截成的片段
   *
   * @param ring LinearRing
   * @param first 第一个点
   * @param second 第二个点
   * @return 首尾为 firstClosest secondClosest，方向与 ring 的方向一致
   */
  public LineString getFragmentFromLinearRing(
      LinearRing ring, Coordinate first, Coordinate second) {
    Coordinate[] coordinates = ring.getCoordinates();
    Coordinate firstClosest = findClosestPoint(ring, first);
    Coordinate secondClosest = findClosestPoint(ring, second);
    int size = coordinates.length - 1;
    int firstIndex = 0;
    int secondIndex = 0;
    // 找到 first, second 所在点 lineSegment 的起点的下标
    for (int i = 0; i < coordinates.length - 1; i++) {
      LineSegment ls = new LineSegment(coordinates[i], coordinates[i + 1]);
      if (ls.distance(firstClosest) < PRECISION) {
        firstIndex = i;
      }
      if (ls.distance(secondClosest) < PRECISION) {
        secondIndex = i;
      }
    }
    List<Coordinate> list = new ArrayList<>();
    // 加上中间的点
    if (firstIndex < secondIndex) {
      list = new ArrayList<>(Arrays.asList(coordinates).subList(firstIndex + 1, secondIndex + 1));
    } else if (firstIndex > secondIndex) {
      list =
          IntStream.range(firstIndex + 1, secondIndex + size + 1)
              .mapToObj(i -> coordinates[i % size])
              .collect(Collectors.toList());
    }
    // 加上首尾两个点
    list.add(0, firstClosest);
    list.add(secondClosest);
    // 如果 firstClosest 或者 secondClosest 与相邻的点太接近，就删掉，防止出现 LineString 方向不一致的问题
    if (list.get(0).equals2D(list.get(1), PRECISION)) {
      list.remove(0);
    }
    if (list.get(list.size() - 1).equals2D(list.get(list.size() - 2), PRECISION)) {
      list.remove(list.size() - 1);
    }
    return JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createLineString(list.toArray(new Coordinate[0]));
  }

  public Coordinate findClosestPoint(Geometry geometry, Coordinate coordinate) {
    List<LineSegment> lineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(geometry);
    LineSegment lineSegment =
        lineSegments.stream()
            .min(Comparator.comparingDouble(x -> x.distance(coordinate)))
            .orElseThrow(() -> new RuntimeException("geometry 不合法"));
    return lineSegment.closestPoint(coordinate);
  }

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

    Polygon polygon7 =
        (Polygon)
            XkGeometryIOUtil.fromWkt(
                "POLYGON ((-1.4255 6.3505, 1.3245 6.3505, 1.5245 6.3505, 1.5245 6.1505, 1.5245 5.3505, 3.1985 5.3505, 3.2975 5.3525, 3.2975 5.3505, 3.2995 5.3505, 3.2995 5.2515, 3.3075 4.7505, 6.3985 4.7505, 6.4985 4.7515, 6.4985 4.7505, 6.4995 4.7505, 6.4995 4.6515, 6.5085 3.9505, 9.3995 3.9505, 9.5995 3.9505, 9.5995 3.7505, 9.5995 0.6595, 10.3005 0.6505, 10.3995 0.6505, 10.3995 0.6495, 10.4005 0.6495, 10.3995 0.5495, 10.3995 -5.2495, 10.3995 -5.4495, 10.1995 -5.4495, 6.6085 -5.4495, 6.5995 -6.2505, 6.5995 -6.3495, 6.5985 -6.3495, 6.5985 -6.3505, 6.4985 -6.3495, 0.0995 -6.3495, -0.1005 -6.3495, -6.4995 -6.3495, -6.5995 -6.3505, -6.5995 -6.3495, -6.6005 -6.3495, -6.6005 -6.2505, -6.6095 -5.4495, -10.2005 -5.4495, -10.4005 -5.4495, -10.4005 -5.2495, -10.4005 0.4505, -10.4005 0.6505, -10.2005 0.6505, -9.6105 0.6505, -9.6005 3.8505, -9.6005 3.9505, -9.5005 3.9505, -6.5095 3.9505, -6.5005 4.6515, -6.5005 4.7505, -6.4995 4.7505, -6.4995 4.7515, -6.3995 4.7505, -3.3085 4.7505, -3.3005 5.2515, -3.3005 5.3505, -3.2985 5.3505, -3.2985 5.3525, -3.1995 5.3505, -1.5275 5.3505, -1.5255 6.2505, -1.5255 6.3505, -1.4255 6.3505))");

    Polygon polygon8 =
        (Polygon)
            XkGeometryIOUtil.fromWkt(
                "POLYGON ((-4.57071 -6.675, -8.320711 -6.675, -8.52071 -6.675, -8.52071 -6.475, -8.52071 -2.125, -8.52071 0.225, -8.52071 0.425, -8.320711 0.425, -7.620711 0.425, -7.620711 4.025, -7.620711 4.225, -7.420711 4.225, -4.620711 4.225, -4.620711 4.625, -4.620711 4.825, -4.420711 4.825, -1.720711 4.825, -1.520711 4.825, 0.879289 4.825, 0.879289 5.425, 0.779289 5.425, 0.57929 5.425, 0.57929 5.625, 0.57929 8.025, 0.57929 8.225, 0.779289 8.225, 2.97929 8.225, 5.779289 8.225, 5.779289 8.125, 5.779289 6.625, 5.779289 4.225, 7.137868 4.225, 7.17929 4.266421, 7.220711 4.225, 7.279289 4.225, 7.279289 4.166421, 7.4 4.045711, 8.250001 3.195711, 8.420711 3.025, 8.47929 3.025, 8.47929 2.966421, 8.52071 2.925, 8.47929 2.883579, 8.47929 1.158333, 8.519289 1.105, 8.47929 1.075, 8.47929 1.025, 8.412622 1.025, 8.244974 0.899263, 7.405026 0.059316, 7.279289 -0.108333, 7.279289 -0.175, 7.22929 -0.175, 7.199289 -0.215, 7.145956 -0.175, 6.67929 -0.175, 6.67929 -4.675, 6.67929 -4.875, 6.47929 -4.875, 2.97929 -4.875, -0.220711 -4.875, -0.220711 -6.575, -0.220711 -8.225, -0.320711 -8.225, -4.470711 -8.225, -4.57071 -8.225, -4.57071 -8.125, -4.57071 -6.675))");

    return Arrays.asList(
        polygon, polygon1, polygon2, polygon3, polygon4, polygon5, polygon6, polygon7, polygon8);
  }
}
