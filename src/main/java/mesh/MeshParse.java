package mesh;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import com.xkool.algo.util.geometry.XkGeometryFactory;
import com.xkool.algo.util.geometry.XkPolygonUtil;
import com.xkool.xkcommon.model.base.XkExtrudedGeometry;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * content TODO 要考虑精度
 *
 * @author luoxin
 * @since 2022/2/17
 */
@Slf4j
public class MeshParse {

  List<MyTriangle3D> triangles = new ArrayList<>();

  /** 将三角形转换为自己的模型 */
  List<MyTriangle3D> transformToTriangle() {
    List<MyTriangle3D> triangleList = new ArrayList<>();
    return triangleList;
  }

  /** 划分block TODO 要考虑基地本身也有triangle的情况 */
  void blockSplit(List<Triangle> triangleList) {}

  /**
   * 将三角形构建成 building
   *
   * <p>从z=0开始，找到所有的边，构建成底座
   *
   * <p>指定一定的递进范围（或者根据 triangle 上点的 z 值确定每次截取的 z 坐标）
   *
   * <p>指定 z 值后，截取三角形，找出所有线段，闭合成一个 geometry
   *
   * <p>与前一个geometry做一个比较，如果差别较大则，继续细分，否则下一步
   *
   * <p>加上离地高度，自身高度
   *
   * <p>TODO 可以根据正视图和侧视图大概确认其形状
   */
  List<XkExtrudedGeometry> transformToBuild(List<MyTriangle3D> allTriangles) {
    // 为了计算方便，triangleList 要去掉与平面 Z=0 平行的三角形（去掉这些三角形，其实并不影响，因为这三个边是肯定会在其他三角形中用的

    final List<MyTriangle3D> triangleList =
        allTriangles.stream()
            // 是否加上精度要看实际情况如何
            .filter(triangle -> triangle.maxZ != triangle.minZ)
            .collect(Collectors.toList());
    List<Double> zValues =
        triangleList.stream()
            .flatMap(triangle -> triangle.getCoordinates().stream().map(Coordinate::getZ))
            .distinct()
            .sorted()
            .collect(Collectors.toList());

    // 用 z=0.1 所截的 polygon 近似替代底座
    MultiPolygon previousPolygon = getGeometryByZ(triangleList, 0.1);
    List<XkExtrudedGeometry> res =
        new ArrayList<>(generateXkExtrudedGeometry(previousPolygon, 0.1, 0));
    MultiPolygon currentPolygon;
    double previousCut = 0.1;
    double currentCut;
    // z 取值按照实际所有的点的 z 值来取
    for (Double z : zValues) {
      // 初始每次取 z 值都至少间隔 0.5，不需要取值太密集
      if (z - previousCut < 0.5) {
        continue;
      }
      currentCut = z;
      currentPolygon = getGeometryByZ(triangleList, currentCut);
      // 是否需要细分
      if (!isNeedDivided(previousCut, currentCut, previousPolygon, currentPolygon)) {
        res.addAll(
            generateXkExtrudedGeometry(previousPolygon, currentCut - previousCut, previousCut));
      } else {
        // 如果差别大，则要继续细分
        res.addAll(divided(previousCut, currentCut, previousPolygon, currentPolygon, triangleList));
      }
      previousCut = currentCut;
      previousPolygon = currentPolygon;
    }

    return res;
  }

  /**
   * 将 multipolygon 转换成 XkExtrudedGeometry
   *
   * @param multiPolygon multiPolygon
   * @param height 自身高度
   * @param elevation 离地高度
   * @return XkExtrudedGeometry
   */
  List<XkExtrudedGeometry> generateXkExtrudedGeometry(
      MultiPolygon multiPolygon, double height, double elevation) {
    return IntStream.range(0, multiPolygon.getNumGeometries())
        .mapToObj(
            i -> new XkExtrudedGeometry((Polygon) multiPolygon.getGeometryN(i), height, elevation))
        .collect(Collectors.toList());
  }

