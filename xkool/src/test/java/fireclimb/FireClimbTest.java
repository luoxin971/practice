package fireclimb;

import com.xkool.algo.util.constant.GeometryConstant;
import com.xkool.algo.util.geometry.XkCoordinateUtil;
import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import com.xkool.algo.util.geometry.XkGeometryUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

import java.io.BufferedReader;
import java.io.FileReader;
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

  public List<Polygon> polygons;

  @Before
  public void setUp() throws Exception {
    BufferedReader reader =
        new BufferedReader(
            new FileReader("/Users/luoxin/Documents/xk_dev/practice/xkool/resources/outlines.txt"));
    String line;
    polygons = new ArrayList<>();
    while ((line = reader.readLine()) != null) {
      Polygon polygon = (Polygon) XkGeometryIOUtil.fromGeoJson(line);
      polygons.add(polygon);
    }
    reader.close();
  }

  @Test
  public void test() throws InterruptedException {

    List<Integer> indexs =
        // Arrays.asList(0, 4, 41, 51, 62, 77, 19, 50, 91, 100, 129, 151, 185, 160);
        IntStream.range(0, polygons.size()).boxed().collect(Collectors.toList());
    Arrays.asList(10);
    // Arrays.asList(119, 117, 115, 112, 109, 87, 74, 43, 15); // outline4
    // Arrays.asList(1, 5, 7, 12, 17, 20, 33, 34, 37, 40, 41); // outline3
    // Arrays.asList(19, 14, 13, 12, 11, 9, 6); // outline6

    List<FireClimb> fireClimbs = new ArrayList<>();

    indexs.forEach(
        i -> {
          Polygon x = polygons.get(i);
          FireClimb fireClimb = new FireClimb(x);
          FireClimbPlatformInfo handle = fireClimb.getDefaultPlatform();
          fireClimbs.add(fireClimb);
          fireClimb.showAllOutcome(
              Collections.singletonList(handle), "Index: " + polygons.indexOf(x));
        });
    for (int i = 0; i < 10000000; i++) {
      Thread.sleep(1000);
    }
  }

  @Test
  public void getFirePlatformWithStartPoint() {
    IntStream.range(0, polygons.size())
        .forEach(
            i -> {
              FireClimb fireClimb = new FireClimb(polygons.get(i));
              List<FireClimbPlatformInfo> allPlatform = fireClimb.getAllPlatforms();
              boolean b =
                  allPlatform.stream()
                      .allMatch(
                          x -> {
                            boolean b1 =
                                x.getPlatform()
                                        .symDifference(
                                            fireClimb
                                                .getFirePlatformWithStartPoint(
                                                    fireClimb.getBuffer5(),
                                                    fireClimb.getBuildingWidth(),
                                                    x.getLineSegments().get(0).p0,
                                                    false)
                                                .getPlatform())
                                        .getArea()
                                    < 1;
                            return b1;
                          });
              System.out.println(b);
            });
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
                  FireClimb.getFragmentFromLinearRing(geometry.getExteriorRing(), c1, c2, false);
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
  public void simplify() throws InterruptedException {}

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
    // XkJsonUtil.loads()

  }

  public void handle(List<Integer> list) {
    list.remove(1);
    list.set(1, 90);
    list.add(22);
    list = new ArrayList<>();
  }
}
