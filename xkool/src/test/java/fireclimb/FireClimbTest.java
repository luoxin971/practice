package fireclimb;

import com.xkool.algo.util.constant.GeometryConstant;
import com.xkool.algo.util.geometry.XkCoordinateUtil;
import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import com.xkool.algo.util.geometry.XkGeometryUtil;
import com.xkool.algo.util.plotter.XkPlotter;
import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.locationtech.jts.simplify.VWSimplifier;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * content
 *
 * @author luoxin
 * @since 2022/7/4
 */
public class FireClimbTest {

  @Test
  public void test() throws IOException, InterruptedException {
    BufferedReader reader =
        new BufferedReader(new FileReader("/Users/luoxin/Documents/temp/fireclimb/outlines.txt"));
    String line;
    List<Polygon> list = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
      Polygon polygon = (Polygon) XkGeometryIOUtil.fromGeoJson(line);
      list.add(polygon);
    }
    List<Integer> indexs = IntStream.range(0, 200).boxed().collect(Collectors.toList());
    // Arrays.asList(0, 4, 41, 51, 62, 77, 19, 50, 91, 100, 129, 151, 185, 160);
    Arrays.asList(90, 62);
    reader.close();
    List<FireClimb> fireClimbs = new ArrayList<>();
    indexs.forEach(
        i -> {
          Polygon x = list.get(i);
          FireClimb fireClimb = new FireClimb(x);
          FireClimbPlatformInfo handle = fireClimb.handle();
          fireClimbs.add(fireClimb);
          fireClimb.showAllOutcome(Collections.singletonList(handle), "Index: " + list.indexOf(x));
        });
    for (int i = 0; i < 10000000; i++) {
      Thread.sleep(1000);
    }
  }

  @Test
  public void solution() throws InterruptedException {
    List<Polygon> polygons = FireClimb.read();
    polygons.forEach(x -> new FireClimb(x).getFireClimbFaces());
    Thread.sleep(100000000);
  }

  @Test
  public void getAllBlankArea() {}

  @Test
  public void handleConnectedLine() {}

  @Test
  public void pointIntersectionGeometryAlongDirection() {}

  @Test
  public void getFragmentFromLinearRing() {
    IntStream.range(0, 100)
        .forEach(
            x -> {
              Polygon geometry =
                  // 随机生成一个 polygon
                  (Polygon)
                      XkCoordinateUtil.generateConvexHullOfCoordinateList(
                          IntStream.range(0, 10)
                              .mapToObj(
                                  y ->
                                      new Coordinate(
                                          new Random().nextInt(100), new Random().nextInt(100)))
                              .collect(Collectors.toList()));
              Coordinate[] coordinates = geometry.getCoordinates();
              // 随机选取一个点 c1
              int i1 = new Random().nextInt(coordinates.length - 1);
              LineSegment lineSegment = new LineSegment(coordinates[i1], coordinates[i1 + 1]);
              Coordinate c1 = lineSegment.pointAlong(new Random().nextDouble());
              // 再选一个 c2
              int i2 = new Random().nextInt(coordinates.length - 1);
              LineSegment l2 = new LineSegment(coordinates[i2], coordinates[i2 + 1]);
              Coordinate c2 = l2.pointAlong(new Random().nextDouble());
              // 调用
              LineString fragmentFromLinearRing =
                  new FireClimb(GeometryConstant.POLYGON_EMPTY)
                      .getFragmentFromLinearRing(geometry.getExteriorRing(), c1, c2);
              // 开头为 c1
              boolean s1 = fragmentFromLinearRing.getCoordinateN(0).distance(c1) < 1e-3;
              // 结尾为 c2
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

  @Test
  public void mergeLineStringsToPolygon() {}

  @Test
  public void generateLines() {}

  @Test
  public void testGetFragmentFromLinearRing() {}

  @Test
  public void t() {
    Geometry geometry = XkGeometryIOUtil.fromWkt(" LINESTRING (-18 7, 4 17, -1 5, 20 5)");
    Geometry buffer = geometry.buffer(10, -5);
    Geometry x = XkGeometryUtil.generateSingleSideBufferedGeometry(buffer, -5);
    System.out.println(x);
    BufferParameters bufferParameters = new BufferParameters();
    bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
    bufferParameters.setJoinStyle(BufferParameters.JOIN_MITRE);
    bufferParameters.setSingleSided(true);
    bufferParameters.setMitreLimit(1.0);
    BufferOp bufferOp = new BufferOp(geometry, bufferParameters);
    Geometry resultGeometry = bufferOp.getResultGeometry(10);
    System.out.println(buffer);
  }

  @Test
  public void simplify() throws InterruptedException {
    List<Polygon> read = FireClimb.read();
    read.forEach(
        polygon -> {
          System.out.println(read.indexOf(polygon));
          System.out.println(polygon);
          Geometry simplify = TopologyPreservingSimplifier.simplify(polygon, 0.1);
          Geometry simplify1 = DouglasPeuckerSimplifier.simplify(polygon, 0.1);
          Geometry simplify2 = VWSimplifier.simplify(polygon, 0.4);
          Geometry g3 = polygon.buffer(0.1).buffer(-0.1);
          System.out.println(simplify);
          System.out.println(simplify1);
          System.out.println(simplify2);
          System.out.println(g3);
          XkPlotter xkPlotter = new XkPlotter();
          xkPlotter.addContent(polygon, Color.red);
          // xkPlotter.addContent(simplify, Color.blue);
          xkPlotter.addContent(simplify1, Color.green);
          // xkPlotter.addContent(simplify3, Color.gray);
          xkPlotter.plot();
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
          System.out.println("-----");
        });
    Thread.sleep(1000000000);
  }

  @Test
  public void moveLineSegmentAlongVerticalDirection() {
    LineSegment lineSegment = new LineSegment(new Coordinate(0, 0), new Coordinate(1, 0));
    LineSegment ls =
        new FireClimb(GeometryConstant.POLYGON_EMPTY)
            .moveLineSegmentAlongVerticalDirection(lineSegment, 2);
    Assert.assertEquals(ls.distance(lineSegment), 2, 1e-3);
    Assert.assertEquals(ls.getCoordinate(0).getY() - lineSegment.getCoordinate(0).getY(), 2, 1e-3);
  }

  @Test
  public void ttt() {
    List<Integer> list = IntStream.range(0, 10).boxed().collect(Collectors.toList());
    handle(list);
    System.out.println(list);
  }

  public void handle(List<Integer> list) {
    list.remove(1);
    list.set(1, 90);
    list.add(22);
    list = new ArrayList<>();
  }
}