  /** 判断是否需要继续细分 */
  boolean isNeedDivided(double low, double high, Geometry lowGeometry, Geometry highGeometry) {
    // 如果间距较小，就不要细分了
    if (high - low < 0.5) {
      return false;
    }
    double lowArea = lowGeometry.getArea();
    double highArea = highGeometry.getArea();
    Geometry lowGeometry2 = lowGeometry.buffer(0).intersection(lowGeometry.buffer(0));
    Geometry highGeometry2 = highGeometry.buffer(0).intersection(highGeometry.buffer(0));
    double intersectionArea = lowGeometry2.intersection(highGeometry2).getArea();
    return intersectionArea / lowArea < 0.8 || intersectionArea / highArea < 0.8;
  }

  /**
   * 在 z 取值为 low, high 之中对 triangleList 在进行划分
   *
   * <p>要区分是斜面还是突变
   */
  List<XkExtrudedGeometry> divided(
      double low,
      double high,
      MultiPolygon lowGeometry,
      MultiPolygon highGeometry,
      List<MyTriangle3D> triangleList) {
    // 取中点
    double mid = low + (high - low) / 2;
    final MultiPolygon midGeometry = getGeometryByZ(triangleList, mid);
    List<XkExtrudedGeometry> res = new ArrayList<>();
    if (isNeedDivided(low, mid, lowGeometry, midGeometry)) {
      res.addAll(divided(low, mid, lowGeometry, midGeometry, triangleList));
    } else {
      res.addAll(generateXkExtrudedGeometry(lowGeometry, mid - low, low));
    }
    if (isNeedDivided(mid, high, midGeometry, highGeometry)) {
      res.addAll(divided(mid, high, midGeometry, highGeometry, triangleList));
    } else {
      res.addAll(generateXkExtrudedGeometry(midGeometry, high - mid, mid));
    }
    return res;
  }

  /** 找到所有平行于平面 Z=0 的三角形所在的 z 轴坐标 */
  List<Double> findAllParallelToZero(List<MyTriangle3D> triangleList) {
    return triangleList.stream()
        .map(MyTriangle3D::getCoordinates)
        .filter(
            list ->
                DoubleMath.fuzzyEquals(list.get(0).getZ(), list.get(1).getZ(), 1e-3)
                    && DoubleMath.fuzzyEquals(list.get(0).getZ(), list.get(2).getZ(), 1e-3))
        .map(list -> list.get(0).getZ())
        .collect(Collectors.toList());
  }

  /**
   * 获取 Z=z 的切面 TODO 要考虑有两个以上的polygon的情况
   *
   * @param triangleList 要求所有三角形不能与平面 Z=z 平行
   * @param z 平面 Z=z
   * @return 返回所有线段连成的 polygon
   */
  MultiPolygon getGeometryByZ(List<MyTriangle3D> triangleList, double z) {
    List<LineSegment> intersectLines =
        triangleList.stream()
            .filter(triangle -> triangle.isIntersectWithZ(z))
            .flatMap(triangle -> triangle.cutByZ(z).stream())
            // 要去重，要考虑精度和方向
            .distinct()
            .collect(Collectors.toList());

    // 有可能 z 过大，没有三角形与之相交
    if (intersectLines.isEmpty() || intersectLines.size() < 3) {
      return new MultiPolygon(new Polygon[] {}, XkGeometryFactory.geometryFactory);
    }
    return transformLinesToMultiPolygon(intersectLines);
    // 以下代码暂时废弃
  }

