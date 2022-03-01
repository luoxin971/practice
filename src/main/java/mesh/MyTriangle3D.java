package mesh;

import com.google.common.math.DoubleMath;
import com.xkool.algo.util.geometry.XkGeometryFactory;
import com.xkool.algo.util.geometry.XkLineStringUtil;
import com.xkool.algo.util.geometry.XkPolygonUtil;
import lombok.Data;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * content
 *
 * @author luoxin
 * @since 2022/2/21
 */
@Data
public class MyTriangle3D {
  static final double TOLERANCE = 1e-3;

  Coordinate p0;
  Coordinate p1;
  Coordinate p2;

  double maxZ;
  double minZ;

  public MyTriangle3D(Coordinate p0, Coordinate p1, Coordinate p2) {
    this.p0 = p0;
    this.p1 = p1;
    this.p2 = p2;
    maxZ = Math.max(p0.getZ(), Math.max(p1.getZ(), p2.getZ()));
    minZ = Math.min(p0.getZ(), Math.min(p1.getZ(), p2.getZ()));
  }

  public List<Coordinate> getCoordinates() {
    return Arrays.asList(p0, p1, p2);
  }

  /** 是否与平面 Z=z 相交 */
  public boolean isIntersectWithZ(double z) {
    return z >= this.minZ && z <= this.maxZ;
  }

  /**
   * 获取三角形被平面 Z=z 所截的线段
   *
   * @return 不相交时，返回empty；全部在该平面上时，返回三条边
   */
  public List<LineSegment> cutByZ(double z) {
    if (!this.isIntersectWithZ(z)) {
      return Collections.emptyList();
    }
    List<Coordinate> greaterThanZ = new ArrayList<>();
    List<Coordinate> equalToZ = new ArrayList<>();
    List<Coordinate> lessThanZ = new ArrayList<>();
    for (Coordinate coordinate : this.getCoordinates()) {
      if (DoubleMath.fuzzyEquals(coordinate.getZ(), z, 1e-3)) {
        equalToZ.add(coordinate);
      } else if (coordinate.getZ() > z) {
        greaterThanZ.add(coordinate);
      } else {
        lessThanZ.add(coordinate);
      }
    }
    if (equalToZ.size() == 3) {
      return Arrays.asList(
          new LineSegment(p0, p1), new LineSegment(p1, p2), new LineSegment(p2, p1));
    } else if (equalToZ.size() == 2) {
      return Collections.singletonList(new LineSegment(equalToZ.get(0), equalToZ.get(1)));
    } else if (equalToZ.size() == 1) {
      if (greaterThanZ.size() * lessThanZ.size() == 0) {
        return Collections.emptyList();
      } else {
        return Collections.singletonList(
            new LineSegment(
                equalToZ.get(0), findCutPointInLine(greaterThanZ.get(0), lessThanZ.get(0), z)));
      }
    } else {
      List<Coordinate> list = new ArrayList<>();
      greaterThanZ.forEach(
          point1 -> lessThanZ.forEach(point2 -> list.add(findCutPointInLine(point1, point2, z))));
      return Collections.singletonList(new LineSegment(list.get(0), list.get(1)));
    }
  }

  /** 返回三条边与平面 Z=z 相交的点 */
  public List<Coordinate> findAllCutPoint(double z) {
    List<Coordinate> ans = new ArrayList<>();
    Coordinate c1 = findCutPointInLine(p0, p1, z);
    Coordinate c2 = findCutPointInLine(p0, p2, z);
    Coordinate c3 = findCutPointInLine(p1, p2, z);
    if (c1 != null) {
      ans.add(c1);
    }
    if (c2 != null) {
      ans.add(c2);
    }
    if (c3 != null) {
      ans.add(c3);
    }
    return ans;
  }

  /**
   * 获取线段 c1c2 被平面 Z=z 所截的点
   *
   * @return 无交点则返回null
   */
  public Coordinate findCutPointInLine(Coordinate c1, Coordinate c2, double z) {
    if ((z - c1.getZ()) * (z - c2.getZ()) > 0) {
      return null;
    }
    double[] direction =
        new double[] {c2.getX() - c1.getX(), c2.getY() - c1.getY(), c2.getZ() - c1.getZ()};
    double t = (z - c1.getZ()) / direction[2];
    return new Coordinate(c1.getX() + direction[0] * t, c1.getY() + direction[1] * t, z);
  }

  static void test() {
    LineString s1 =
        new LineSegment(new Coordinate(0, 0), new Coordinate(0, 1))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s2 =
        new LineSegment(new Coordinate(0, 1), new Coordinate(0, 2))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s3 =
        new LineSegment(new Coordinate(0, 2), new Coordinate(0, 3))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s4 =
        new LineSegment(new Coordinate(0, 2), new Coordinate(0, 1))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s5 =
        new LineSegment(new Coordinate(0, 3), new Coordinate(3, 1))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s6 =
        new LineSegment(new Coordinate(0, 0), new Coordinate(6, 0))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s7 =
        new LineSegment(new Coordinate(3, 1), new Coordinate(6, 0))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s8 =
        new LineSegment(new Coordinate(13, 11), new Coordinate(16, 10))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s9 =
        new LineSegment(new Coordinate(13, 11), new Coordinate(16, 20))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString s10 =
        new LineSegment(new Coordinate(16, 20), new Coordinate(16, 10))
            .toGeometry(XkGeometryFactory.geometryFactory);
    LineString[] ls = new LineString[] {s1, s2, s3, s4, s5, s6, s7, s8, s9, s10};
    GeometryCollection geometryCollection =
        new GeometryCollection(ls, XkGeometryFactory.geometryFactory);
    LineMerger lineMerger = new LineMerger();
    System.out.println(geometryCollection.getNumGeometries());
    lineMerger.add(geometryCollection);
    System.out.println(lineMerger.getMergedLineStrings());
  }

  static void test2() {
    LineString lineString =
        XkLineStringUtil.generateLineStringFromCoordinateArray(
            new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(0, 2)});
    LineString lineString1 =
        XkLineStringUtil.generateLineStringFromCoordinateArray(
            new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 2)});
    System.out.println(lineString1.getBoundary().buffer(0.1).contains(lineString.getBoundary()));
  }

  static void test3() {
    Polygon polygon2d =
        XkPolygonUtil.createPolygon2d(
            new Coordinate(0, 0),
            new Coordinate(0, 1),
            new Coordinate(1, 1),
            new Coordinate(1, 0),
            new Coordinate(0, 0));
    Polygon polygon =
        XkPolygonUtil.createPolygon2d(
            new Coordinate(0, 0),
            new Coordinate(0, 2),
            new Coordinate(1, 1),
            new Coordinate(1, 0),
            new Coordinate(0, 0));
    MultiPolygon multiPolygon =
        new MultiPolygon(new Polygon[] {polygon, polygon2d}, XkGeometryFactory.geometryFactory);
    System.out.println(multiPolygon);
  }

  public static void main(String[] args) {
    // test();
    // test2();
    test3();
  }
}
