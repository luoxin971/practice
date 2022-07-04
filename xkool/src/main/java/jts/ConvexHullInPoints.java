package jts;

import com.xkool.algo.util.plotter.XkPlotter;
import ga.constant.JtsConstant;
import ga.util.PointUtil;
import org.apache.commons.lang.math.RandomUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/31
 */
public class ConvexHullInPoints {
  public static void main(String[] args) {
    Point point = JtsConstant.GEOMETRY_FACTORY_FLOATING.createPoint(new Coordinate(0, 0));
    List<Point> list = new ArrayList<>();
    IntStream.range(0, 30)
        .forEach(
            i ->
                list.add(
                    PointUtil.movePoint(
                        point,
                        ((double) RandomUtils.nextInt(3000)) / 100.0,
                        ((double) RandomUtils.nextInt(3000)) / 100.0)));
    GeometryCollection geometryCollection =
        JtsConstant.GEOMETRY_FACTORY_FLOATING.createGeometryCollection(list.toArray(new Point[0]));
    Geometry hull = geometryCollection.convexHull();
    list.forEach(System.out::println);
    System.out.println(hull);
    XkPlotter xkPlotter = new XkPlotter();
    list.forEach(p -> xkPlotter.addContent(p, Color.black));
    xkPlotter.addContent(hull, Color.pink);
    xkPlotter.plot();
  }
}
