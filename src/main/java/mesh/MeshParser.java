package mesh;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.math.DoubleMath;
import com.xkool.algo.util.geometry.XkGeometryFactory;
import com.xkool.algo.util.geometry.XkPolygonUtil;
import com.xkool.xkcommon.model.base.XkExtrudedGeometry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeshParser {

  List<MyTriangle3D> triangles = new ArrayList<>();

  public List<XkExtrudedGeometry> parse() {
    Stopwatch timer = Stopwatch.createStarted();
    List<Set<MyTriangle3D>> buildings = this.blockSplit();
    List<XkExtrudedGeometry> res =
        buildings.parallelStream()
            .map(ArrayList::new)
            .flatMap(list -> this.transformToBuild(list).stream())
            .collect(Collectors.toList());
    res.forEach(
        xkExtrudedGeometry -> {
          System.out.println(xkExtrudedGeometry.getGeometry());
          System.out.println(
              "elevation:"
                  + xkExtrudedGeometry.getElevation()
                  + " high: "
                  + xkExtrudedGeometry.getSelfHeight());
        });
    System.out.println(timer.stop());
    return res;
  }

  public List<Set<MyTriangle3D>> blockSplit() {
    List<Set<MyTriangle3D>> sets = blockSplit(this.triangles);
    return sets;
  }

  /**
   * 将基地内所有的 triangle 根据所在 building 分组，基本步骤如下
   *
   * <p>building 划分的依据是，两栋 building 中的 triangles 不共边，不共点
   *
   * <p>预处理：找出所有的点，以及包含该点的 triangles
   *
   * <ol>
   *   主要步骤：
   *   <li>去除平面 Z=0 上的所有三角形，防止基地本身的 triangle 产生影响
   *   <li>随机找一个没有遍历过的点，根据这个点，找出这个点所在的 building 中的所有 triangle
   *   <li>重复第 2 步，直到所有点都遍历完了
   * </ol>
   *
   * <ol>
   *   该方法要求：
   *   <li>基地是平的，否则整个基地将只有一个 building
   *   <li>各点之间的 equal 不存在 double 导致的精度问题
   * </ol>
   *
   * @param triangleList 基地内所有的 triangle
   * @return 每栋建筑内所有的 triangles
   */
  public static List<Set<MyTriangle3D>> blockSplit(List<MyTriangle3D> triangleList) {
    // 去掉平面 Z=0 上的 triangles
    List<MyTriangle3D> list =
        triangleList.stream()
            .filter(tri -> !DoubleMath.fuzzyEquals(tri.getMaxZ(), 0, 1e-3))
            .collect(Collectors.toList());
    // 存所有的 coordinates
    Map<Coordinate, List<MyTriangle3D>> map = new ConcurrentHashMap<>();
    list.stream()
        .parallel()
        .forEach(
            myTriangle3D ->
                myTriangle3D
                    .getCoordinates()
                    .forEach(
                        p -> {
                          List<MyTriangle3D> value = map.getOrDefault(p, new ArrayList<>());
                          value.add(myTriangle3D);
                          map.put(p, value);
                        }));
    // 随机找出一点，根据这一点所在的所有 triangle 逐渐向外扩散，并记录这些 triangle 所包含的点
    // 再由这些点，通过 map，找到所有的 triangle，组合在一起就是一个 building

    // 每栋建筑所包含的 coordinate
    List<Set<Coordinate>> coordinateInBuildingList = new ArrayList<>();
    // 已遍历的所有的 coordinates
    Set<Coordinate> coordinatesAccessed = new HashSet<>();
    // 大循环，将所有 coordinates 按所在的 building 进行分组，放入 coordinateInBuildingList
    while (coordinatesAccessed.size() != map.size()) {
      // 选取一个还没遍历的点
      Coordinate first =
          map.keySet().stream()
              .filter(coordinate -> !coordinatesAccessed.contains(coordinate))
              .findAny()
              .orElse(null);
      // 如果为空，则表明全部遍历完，理论上不可能为空，除非有 bug
      if (Objects.isNull(first)) {
        break;
      }
      // 小循环，找出点 first 所在的 building 中的所有 coordinates
      // 每一轮需要遍历的 coordinate curSet
      Set<Coordinate> curSet = new HashSet<>();
      // 当前 building 中的所有 coordinates
      Set<Coordinate> coordinatesInBuilding = new HashSet<>();
      curSet.add(first);
      coordinatesInBuilding.add(first);

      // 从 curSet 扩散出的下一轮 coordinate
      // 为空说明已经遍历完了
      while (!curSet.isEmpty()) {
        curSet =
            curSet.parallelStream()
                .flatMap(
                    coordinate ->
                        map.get(coordinate).stream()
                            .flatMap(
                                triangle ->
                                    Objects.isNull(triangle)
                                        ? Stream.empty()
                                        : triangle.getCoordinates().stream()))
                .distinct()
                .filter(coordinate -> !coordinatesInBuilding.contains(coordinate))
                .collect(Collectors.toSet());
        coordinatesInBuilding.addAll(curSet);
      }
      coordinatesAccessed.addAll(coordinatesInBuilding);
      coordinateInBuildingList.add(coordinatesInBuilding);
    }
    return coordinateInBuildingList.stream()
        .map(
            set ->
                set.stream()
                    .flatMap(coordinate -> map.get(coordinate).stream())
                    // 不清楚哪里出现了 null 值
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()))
        .collect(Collectors.toList());
  }

  /**
   * 将三角形构建成 building
   *
   * <p>从z=0开始，找到所有的边，构建成底座
   *
   * <p>根据 triangle 上点的 z 值确定每次截取的 z 坐标
   *
   * <p>指定 z 值后，截取三角形，找出所有线段，闭合成一个 MultiPolygon
   *
   * <p>与前一个 MultiPolygon 做一个比较，如果差别较大则，继续细分，否则下一步
   *
   * <p>加上离地高度，自身高度
   *
   * <p>或许可以考虑根据正视图和侧视图大概确认其形状，防止一些误差或者意外情况
   */
  protected List<XkExtrudedGeometry> transformToBuild(List<MyTriangle3D> allTriangles) {
    // 为了计算方便，triangleList 要去掉与平面 Z=0 平行的三角形（去掉这些三角形，其实并不影响，因为这三个边是肯定会在其他三角形中用的
    final List<MyTriangle3D> triangleList =
        allTriangles.stream()
            // 是否加上精度要看实际情况如何
            .filter(triangle -> Objects.nonNull(triangle) && triangle.maxZ != triangle.minZ)
            .collect(Collectors.toList());
    // 找出所有点的z坐标，注意要去重和排序
    // TODO 考虑直接在一开始就将距离较近的点过滤，省的参与排序
    List<Double> zValues =
        triangleList.stream()
            .flatMap(triangle -> triangle.getCoordinates().stream().map(Coordinate::getZ))
            .distinct()
            .sorted()
            .collect(Collectors.toList());

    // 用 z=0.1 所截的 polygon 近似替代底座
    // FIXME 现在感觉去掉了平行的三角形后，z=0 也可以哈，之后再回来看
    MultiPolygon previousPolygon = this.getGeometryByZ(triangleList, 0.1);
    List<XkExtrudedGeometry> res =
        new ArrayList<>(this.generateXkExtrudedGeometry(previousPolygon, 0.1, 0));
    MultiPolygon currentPolygon;
    double previousCut = 0.1;
    double currentCut;
    // 逐个遍历 z 值，进行截断
    for (Double z : zValues) {
      // 初始每次取 z 值都至少间隔 1，不需要取值太密集
      if (z - previousCut < 1) {
        continue;
      }
      currentCut = z;
      currentPolygon = this.getGeometryByZ(triangleList, currentCut);
      // 是否需要细分
      if (!this.isNeedDivided(previousCut, currentCut, previousPolygon, currentPolygon)) {
        res.addAll(
            this.generateXkExtrudedGeometry(
                previousPolygon, currentCut - previousCut, previousCut));
      } else {
        // 如果差别大，则要继续细分
        res.addAll(
            this.divide(previousCut, currentCut, previousPolygon, currentPolygon, triangleList));
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
  private List<XkExtrudedGeometry> generateXkExtrudedGeometry(
      MultiPolygon multiPolygon, double height, double elevation) {
    return IntStream.range(0, multiPolygon.getNumGeometries())
        .mapToObj(
            i -> new XkExtrudedGeometry((Polygon) multiPolygon.getGeometryN(i), height, elevation))
        .collect(Collectors.toList());
  }

  /** 判断是否需要继续细分 */
  protected boolean isNeedDivided(
      double low, double high, Geometry lowGeometry, Geometry highGeometry) {
    // 如果间距较小，就不要细分了
    if (high - low < 1.5) {
      return false;
    }
    double lowArea = lowGeometry.getArea();
    double highArea = highGeometry.getArea();
    Geometry lowGeometry2 = lowGeometry.buffer(0).intersection(lowGeometry.buffer(0));
    Geometry highGeometry2 = highGeometry.buffer(0).intersection(highGeometry.buffer(0));
    double intersectionArea = lowGeometry2.intersection(highGeometry2).getArea();
    return intersectionArea / lowArea < 0.8 || intersectionArea / highArea < 0.8;
  }

  /** 获取 g1, g2 的交集的面积 */
  double getIntersectionArea(Geometry g1, Geometry g2) {
    Geometry buffer1 = g1.buffer(0);
    Geometry buffer2 = g2.buffer(0);
    Geometry geo1 = buffer1.intersection(buffer1);
    Geometry geo2 = buffer2.intersection(buffer2);
    return geo1.intersection(geo2).getArea();
  }

  /**
   * 切割策略，在 z 取值为 low, high 之中对 triangleList 进行划分
   *
   * <p>目前采取的策略是
   *
   * <p>先判断是否是平面做拉伸的（如果上下 geometry 相交的面积占比很大，则认为是拉伸的）
   *
   * <p>如果是拉伸的，直接取被拉伸的平面为 geometry，高度差为 selfHigh
   *
   * <p>否则就是斜面的，那就以一定间距慢慢切割
   */
  protected List<XkExtrudedGeometry> divide(
      double low,
      double high,
      MultiPolygon lowGeometry,
      MultiPolygon highGeometry,
      List<MyTriangle3D> triangleList) {
    // 取中点
    double mid = low + (high - low) / 2;
    final MultiPolygon midGeometry = this.getGeometryByZ(triangleList, mid);

    // 与上下的geometry相近时，认为是拉伸的
    if (this.getIntersectionArea(midGeometry, lowGeometry) / lowGeometry.getArea() > 0.95) {
      return this.generateXkExtrudedGeometry(lowGeometry, high - low, low);
    } else if (this.getIntersectionArea(midGeometry, highGeometry) / highGeometry.getArea()
        > 0.95) {
      return this.generateXkExtrudedGeometry(highGeometry, high - low, low);
    }
    // 否则认为是一个斜面，渐变的
    List<Double> zValues = this.getCutZValues(low, high);
    List<MultiPolygon> multiPolygons =
        zValues.parallelStream()
            .map(z -> this.getGeometryByZ(triangleList, z))
            .collect(Collectors.toList());
    int size = zValues.size();

    List<XkExtrudedGeometry> res =
        this.generateXkExtrudedGeometry(lowGeometry, zValues.get(0) - low, low);
    res.addAll(
        IntStream.range(0, size - 1)
            .parallel()
            .mapToObj(
                x ->
                    this.generateXkExtrudedGeometry(
                        multiPolygons.get(x), zValues.get(x + 1) - zValues.get(x), zValues.get(x)))
            .flatMap(Collection::stream)
            .collect(Collectors.toList()));
    Double lastZ = zValues.get(size - 1);
    res.addAll(
        this.generateXkExtrudedGeometry(
            multiPolygons.get(multiPolygons.size() - 1), high - lastZ, lastZ));
    return res;
  }

  /** 获取从 low 到 high 中要切割的 z值 */
  List<Double> getCutZValues(double low, double high) {
    List<Double> res = new ArrayList<>();
    for (double z = low + 1; z < high; ++z) {
      res.add(z);
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
   * 获取 Z=z 的切面
   *
   * @param triangleList 要求所有三角形不能与平面 Z=z 平行
   * @param z 平面 Z=z
   * @return 返回所有线段连成的 polygon
   */
  protected MultiPolygon getGeometryByZ(List<MyTriangle3D> triangleList, double z) {
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
    return this.transformLinesToMultiPolygon(intersectLines);
    // 以下代码暂时废弃
  }

  /** 将 lineSegment 转换成 multiPolygon TODO 算法需要优化 */
  protected MultiPolygon transformLinesToMultiPolygon(List<LineSegment> list) {
    LineMerger lineMerger = new LineMerger();
    list.stream()
        .map(line -> line.toGeometry(XkGeometryFactory.geometryFactory))
        .forEach(lineMerger::add);
    List<LineString> lineStrings = (ArrayList<LineString>) lineMerger.getMergedLineStrings();
    List<Polygon> polygonList = new ArrayList<>();
    // 将 lineString 分为自闭合和非闭合的两组
    // 看看 isClosed 和 isRing 的区别
    Map<Boolean, List<LineString>> partition =
        lineStrings.stream().collect(Collectors.partitioningBy(LineString::isClosed));
    // 自闭合的直接生成 polygon
    partition
        .get(true)
        .forEach(line -> polygonList.add(XkPolygonUtil.createPolygon2d(line.getCoordinates())));
    // 不闭合的去找另一半
    // FIXME 要考虑是否会有多段 lineString 才生成一个 ring 的情况，之后碰到 bug 再解决
    final List<LineString> unclosed = partition.get(false);
    List<Pair<Coordinate, Coordinate>> coordinatePairList = new ArrayList<>();
    for (LineString ls : unclosed) {
      // ls 的首尾
      Coordinate lastCoordinate = ls.getCoordinateN(ls.getNumPoints() - 1);
      Coordinate firstCoordinate = ls.getCoordinateN(0);
      if (coordinatePairList.stream()
          .noneMatch(
              pair ->
                  this.isTwoCoordinateEqual(pair, Pair.create(firstCoordinate, lastCoordinate)))) {

        coordinatePairList.add(Pair.create(firstCoordinate, lastCoordinate));
        List<Polygon> iPolygons = new ArrayList<>();
        for (int i = unclosed.indexOf(ls) + 1; i < unclosed.size(); i++) {
          // unclosed.get(i) 的首尾
          Coordinate first = unclosed.get(i).getCoordinateN(0);
          Coordinate last = unclosed.get(i).getCoordinateN(unclosed.get(i).getNumPoints() - 1);
          // 两条 lineString 首尾点是否相同，相同就可以闭合
          if (firstCoordinate.equals2D(first) && lastCoordinate.equals2D(last)) {
            Coordinate[] coordinates =
                Stream.concat(
                        Arrays.stream(ls.getCoordinates()),
                        Arrays.stream(unclosed.get(i).reverse().getCoordinates()).skip(1))
                    .toArray(Coordinate[]::new);
            iPolygons.add(XkPolygonUtil.createPolygon2d(coordinates));
          } else if (firstCoordinate.equals2D(last) && lastCoordinate.equals2D(first)) {
            Coordinate[] coordinates =
                Stream.concat(
                        Arrays.stream(ls.getCoordinates()),
                        Arrays.stream(unclosed.get(i).getCoordinates()).skip(1))
                    .toArray(Coordinate[]::new);
            iPolygons.add(XkPolygonUtil.createPolygon2d(coordinates));
          }
        }
        if (iPolygons.size() > 1) {
          polygonList.add((Polygon) CascadedPolygonUnion.union(iPolygons));
        } else {
          polygonList.addAll(iPolygons);
        }
      }
    }

    return new MultiPolygon(
        polygonList.toArray(new Polygon[] {}), XkGeometryFactory.geometryFactory);

    // 以下代码暂时废弃
  }

  /** 判断两个 pair 里的 coordinate 是否 equal，不考虑顺序 */
  boolean isTwoCoordinateEqual(Pair<Coordinate, Coordinate> p1, Pair<Coordinate, Coordinate> p2) {
    return p1.getFirst().equals2D(p2.getFirst(), 1e-3)
            && p1.getSecond().equals2D(p2.getSecond(), 1e-3)
        || p1.getFirst().equals2D(p2.getSecond(), 1e-3)
            && p1.getSecond().equals2D(p2.getFirst(), 1e-3);
  }

  static void testParse() {
    MeshParser meshParser = new MeshParser(MeshParser.generate());
    while (true) {
      List<XkExtrudedGeometry> parse = meshParser.parse();
      System.out.println(parse);
    }
  }

  static void testBlockSplit() {
    List<MyTriangle3D> triangle3DS = MeshParser.generate();
    MeshParser meshParser = new MeshParser(triangle3DS);
    List<Set<MyTriangle3D>> sets = meshParser.blockSplit(triangle3DS);
    // 打断点查看结果
    System.out.println(sets);
  }

  static void testTransformToBuilding() {
    Stopwatch timer = Stopwatch.createStarted();
    List<MyTriangle3D> allTriangles = MeshParser.generate();
    MeshParser meshParser = new MeshParser(allTriangles);
    List<XkExtrudedGeometry> xkExtrudedGeometries = meshParser.transformToBuild(allTriangles);
    System.out.println(timer.stop());
    xkExtrudedGeometries.forEach(
        xkExtrudedGeometry -> {
          System.out.println(xkExtrudedGeometry.getGeometry());
          System.out.printf(
              "low: %f, high: %f%n",
              xkExtrudedGeometry.getElevation(),
              xkExtrudedGeometry.getElevation() + xkExtrudedGeometry.getSelfHeight());
        });

    System.out.println();
  }

  public static void main(String[] args) throws InterruptedException {
    // MeshParse.testBlockSplit();
    // MeshParse.testTransformToBuilding();
    MeshParser.testParse();
    Thread.sleep(100000000);
  }

  static List<MyTriangle3D> generate() {

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
    triangle3DList.addAll(MeshParser.generateTri());
    return triangle3DList;
  }

  static List<MyTriangle3D> generateTri() {
    List<Coordinate> coordinateList =
        Lists.partition(MeshParser.generateDouble(), 3).stream()
            .map(list -> new Coordinate(list.get(0), list.get(1), list.get(2)))
            .collect(Collectors.toList());
    List<MyTriangle3D> triangle3DList3 =
        Lists.partition(MeshParser.generateInt(), 3).stream()
            .map(
                list ->
                    new MyTriangle3D(
                        coordinateList.get(list.get(0)),
                        coordinateList.get(list.get(1)),
                        coordinateList.get(list.get(2))))
            .collect(Collectors.toList());
    return triangle3DList3;
  }

  static List<Integer> generateInt() {
    return Arrays.asList(
        0, 1, 2, 1, 0, 3, 4, 5, 6, 5, 4, 7, 8, 9, 10, 9, 8, 11, 12, 13, 14, 13, 12, 15, 16, 17, 18,
        17, 16, 19, 20, 21, 22, 21, 20, 23);
  }

  static List<Double> generateDouble() {
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