  /** 将 lineSegment 转换成 multiPolygon */
  MultiPolygon transformLinesToMultiPolygon(List<LineSegment> list) {
    LineMerger lineMerger = new LineMerger();
    list.stream()
        .map(line -> line.toGeometry(XkGeometryFactory.geometryFactory))
        .forEach(lineMerger::add);
    List<LineString> lineStrings = (ArrayList) lineMerger.getMergedLineStrings();
    List<Polygon> polygonList = new ArrayList<>();
    // 将line转换成polygon
    for (int i = 0; i < lineStrings.size(); i++) {
      // 看看 isClosed 和 isRing 的区别
      if (lineStrings.get(i).isClosed()) {
        polygonList.add(XkPolygonUtil.createPolygon2d(lineStrings.get(i).getCoordinates()));
      } else {
        for (int j = 0; j < lineStrings.size(); j++) {
          if (DoubleMath.fuzzyEquals(
              lineStrings.get(i).getLength(), lineStrings.get(j).getLength(), 1e-3)) {
            continue;
          }
          if (lineStrings.get(i).getStartPoint().equalsExact(lineStrings.get(j).getStartPoint())
              && lineStrings.get(i).getEndPoint().equalsExact(lineStrings.get(j).getEndPoint())) {
            Coordinate[] coordinates =
                Stream.concat(
                        Arrays.stream(lineStrings.get(i).getCoordinates()),
                        Arrays.stream(lineStrings.get(j).reverse().getCoordinates()).skip(1))
                    .toArray(Coordinate[]::new);
            polygonList.add(XkPolygonUtil.createPolygon2d(coordinates));
          } else if (lineStrings
                  .get(i)
                  .getStartPoint()
                  .equalsExact(lineStrings.get(j).getEndPoint())
              && lineStrings.get(i).getEndPoint().equalsExact(lineStrings.get(j).getStartPoint())) {
            Coordinate[] coordinates =
                Stream.concat(
                        Arrays.stream(lineStrings.get(i).getCoordinates()),
                        Arrays.stream(lineStrings.get(j).getCoordinates()).skip(1))
                    .toArray(Coordinate[]::new);
            polygonList.add(XkPolygonUtil.createPolygon2d(coordinates));
          }
        }
      }
    }

    return new MultiPolygon(
        polygonList.toArray(new Polygon[] {}), XkGeometryFactory.geometryFactory);

    // 以下代码暂时废弃
  }

  public static void main(String[] args) {
    MeshParse meshParse = new MeshParse();
    Stopwatch timer = Stopwatch.createStarted();
    List<XkExtrudedGeometry> xkExtrudedGeometries =
        meshParse.transformToBuild(meshParse.generate());
    System.out.println(timer.stop());
    System.out.println(xkExtrudedGeometries);
    xkExtrudedGeometries.forEach(
        xkExtrudedGeometry -> System.out.println(xkExtrudedGeometry.getGeometry()));

    System.out.println();
  }

