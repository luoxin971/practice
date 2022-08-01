package fireclimb;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.xkool.algo.util.geometry.XkGeometryUtil;
import com.xkool.algo.util.geometry.XkLineSegmentUtil;
import com.xkool.algo.util.plotter.XkPlotter;
import lombok.Data;
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

import static fireclimb.FireClimbConstant.PRECISION;

/**
 * 消防登高，初步完成整个流程，需要进一步优化 <br>
 *
 * <ul>
 *   <li>两个可行线段相连的情况
 *   <li>重新梳理 ccw 相关的问题
 *   <li>在复杂楼型上验证
 * </ul>
 *
 * @author luoxin
 * @since 2022/6/1
 */
@ToString
@Data
public class FireClimb {

  private Polygon originPolygon;

  private Polygon afterPolygon;

  private Polygon simplifiedPolygon;

  private Polygon buffer5;

  private List<FireClimbPlatformInfo> result;

  public FireClimb(Polygon originPolygon) {
    this.originPolygon = originPolygon;
    initialize(originPolygon);
  }

  public void showAllOutcome(List<FireClimbPlatformInfo> list, String title) {
    if (CollUtil.isEmpty(list) || CollUtil.hasNull(list)) {
      System.out.println("Invalid: " + this.originPolygon);
      return;
    }
    System.out.println(title + "begin");
    list.forEach(x -> System.out.println(x.getPlatform()));
    list.forEach(
        x -> {
          XkPlotter xkPlotter = new XkPlotter(title);
          xkPlotter.addContent(this.afterPolygon, Color.blue);
          xkPlotter.addContent(buffer5, Color.blue);
          xkPlotter.addContent(x.getPlatform().getBoundary(), Color.red);
          x.getLineSegments()
              .forEach(
                  l ->
                      xkPlotter.addContent(
                          l.toGeometry(FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT),
                          Color.orange));
          xkPlotter.plot();
        });
    System.out.println(title + "end");
  }

  /** 选择默认方案，面积最小 */
  public FireClimbPlatformInfo getDefaultPlatform() {
    return getAllPlatforms().stream()
        .min(Comparator.comparingDouble(x -> x.getPlatform().getArea()))
        // .min(Comparator.comparingDouble(x -> x.getPlatform().getCoordinates().length))
        .orElseThrow(() -> new RuntimeException("无法生成"));
  }

  /** 获取所有登高面 */
  public List<FireClimbPlatformInfo> getAllPlatforms() {
    if (!CollUtil.isEmpty(result)) {
      return result;
    }
    // 建筑上所有有效的登高面
    List<LineSegment> faces = generateLines();
    // 面宽
    double width = getBuildingWidth();
    // 获取所有的登高面
    List<FireClimbPlatformInfo> fireClimbPlatformInfos =
        getAllPlatforms(originPolygon, buffer5, faces, width);
    this.result = fireClimbPlatformInfos;
    return fireClimbPlatformInfos;
  }

