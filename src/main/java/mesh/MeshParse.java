package mesh;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import com.xkool.algo.util.constant.PolygonConstant;
import com.xkool.algo.util.geometry.XkGeometryFactory;
import com.xkool.algo.util.geometry.XkPolygonUtil;
import com.xkool.xkcommon.model.base.XkExtrudedGeometry;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.util.*;
import java.util.stream.Collectors;
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
    List<XkExtrudedGeometry> res = new ArrayList<>();
    List<Double> zValues =
        triangleList.stream()
            .flatMap(triangle -> triangle.getCoordinates().stream().map(Coordinate::getZ))
            .distinct()
            .sorted()
            .collect(Collectors.toList());

    // 用 z=0.1 所截的 polygon 近似替代底座
    Polygon previousPolygon = getGeometryByZ(triangleList, 0.1);
    res.add(new XkExtrudedGeometry(previousPolygon, 0.1, 0));
    Polygon currentPolygon = PolygonConstant.POLYGON_EMPTY;
    double previousCut = 0.1;
    double currentCut = 0.0;
    // z 取值按照实际所有的点的 z 值来取
    for (Double z : zValues) {
      // 初始每次取 z 值都至少间隔 0.5，不需要取值太密集
      if (z - previousCut < 0.5) {
        continue;
      }
      currentCut = z - 0.1;
      currentPolygon = getGeometryByZ(triangleList, currentCut);
      // 是否需要细分
      if (!isNeedDivided(previousCut, currentCut, previousPolygon, currentPolygon)) {
        res.add(new XkExtrudedGeometry(previousPolygon, currentCut - previousCut, previousCut));
      } else {
        // 如果差别大，则要继续细分
        res.addAll(divided(previousCut, currentCut, previousPolygon, currentPolygon, triangleList));
      }
      previousCut = currentCut;
      previousPolygon = currentPolygon;
    }

    return res;
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
      Polygon lowGeometry,
      Polygon highGeometry,
      List<MyTriangle3D> triangleList) {
    // 取中点
    double mid = low + (high - low) / 2;
    final Polygon midGeometry = getGeometryByZ(triangleList, mid);
    List<XkExtrudedGeometry> res = new ArrayList<>();
    if (isNeedDivided(low, mid, lowGeometry, midGeometry)) {
      res.addAll(divided(low, mid, lowGeometry, midGeometry, triangleList));
    } else {
      res.add(new XkExtrudedGeometry(lowGeometry, mid - low, low));
    }
    if (isNeedDivided(mid, high, midGeometry, highGeometry)) {
      res.addAll(divided(mid, high, midGeometry, highGeometry, triangleList));
    } else {
      res.add(new XkExtrudedGeometry(midGeometry, high - mid, mid));
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
  Polygon getGeometryByZ(List<MyTriangle3D> triangleList, double z) {
    List<LineSegment> intersectLines =
        triangleList.stream()
            .filter(triangle -> triangle.isIntersectWithZ(z))
            .flatMap(triangle -> triangle.cutByZ(z).stream())
            // 要去重，要考虑精度和方向
            .distinct()
            .collect(Collectors.toList());

    // 有可能 z 过大，没有三角形与之相交
    if (intersectLines.isEmpty() || intersectLines.size() < 3) {
      return PolygonConstant.POLYGON_EMPTY;
    }
    if (1 == 1) {
      return test(intersectLines);
    }
    LineSegment currentLine = intersectLines.get(0);
    List<LineSegment> linesCache = new ArrayList<>(intersectLines);
    List<LineSegment> resultLines = new ArrayList<>();
    resultLines.add(currentLine);
    intersectLines.remove(currentLine);
    // 按顺序找出线段
    while (true) {
      int size = linesCache.size();
      for (LineSegment line : intersectLines) {
        if (currentLine.p1.equals2D(line.p0, 1e-3)) {
          if (!currentLine.p0.equals2D(line.p1, 1e-3)) {
            resultLines.add(line);
          }
          linesCache.remove(line);
          break;
        } else if (currentLine.p1.equals2D(line.p1, 1e-3)) {
          if (!currentLine.p0.equals2D(line.p0, 1e-3)) {
            line.reverse();
            resultLines.add(line);
          }
          linesCache.remove(line);
          break;
        }
      }
      currentLine = resultLines.get(resultLines.size() - 1);
      intersectLines = linesCache;
      if (linesCache.size() == size) {
        break;
      }
    }
    // 处理意外逻辑，比如不闭合等
    // 暂时先默认全部闭合
    // 要考虑有两个以上的polygon的情况
    if (!resultLines.get(0).p0.equals2D(resultLines.get(resultLines.size() - 1).p1, 1e-3)) {
      resultLines.add(
          new LineSegment(resultLines.get(resultLines.size() - 1).p1, resultLines.get(0).p0));
    }
    List<Coordinate> list = resultLines.stream().map(line -> line.p0).collect(Collectors.toList());
    list.add(resultLines.get(resultLines.size() - 1).p1);
    list.add(resultLines.get(0).p0);
    Coordinate[] coordinates = list.toArray(new Coordinate[] {});
    return XkPolygonUtil.createPolygon2d(coordinates);
  }

  Polygon test(List<LineSegment> list) {
    LineMerger lineMerger = new LineMerger();
    list.stream()
        .map(line -> line.toGeometry(XkGeometryFactory.geometryFactory))
        .forEach(lineMerger::add);
    List<LineString> lineStrings = (ArrayList) lineMerger.getMergedLineStrings();
    Map<Integer, Geometry> map = new HashMap<>();
    List<Polygon> polygonList = new ArrayList<>();
    // 首先要去重
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
    // Polygon polygon = polygonList.get(0);
    if (polygonList.size() == 1) {
      return polygonList.get(0);
    } else if (polygonList.size() > 1) {
      return polygonList.stream()
          .max(
              (a, b) -> {
                double v = a.getArea() - b.getArea();
                if (v > 0) {
                  return 1;
                } else if (v < 0) {
                  return -1;
                } else {
                  return 0;
                }
              })
          .get();
    } else if (polygonList.size() == 0) {
      return PolygonConstant.POLYGON_EMPTY;
    }

    // 以下代码暂时废弃

    // TODO 要合并linesegment
    // 用于保存所有点的坐标
    List<Coordinate> coordinates = new ArrayList<>();
    // 用点在coordinates中的 index 标识一个点，并给出所有直接相连的其他节点
    Map<Integer, Set<Integer>> route = new HashMap<>(list.size() * 2);
    list.stream()
        // 线段首尾不能太接近
        .filter(line -> !line.p0.equals2D(line.p1))
        .forEach(
            line -> {
              int index0 = -1;
              int index1 = -1;
              for (int i = 0; i < coordinates.size(); i++) {
                if (index0 == -1 && coordinates.get(i).equals2D(line.p0, 1e-3)) {
                  index0 = i;
                }
                if (index1 == -1 && coordinates.get(i).equals2D(line.p1, 1e-3)) {
                  index1 = i;
                }
                if (index0 != -1 && index1 != -1) {
                  break;
                }
              }
              if (index0 == -1) {
                index0 = coordinates.size();
                coordinates.add(line.p0);
              }
              if (index1 == -1) {
                index1 = coordinates.size();
                coordinates.add(line.p1);
              }

              Set<Integer> s1 = route.getOrDefault(index0, new HashSet<>());
              s1.add(index1);
              route.put(index0, s1);
              Set<Integer> s2 = route.getOrDefault(index1, new HashSet<>());
              s2.add(index0);
              route.put(index1, s2);
            });
    // 如果有的节点只通往一个其他节点，说明这个节点是孤立的（实际应该不可能出现这种情况）
    route.entrySet().stream()
        .filter(entry -> entry.getValue().size() <= 1)
        .forEach(
            entry -> {
              // 删除掉这个节点
              route.remove(entry.getKey());
              entry.getValue().forEach(p -> route.get(p).remove(entry.getKey()));
            });
    Polygon initialPolygon = findRoute(coordinates, route);
    return initialPolygon;
  }

  /**
   * 根据路径得到所有可能的闭合路径
   *
   * @param route key是节点的次序，value是可以直接到达的其他节点次序（用 integer 来标志节点）
   * @return
   */
  private Polygon findRoute(List<Coordinate> coordinates, Map<Integer, Set<Integer>> route) {
    List<List<Integer>> res = new ArrayList<>();
    // 初始选取一个节点
    int initial = (int) route.keySet().toArray()[0];
    // 找到其可以到达的其他节点
    // 从其他节点继续扫描下一个节点
    // ...
    // 直到回到初始节点
    // dfs
    // 先要找出一个 闭合的图形，然后围绕着这个闭合的图形一个个向外扩张

    List<Integer> list = new ArrayList<>();
    list.add(initial);
    final List<Integer> path = dfs(route, list);
    if (path.size() > 0) {
      path.add(path.get(0));
    }
    Coordinate[] initialCoordinates =
        path.stream().map(coordinates::get).toArray(Coordinate[]::new);

    // TODO 还有其他孤立的点，要重新取initial
    return XkPolygonUtil.createPolygon2d(initialCoordinates);
  }

  /**
   * 找到一条可以闭合的路
   *
   * @param route 所有联通的路径
   * @param path 已走的路，不能为空，至少经过一个节点
   * @return 返回闭合的路上所有的点（按顺序），如果不闭合则返回空
   */
  List<Integer> dfs(Map<Integer, Set<Integer>> route, List<Integer> path) {
    // 遍历下一步所有可能走的点
    for (Integer next : route.get(path.get(path.size() - 1))) {
      final int index = path.indexOf(next);
      // 不能走回头路
      if (index >= 0 && index == path.size() - 2) {
        continue;
      }
      // 已经走过 next 节点，说明形成了闭环，直接返回
      if (index != -1) {
        return path.subList(index, path.size() - 1);
      }
      // 否则继续寻找路径
      path.add(next);
      final List<Integer> nextPath = dfs(route, path);
      // 如果刚刚的这条路不行，则继续换下一个
      if (nextPath.isEmpty()) {
        path.remove(path.size() - 1);
      } else {
        return nextPath;
      }
    }
    // 无路可走了，返回空
    return Collections.emptyList();
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
    List<Double> area = new ArrayList<>();
    xkExtrudedGeometries.forEach(geometry -> area.add(geometry.getGeometry().getArea()));
    System.out.println("area: ");
    area.forEach(a -> System.out.print(a + " "));
    System.out.println();
    System.out.println("intersection area: ");
    xkExtrudedGeometries.stream()
        .skip(1)
        .forEach(
            a -> {
              int index = xkExtrudedGeometries.indexOf(a);

              System.out.print(
                  a.getGeometry()
                          .buffer(0)
                          .intersection(xkExtrudedGeometries.get(index - 1).getGeometry().buffer(0))
                          .getArea()
                      + " ");
            });

    System.out.println();
    // System.out.println(xkExtrudedGeometries.get(0).getGeometry());
    // Geometry dissolve = LineDissolver.dissolve(xkExtrudedGeometries.get(0).getGeometry());
    // List<LineSegment> lineSegments =
    //     XkLineSegmentUtil.generateLineSegmentListFromGeometry(dissolve);
    // LineString[] collect =
    //     lineSegments.stream()
    //         .map(line -> line.toGeometry(XkGeometryFactory.geometryFactory))
    //         .toArray(LineString[]::new);
    // // NOTE linemerger
    // GeometryCollection geometryCollection =
    //     new GeometryCollection(collect, XkGeometryFactory.geometryFactory);
    // LineMerger lineMerger = new LineMerger();
    // System.out.println(geometryCollection.getNumGeometries());
    // lineMerger.add(geometryCollection);
    // System.out.println(lineMerger.getMergedLineStrings());
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