  List<MyTriangle3D> generate() {

    List<Double> values =
        Arrays.asList(
            301.1204691,
            394.4328942,
            0.0000000,
            203.0889731,
            254.2754138,
            0.0000000,
            3.8963603,
            394.4328942,
            0.0000000,
            301.1204691,
            254.2754138,
            0.0000000,
            301.1204691,
            0.0000000,
            0.0000000,
            0.0000000,
            0.0000000,
            0.0000000,
            203.0889731,
            131.9132091,
            0.0000000,
            301.1204691,
            131.9132091,
            0.0000000,
            3.8963603,
            394.4328942,
            410.3149606,
            0.0000000,
            0.0000000,
            0.0000000,
            0.0000000,
            0.0000000,
            410.3149606,
            3.8963603,
            394.4328942,
            0.0000000,
            3.8963603,
            394.4328942,
            0.0000000,
            56.1121421,
            394.4328942,
            217.7088168,
            301.1204691,
            394.4328942,
            0.0000000,
            3.8963603,
            394.4328942,
            410.3149606,
            56.1121421,
            394.4328942,
            291.6458246,
            239.0648980,
            394.4328942,
            291.6458246,
            92.2341757,
            394.4328942,
            459.2519685,
            152.5084147,
            394.4328942,
            410.3149606,
            43.9061824,
            394.4328942,
            410.3149606,
            104.1804213,
            394.4328942,
            459.2519685,
            239.0648980,
            394.4328942,
            217.7088168,
            301.1204691,
            394.4328942,
            410.3149606,
            301.1204691,
            394.4328942,
            0.0000000,
            301.1204691,
            254.2754138,
            410.3149606,
            301.1204691,
            254.2754138,
            0.0000000,
            301.1204691,
            394.4328942,
            410.3149606,
            301.1204691,
            254.2754138,
            410.3149606,
            203.0889731,
            254.2754138,
            0.0000000,
            301.1204691,
            254.2754138,
            0.0000000,
            203.0889731,
            254.2754138,
            410.3149606,
            203.0889731,
            254.2754138,
            0.0000000,
            203.0889731,
            131.9132091,
            410.3149606,
            203.0889731,
            131.9132091,
            0.0000000,
            203.0889731,
            254.2754138,
            410.3149606,
            203.0889731,
            131.9132091,
            410.3149606,
            301.1204691,
            131.9132091,
            0.0000000,
            203.0889731,
            131.9132091,
            0.0000000,
            301.1204691,
            131.9132091,
            410.3149606,
            301.1204691,
            131.9132091,
            0.0000000,
            301.1204691,
            0.0000000,
            410.3149606,
            301.1204691,
            0.0000000,
            0.0000000,
            301.1204691,
            131.9132091,
            410.3149606,
            301.1204691,
            0.0000000,
            0.0000000,
            276.0816295,
            0.0000000,
            194.8288566,
            0.0000000,
            0.0000000,
            0.0000000,
            301.1204691,
            0.0000000,
            410.3149606,
            276.0816295,
            0.0000000,
            275.5768881,
            46.1603696,
            0.0000000,
            275.5768881,
            104.1804213,
            0.0000000,
            459.2519685,
            43.9061824,
            0.0000000,
            410.3149606,
            152.5084147,
            0.0000000,
            410.3149606,
            92.2341757,
            0.0000000,
            459.2519685,
            46.1603696,
            0.0000000,
            194.8288566,
            0.0000000,
            0.0000000,
            410.3149606,
            43.9061824,
            0.0000000,
            410.3149606,
            3.8963603,
            394.4328942,
            410.3149606,
            0.0000000,
            0.0000000,
            410.3149606,
            43.9061824,
            394.4328942,
            410.3149606,
            92.2341757,
            394.4328942,
            459.2519685,
            43.9061824,
            0.0000000,
            410.3149606,
            92.2341757,
            0.0000000,
            459.2519685,
            43.9061824,
            394.4328942,
            410.3149606,
            104.1804213,
            0.0000000,
            459.2519685,
            92.2341757,
            394.4328942,
            459.2519685,
            92.2341757,
            0.0000000,
            459.2519685,
            104.1804213,
            394.4328942,
            459.2519685,
            152.5084147,
            394.4328942,
            410.3149606,
            104.1804213,
            0.0000000,
            459.2519685,
            152.5084147,
            0.0000000,
            410.3149606,
            104.1804213,
            394.4328942,
            459.2519685,
            152.5084147,
            0.0000000,
            410.3149606,
            203.0889731,
            131.9132091,
            410.3149606,
            152.5084147,
            394.4328942,
            410.3149606,
            301.1204691,
            0.0000000,
            410.3149606,
            301.1204691,
            131.9132091,
            410.3149606,
            203.0889731,
            254.2754138,
            410.3149606,
            301.1204691,
            394.4328942,
            410.3149606,
            301.1204691,
            254.2754138,
            410.3149606);
    List<Integer> loc =
        Arrays.asList(
            0, 1, 2, 1, 0, 3, 2, 4, 5, 4, 2, 6, 6, 2, 1, 4, 6, 7, 8, 9, 10, 9, 8, 11, 12, 13, 14,
            13, 12, 15, 13, 15, 16, 16, 15, 17, 18, 19, 20, 19, 18, 21, 14, 22, 23, 22, 14, 13, 23,
            22, 17, 23, 17, 15, 23, 15, 20, 23, 20, 19, 24, 25, 26, 25, 24, 27, 28, 29, 30, 29, 28,
            31, 32, 33, 34, 33, 32, 35, 36, 37, 38, 37, 36, 39, 40, 41, 42, 41, 40, 43, 44, 45, 46,
            45, 44, 47, 45, 47, 48, 48, 47, 49, 50, 51, 52, 51, 50, 53, 46, 54, 55, 54, 46, 45, 55,
            54, 49, 55, 49, 47, 55, 47, 52, 55, 52, 51, 56, 57, 58, 57, 56, 59, 60, 61, 62, 61, 60,
            63, 64, 65, 66, 65, 64, 67, 68, 69, 70, 69, 68, 71, 72, 73, 74, 73, 72, 75, 73, 75, 76,
            74, 77, 78, 77, 74, 73, 78, 77, 79);

    List<Double> values2 =
        Arrays.asList(
            276.0816295,
            -141.6535433,
            275.5768881,
            46.1603696,
            -141.6535433,
            194.8288566,
            276.0816295,
            -141.6535433,
            194.8288566,
            46.1603696,
            -141.6535433,
            275.5768881,
            276.0816295,
            0.0000000,
            194.8288566,
            276.0816295,
            -141.6535433,
            275.5768881,
            276.0816295,
            -141.6535433,
            194.8288566,
            276.0816295,
            0.0000000,
            275.5768881,
            276.0816295,
            -141.6535433,
            275.5768881,
            46.1603696,
            0.0000000,
            275.5768881,
            46.1603696,
            -141.6535433,
            275.5768881,
            276.0816295,
            0.0000000,
            275.5768881,
            46.1603696,
            0.0000000,
            275.5768881,
            46.1603696,
            -141.6535433,
            194.8288566,
            46.1603696,
            -141.6535433,
            275.5768881,
            46.1603696,
            0.0000000,
            194.8288566,
            276.0816295,
            0.0000000,
            194.8288566,
            46.1603696,
            -141.6535433,
            194.8288566,
            46.1603696,
            0.0000000,
            194.8288566,
            276.0816295,
            -141.6535433,
            194.8288566);
    List<Integer> loc2 =
        Arrays.asList(
            0, 1, 2, 1, 0, 3, 4, 5, 6, 5, 4, 7, 8, 9, 10, 9, 8, 11, 12, 13, 14, 13, 12, 15, 16, 17,
            18, 17, 16, 19);

    List<Double> values3 =
        Arrays.asList(
            56.1121421,
            522.1887997,
            291.6458246,
            239.0648980,
            522.1887997,
            217.7088168,
            56.1121421,
            522.1887997,
            217.7088168,
            239.0648980,
            522.1887997,
            291.6458246,
            239.0648980,
            522.1887997,
            217.7088168,
            239.0648980,
            394.4328942,
            291.6458246,
            239.0648980,
            394.4328942,
            217.7088168,
            239.0648980,
            522.1887997,
            291.6458246,
            239.0648980,
            522.1887997,
            217.7088168,
            56.1121421,
            394.4328942,
            217.7088168,
            56.1121421,
            522.1887997,
            217.7088168,
            239.0648980,
            394.4328942,
            217.7088168,
            56.1121421,
            522.1887997,
            291.6458246,
            56.1121421,
            394.4328942,
            217.7088168,
            56.1121421,
            394.4328942,
            291.6458246,
            56.1121421,
            522.1887997,
            217.7088168,
            239.0648980,
            394.4328942,
            291.6458246,
            56.1121421,
            522.1887997,
            291.6458246,
            56.1121421,
            394.4328942,
            291.6458246,
            239.0648980,
            522.1887997,
            291.6458246);
    List<Integer> loc3 =
        Arrays.asList(
            0, 1, 2, 1, 0, 3, 4, 5, 6, 5, 4, 7, 8, 9, 10, 9, 8, 11, 12, 13, 14, 13, 12, 15, 16, 17,
            18, 17, 16, 19);
    List<Coordinate> coordinateList =
        Lists.partition(values, 3).stream()
            .map(list -> new Coordinate(list.get(0), list.get(1), list.get(2)))
            .collect(Collectors.toList());
    List<MyTriangle3D> triangle3DList =
        Lists.partition(loc, 3).stream()
            .map(
                list ->
                    new MyTriangle3D(
                        coordinateList.get(list.get(0)),
                        coordinateList.get(list.get(1)),
                        coordinateList.get(list.get(2))))
            .collect(Collectors.toList());
    List<Coordinate> coordinateList2 =
        Lists.partition(values2, 3).stream()
            .map(list -> new Coordinate(list.get(0), list.get(1), list.get(2)))
            .collect(Collectors.toList());
    List<MyTriangle3D> triangle3DList2 =
        Lists.partition(loc2, 3).stream()
            .map(
                list ->
                    new MyTriangle3D(
                        coordinateList2.get(list.get(0)),
                        coordinateList2.get(list.get(1)),
                        coordinateList2.get(list.get(2))))
            .collect(Collectors.toList());

    List<Coordinate> coordinateList3 =
        Lists.partition(values3, 3).stream()
            .map(list -> new Coordinate(list.get(0), list.get(1), list.get(2)))
            .collect(Collectors.toList());
    List<MyTriangle3D> triangle3DList3 =
        Lists.partition(loc3, 3).stream()
            .map(
                list ->
                    new MyTriangle3D(
                        coordinateList3.get(list.get(0)),
                        coordinateList3.get(list.get(1)),
                        coordinateList3.get(list.get(2))))
            .collect(Collectors.toList());
    triangle3DList.addAll(triangle3DList2);
    triangle3DList.addAll(triangle3DList3);
    triangle3DList.addAll(generateTri());
    return triangle3DList;
  }