  /**
   * 根据 polygon 轮廓，起点，方向求出登高面
   *
   * <ol>
   *   主要思路：
   *   <li>找出 polygon 上有效线段 validLineSegments
   *   <li>调整上述线段的方向，使其按 isCCW 排列
   *   <li>找出 start 在 polygon 轮廓上所在的线段
   *   <li>将有效线段按照在轮廓上与 start 的距离（计算线段终点的距离，考虑方向）进行排序
   *   <li>选择符合要求的终点
   *   <li>根据起点和终点以及polygon轮廓，求出其 buffer 10m 的 polygon
   * </ol>
   *
   * @param polygon buffer5 的轮廓
   * @param targetLength 待生成的登高面的长度
   * @param start 起点
   * @param isCCW 方向，是否逆时针
   * @return 消防登高面
   */
  public FireClimbPlatformInfo getFirePlatformWithStartPoint(
      Polygon polygon, double targetLength, Coordinate start, boolean isCCW) {
    // TODO 先只考虑 polygon 的情况
    // polygon 上的有效线段，validLineSegments 顺时针排列
    List<LineSegment> validLineSegments = generateLines(polygon, simplifiedPolygon);
    if (isCCW) {
      // 如果指定逆时针，则需要调整 validLineSegments 方向和顺序
      validLineSegments = CollectionUtil.reverse(validLineSegments);
      validLineSegments.forEach(LineSegment::reverse);
    }
    // polygon 上的所有线段
    List<LineSegment> allLines =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(polygon.getExteriorRing());
    // 防止 start 有误差，还是要找最近的点
    LineSegment closestLineSegment =
        allLines.stream()
            .min(Comparator.comparingDouble(x -> x.distance(start)))
            .orElse(allLines.get(0));
    Coordinate startCoordinate = closestLineSegment.closestPoint(start);
    // 按线段终点到 startCoordinate 在轮廓上的距离排序
    validLineSegments.sort(
        Comparator.comparingDouble(
            x ->
                getDistanceAlongLinearRing(
                    polygon.getExteriorRing(), startCoordinate, x.p1, isCCW)));
    // 如果起点在有效线段上，就将其一分为二
    if (validLineSegments.get(0).distance(startCoordinate) < PRECISION) {
      LineSegment firstLine = validLineSegments.get(0);
      LineSegment preLine = new LineSegment(startCoordinate, firstLine.p1);
      if (preLine.getLength() < FireClimbConstant.PRECISION) {
        validLineSegments.remove(0);
      } else {
        validLineSegments.set(0, preLine);
      }
      LineSegment line = new LineSegment(firstLine.p0, startCoordinate);
      if (line.getLength() > FireClimbConstant.PRECISION) {
        validLineSegments.add(line);
      }
    }
    // 找到有效线段长度刚好为 targetLength 的 LineString 的终点
    List<Double> validDistance = new ArrayList<>();
    validDistance.add(0.);
    List<LineSegment> finalValidLineSegments = validLineSegments;
    IntStream.range(0, validLineSegments.size())
        .forEach(
            i ->
                validDistance.add(
                    validDistance.get(i) + finalValidLineSegments.get(i).getLength()));
    // validDistance.get(i) 为 validLineSegments 0-i（双闭区间） 线段长度之和
    validDistance.remove(0);
    // 刚好在 indexMatch 上符合条件
    int indexMatch = Arrays.binarySearch(validDistance.toArray(new Double[0]), targetLength);
    if (indexMatch < 0) {
      indexMatch = -(indexMatch + 1);
    }
    int size = validLineSegments.size();
    // 前一个 index
    int preIndex = Math.max(indexMatch - 1, 0);
    if (indexMatch >= size) {
      // 说明总长度不够
      // throw new Exception("不可以");
      System.out.println("No");
    }
    double distanceGap = targetLength - validDistance.get(preIndex);
    Coordinate endCoordinate =
        distanceGap < FireClimbConstant.TOTAL_LENGTH_TOLERANCE
            ? validLineSegments.get(preIndex).p1
            : validLineSegments
                .get(indexMatch)
                .pointAlong(distanceGap / validLineSegments.get(indexMatch).getLength());
    // 根据起始点及polygon，计算出 buffer 后的 polygon
    Polygon resultPolygon =
        calculateGeometryWithCoordinates(polygon, startCoordinate, endCoordinate, isCCW);
    // 有效线段
    List<LineSegment> resultLineSegments = validLineSegments.subList(0, preIndex);
    resultLineSegments.add(new LineSegment(validLineSegments.get(indexMatch).p0, endCoordinate));
    return new FireClimbPlatformInfo(resultPolygon, resultLineSegments);
  }

