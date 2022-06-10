package ga.util;

import ga.constant.JtsConstant;
import org.locationtech.jts.geom.*;

import java.util.List;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/31
 */
public class PointUtil {
  public static Point movePoint(Point point, double dx, double dy) {
    return point.getFactory().createPoint(new Coordinate(point.getX() + dx, point.getY() + dy));
  }

  public static Polygon createConvexHullFromPoints(List<Point> points) {
    GeometryCollection geometryCollection =
        JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createGeometryCollection(
            points.toArray(new Point[0]));
    Geometry hull = geometryCollection.convexHull();
    return (Polygon) hull;
  }
}