  List<MyTriangle3D> generateTri() {
    List<Coordinate> coordinateList =
        Lists.partition(generateDouble(), 3).stream()
            .map(list -> new Coordinate(list.get(0), list.get(1), list.get(2)))
            .collect(Collectors.toList());
    List<MyTriangle3D> triangle3DList3 =
        Lists.partition(generateInt(), 3).stream()
            .map(
                list ->
                    new MyTriangle3D(
                        coordinateList.get(list.get(0)),
                        coordinateList.get(list.get(1)),
                        coordinateList.get(list.get(2))))
            .collect(Collectors.toList());
    return triangle3DList3;
  }

  List<Integer> generateInt() {
    return Arrays.asList(
        0, 1, 2, 1, 0, 3, 4, 5, 6, 5, 4, 7, 8, 9, 10, 9, 8, 11, 12, 13, 14, 13, 12, 15, 16, 17, 18,
        17, 16, 19, 20, 21, 22, 21, 20, 23);
  }

  List<Double> generateDouble() {
    return Arrays.asList(
        516.7715654,
        272.6935770,
        0.0000000,
        413.1495181,
        0.3707424,
        0.0000000,
        413.1495181,
        272.6935770,
        0.0000000,
        516.7715654,
        0.3707424,
        0.0000000,
        413.1495181,
        272.6935770,
        401.1417323,
        413.1495181,
        0.3707424,
        0.0000000,
        413.1495181,
        0.3707424,
        401.1417323,
        413.1495181,
        272.6935770,
        0.0000000,
        413.1495181,
        272.6935770,
        401.1417323,
        516.7715654,
        272.6935770,
        0.0000000,
        413.1495181,
        272.6935770,
        0.0000000,
        516.7715654,
        272.6935770,
        401.1417323,
        516.7715654,
        272.6935770,
        0.0000000,
        516.7715654,
        0.3707424,
        401.1417323,
        516.7715654,
        0.3707424,
        0.0000000,
        516.7715654,
        272.6935770,
        401.1417323,
        516.7715654,
        0.3707424,
        401.1417323,
        413.1495181,
        0.3707424,
        0.0000000,
        516.7715654,
        0.3707424,
        0.0000000,
        413.1495181,
        0.3707424,
        401.1417323,
        516.7715654,
        0.3707424,
        401.1417323,
        413.1495181,
        272.6935770,
        401.1417323,
        413.1495181,
        0.3707424,
        401.1417323,
        516.7715654,
        272.6935770,
        401.1417323);
  }
}