  /** 获取建筑面宽 */
  public double getBuildingWidth() {
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
    originPolygon = (Polygon) FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT.createGeometry(polygon);
    // 简化后的 polygon
    Polygon simplify = (Polygon) TopologyPreservingSimplifier.simplify(originPolygon, 0.05);
    // 去掉 hole
    afterPolygon =
        FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(simplify.getExteriorRing());
    simplify = PolygonSimplifyUtil.patchSlopingSide(afterPolygon);

    simplifiedPolygon =
        Orientation.isCCW(simplify.getCoordinates()) ? simplify.reverse() : simplify;

    buffer5 =
        (Polygon) simplifiedPolygon.buffer(FireClimbConstant.FIRE_PLATFORM_MINIMUM_DISTANCE, -100);
    buffer5 = PolygonSimplifyUtil.patchAcute(PolygonSimplifyUtil.removeShortEdge(buffer5));
    buffer5 = Orientation.isCCW(buffer5.getCoordinates()) ? buffer5.reverse() : buffer5;
  }

  public List<LineSegment> generateLines() {
    return generateLines(buffer5, simplifiedPolygon);
  }

  /** 获取 polygon 上有效的登高面，按顺时针方向排列 */
  public static List<LineSegment> generateLines(Polygon buffer, Polygon origin) {

    LinearRing polygonBoundary = buffer.getExteriorRing();
    LinearRing boundary =
        Orientation.isCCW(polygonBoundary.getCoordinates())
            ? polygonBoundary.reverse()
            : polygonBoundary;
    List<LineSegment> buffer5BoundaryLineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(boundary);
    List<LineSegment> res = new ArrayList<>();
    // 对每一条边单独做 buffer，判断是否与 boundary 有交集
    List<LineSegment> allLineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(origin).stream()
            .flatMap(
                x -> {
                  Geometry intersection =
                      XkGeometryUtil.generateSingleSideBufferedGeometry(
                              x.toGeometry(FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT), 10)
                          .intersection(boundary);
                  List<LineSegment> lineSegments =
                      XkLineSegmentUtil.generateLineSegmentListFromGeometry(intersection);
                  return lineSegments.stream()
                      .filter(
                          ls ->
                              ls.getLength() > 0.02
                                  && Angle.diff(x.angle(), ls.angle())
                                      < FireClimbConstant.ANGLE_PRECISION);
                })
            .collect(Collectors.toList());
    allLineSegments.forEach(
        lineSegment -> {
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
    // 修剪部分 lineSegment（主要是成锐角的部分）
    List<LineSegment> lineSegments = deleteInvalidPartInLineSegment(boundary, res);
    lineSegments.sort(
        Comparator.comparingDouble(x -> getDistanceAlongLinearRing(boundary, x.getCoordinate(0))));
    // 删除重合的 lineSegments
    for (int i = 0; i < lineSegments.size(); i++) {
      LineSegment l1 = lineSegments.get(i);
      LineSegment l2 = lineSegments.get((i + 1) % lineSegments.size());
      if (Angle.diff(l1.angle(), l2.angle()) < Math.toRadians(1) && l1.distance(l2) < 0.02) {
        l1.setCoordinates(l1.p0, l1.p0.distance(l1.p1) > l1.p0.distance(l2.p1) ? l1.p1 : l2.p1);
        lineSegments.remove((i + 1) % lineSegments.size());
        --i;
      }
    }
    return lineSegments;
  }
  /** 部分线段（主要存在与锐角的情况）buffer 5-15 后的 geometry 在 buffer5 里面，不符合要求，要剔除部分 */
  public static List<LineSegment> deleteInvalidPartInLineSegment(
      LinearRing buffer5Boundary, List<LineSegment> res) {
    for (int i = 0; i < res.size(); i++) {
      // 沿着逆时针90度方向，向外移动 10m
      LineSegment lineSegment = moveLineSegmentAlongVerticalDirection(res.get(i), 10);
      LineString moveLine = lineSegment.toGeometry(FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT);
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
      Coordinate project = res.get(i).project(co);
      Polygon bufferPolygon =
          FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(buffer5Boundary);
      if (bufferPolygon.contains(moveLine.getStartPoint())) {
        res.set(i, new LineSegment(res.get(i).getCoordinate(1), project));
      } else if (bufferPolygon.contains(moveLine.getEndPoint())) {
        res.set(i, new LineSegment(project, res.get(i).getCoordinate(1)));
      }
    }
    return res.stream().filter(x -> x.getLength() > 0.02).collect(Collectors.toList());
  }

  public static double getDistanceAlongLinearRing(LinearRing linearRing, Coordinate coordinate) {
    List<LineSegment> lineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(linearRing);
    List<Double> lens = new ArrayList<>();
    lens.add(0.0);
    IntStream.range(0, lineSegments.size() - 1)
        .forEach(i -> lens.add(lens.get(lens.size() - 1) + lineSegments.get(i).getLength()));
    LineSegment lineSegment =
        lineSegments.stream()
            .min(Comparator.comparingDouble(x -> x.distance(coordinate)))
            .orElseThrow(() -> new RuntimeException("linearRing 不合法"));

    double distanceInCurrentLine = lineSegment.closestPoint(coordinate).distance(lineSegment.p0);
    return lens.get(lineSegments.indexOf(lineSegment)) + distanceInCurrentLine;
  }

  /** ring 上从 start 到 end 到距离，方向由 isCCW 决定 */
  public static double getDistanceAlongLinearRing(
      LinearRing ring, Coordinate start, Coordinate end, boolean isCCW) {
    Coordinate[] ringCoordinates = ring.getCoordinates();
    // 方向不一致，就翻转
    if (Orientation.isCCW(ringCoordinates) != isCCW) {
      ring = ring.reverse();
    }
    List<LineSegment> allLineSegments = XkLineSegmentUtil.generateLineSegmentListFromGeometry(ring);
    LineSegment startClosestLine =
        allLineSegments.stream()
            .min(Comparator.comparingDouble(x -> x.distance(start)))
            .orElse(allLineSegments.get(0));
    Coordinate startProject = startClosestLine.closestPoint(start);
    LineSegment endClosestLine =
        allLineSegments.stream()
            .min(Comparator.comparingDouble(x -> x.distance(end)))
            .orElse(allLineSegments.get(0));
    Coordinate endProject = endClosestLine.closestPoint(end);
    int startIndex = allLineSegments.indexOf(startClosestLine);

    int endIndex = allLineSegments.indexOf(endClosestLine);
    int size = allLineSegments.size();
    // 如果在同一条线段上
    if (startIndex == endIndex) {
      double distance = startProject.distance(endProject);
      // 理论上角度差值只可能为 0 或者 Math.PI
      if (Angle.diff(startClosestLine.angle(), Angle.angle(startProject, endProject)) < PRECISION) {
        return distance;
      } else {
        return allLineSegments.stream().mapToDouble(LineSegment::getLength).sum() - distance;
      }
    } else if (startIndex > endIndex) {
      endIndex += size;
    }
    return startProject.distance(startClosestLine.p1)
        + endProject.distance(endClosestLine.p0)
        + IntStream.range(startIndex + 1, endIndex)
            .boxed()
            .mapToDouble(i -> allLineSegments.get(i % size).getLength())
            .sum();
  }

  /** 按逆时针 90 度方向，移动一定距离 */
  public static LineSegment moveLineSegmentAlongVerticalDirection(
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
   * @param origin
   * @param buffer
   * @param ls buffer5 上的有效线段
   * @param targetLength 目标的有效线段长度之和
   * @return 返回所有找到的可行的消防登高面
   */
  public static List<FireClimbPlatformInfo> getAllPlatforms(
      Polygon origin, Polygon buffer, List<LineSegment> ls, double targetLength) {
    // 结果
    List<FireClimbPlatformInfo> result = new ArrayList<>();
    // 保存滑动窗口的结果，key: lastIndex, value: firstIndex
    Map<Integer, Integer> map = generateValidLineSegmentsInBuffer5(ls, targetLength);
    int size = ls.size();

    map.forEach(
        (r, l) -> {
          int end = r >= l ? r + 1 : r + size + 1;
          List<LineSegment> lsInUse =
              IntStream.range(l, end)
                  .boxed()
                  .map(i -> ls.get(i % size))
                  .collect(Collectors.toList());
          List<List<LineSegment>> lists = generateValidLineStringInBuffer5(lsInUse, targetLength);
          lists.forEach(
              x ->
                  result.add(
                      new FireClimbPlatformInfo(
                          calculateGeometryWithCoordinates(
                              buffer,
                              x.get(0).getCoordinate(0),
                              x.get(x.size() - 1).getCoordinate(1),
                              false),
                          x)));
        });
    return result.stream()
        .filter(
            x ->
                x.getPlatform().distance(origin)
                    > FireClimbConstant.FIRE_PLATFORM_MINIMUM_DISTANCE
                        - FireClimbConstant.TOTAL_LENGTH_TOLERANCE)
        .collect(Collectors.toList());
  }

  /** lsInUse 实际长度大于 targetLength，在此修剪首尾 lineSegment 使其刚好满足要求 */
  private static List<List<LineSegment>> generateValidLineStringInBuffer5(
      List<LineSegment> lsInUse, double targetLength) {
    double currentSum = lsInUse.stream().mapToDouble(LineSegment::getLength).sum();

    double gap = currentSum - targetLength;
    LineSegment first = lsInUse.get(0);
    LineSegment last = lsInUse.get(lsInUse.size() - 1);
    // 对最后一条 lineSegment 进行修剪
    Coordinate end1 = findPointAlongLineSegment(last, last.getLength() - gap);
    List<LineSegment> trimLastLineSegments = new ArrayList<>(lsInUse);
    LineSegment trimLast = new LineSegment(last.getCoordinate(0), end1);
    // 此举是为了避免由于一定误差内，整个登高面增加了转角的面积
    // distance > 1 的判断只是粗略的
    if (trimLast.getLength() < FireClimbConstant.TOTAL_LENGTH_TOLERANCE
        && trimLast.distance(trimLastLineSegments.get(trimLastLineSegments.size() - 2))
            > FireClimbConstant.TOTAL_LENGTH_TOLERANCE) {
      trimLastLineSegments.remove(trimLastLineSegments.size() - 1);
    } else {
      trimLastLineSegments.set(trimLastLineSegments.size() - 1, trimLast);
    }
    // 对第一条 lineSegment 进行修剪
    Coordinate start1 = findPointAlongLineSegment(first, gap);
    LineSegment trimFirst = new LineSegment(start1, first.getCoordinate(1));
    List<LineSegment> trimFirstLineSegments = new ArrayList<>(lsInUse);
    if (trimFirst.getLength() < FireClimbConstant.TOTAL_LENGTH_TOLERANCE
        && trimFirst.distance(trimFirstLineSegments.get(trimFirstLineSegments.size() - 2))
            > FireClimbConstant.TOTAL_LENGTH_TOLERANCE) {
      trimFirstLineSegments.remove(0);
    } else {
      trimFirstLineSegments.set(0, trimFirst);
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
  public static Map<Integer, Integer> generateValidLineSegmentsInBuffer5(
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
    while (currentLength < targetLength - FireClimbConstant.TOTAL_LENGTH_TOLERANCE
        && left < size
        && right < left + size) {
      // 长度不够，right++
      currentLength += lens.get((++right) % size);
      while (currentLength >= targetLength - FireClimbConstant.TOTAL_LENGTH_TOLERANCE) {
        // 长度多了，left++
        map.put(right % size, left % size);
        currentLength -= lens.get(left % size);
        ++left;
      }
    }
    return map;
  }

  /** 根据 polygon 上的起始两点，按顺时针顺序从 start 到 end，并生成该 lineString buffer 10 的 polygon */
  public static Polygon calculateGeometryWithCoordinates(
      Polygon polygon, Coordinate start, Coordinate end, boolean isCCW) {
    LineString fragmentFromLinearRing =
        getFragmentFromLinearRing(polygon.getExteriorRing(), start, end, isCCW);
    List<LineSegment> lineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromLineString(fragmentFromLinearRing);
    int bufferDistance = isCCW ? -10 : 10;
    List<Polygon> polygons =
        lineSegments.stream()
            .filter(x -> x.getLength() > 1)
            .map(
                ls ->
                    lineStringSingleSizeBuffer(
                        ls.toGeometry(FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT),
                        bufferDistance))
            .collect(Collectors.toList());
    if (1 != 1) {
      return lineStringSingleSizeBuffer(fragmentFromLinearRing, bufferDistance);
    }
    polygons.add(lineStringSingleSizeBuffer(fragmentFromLinearRing, bufferDistance));
    return (Polygon) XkGeometryUtil.generateUnionGeometryFromGeometryList(polygons);
  }

  /** 在给定参数下，对 lineString 做单侧 buffer */
  public static Polygon lineStringSingleSizeBuffer(LineString lineString, double distance) {
    BufferParameters bufferParameters = new BufferParameters();
    bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
    bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
    bufferParameters.setSingleSided(true);
    // bufferParameters.setMitreLimit(1.414);
    BufferOp bufferOp = new BufferOp(lineString, bufferParameters);
    Geometry resultGeometry = bufferOp.getResultGeometry(distance);
    if (resultGeometry instanceof MultiPolygon) {
      Integer index =
          IntStream.range(0, resultGeometry.getNumGeometries())
              .boxed()
              .max(Comparator.comparingDouble(x -> resultGeometry.getGeometryN(x).getArea()))
              .orElse(0);
      return (Polygon) resultGeometry.getGeometryN(index);
    }
    return (Polygon) resultGeometry;
  }

  /** 找到沿 lineSegment 方向，距离为 distance 的点 */
  public static Coordinate findPointAlongLineSegment(LineSegment lineSegment, double distance) {
    return lineSegment.pointAlong(distance / lineSegment.getLength());
  }

  /**
   * 获取 ring 由 start end 两点按 isCCW 方向截成的 lineString
   *
   * <ol>
   *   主要思路：
   *   <li>确保 ring 的方向与 isCCW 保持一致
   *   <li>找到 start, end 在 ring 上所在的线段
   *   <li>依次插入中间的点
   * </ol>
   *
   * @param ring LinearRing
   * @param start 起始点
   * @param end 终点
   * @param isCCW 是否逆时针
   * @return 首尾为 firstClosest secondClosest，方向与 ring 的方向一致
   */
  public static LineString getFragmentFromLinearRing(
      LinearRing ring, Coordinate start, Coordinate end, boolean isCCW) {
    Coordinate[] coordinates = ring.getCoordinates();
    // 如果方向不一致，则需要翻转方向
    if (Orientation.isCCW(coordinates) != isCCW) {
      ring = ring.reverse();
    }
    Coordinate firstClosest = findClosestPoint(ring, start);
    Coordinate secondClosest = findClosestPoint(ring, end);
    int size = coordinates.length - 1;
    int firstIndex = 0;
    int secondIndex = 0;
    // 找到 start, end 所在点 lineSegment 的起点的下标
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
    if (list.get(0).equals2D(list.get(1), FireClimbConstant.PRECISION)) {
      list.remove(0);
    }
    if (list.get(list.size() - 1)
        .equals2D(list.get(list.size() - 2), FireClimbConstant.PRECISION)) {
      list.remove(list.size() - 1);
    }
    return FireClimbConstant.GEOMETRY_FACTORY_TWO_DIGIT.createLineString(
        list.toArray(new Coordinate[0]));
  }

  public static Coordinate findClosestPoint(Geometry geometry, Coordinate coordinate) {
    List<LineSegment> lineSegments =
        XkLineSegmentUtil.generateLineSegmentListFromGeometry(geometry);
    LineSegment lineSegment =
        lineSegments.stream()
            .min(Comparator.comparingDouble(x -> x.distance(coordinate)))
            .orElseThrow(() -> new RuntimeException("geometry 不合法"));
    return lineSegment.closestPoint(coordinate);
  }
}
