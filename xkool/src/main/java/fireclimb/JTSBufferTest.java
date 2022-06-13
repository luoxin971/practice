package fireclimb;

import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import com.xkool.algo.util.plotter.PlotContent;
import com.xkool.algo.util.plotter.XkPlotter;
import ga.constant.JtsConstant;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/25
 */
public class JTSBufferTest {
  public static void main(String[] args) {
    if (1 == 1) {
      test();
      return;
    }

    Polygon p =
        (Polygon)
            XkGeometryIOUtil.fromWkt(
                "POLYGON((-3.8 -6.825,-3.9 -6.825,-3.9 -6.725,-3.9 -6.075,-4.2 -6.075,-7.6 -6.075,-7.8 -6.075,-7.8 -5.875,-7.8 -2.525,-7.8 -0.825,-7.8 0.225,-7.8 1.475,-7.8 3.125,-7.7 3.125,-6.7 3.125,-6.7 6.625,-6.7 6.825,-6.5 6.825,-3.7 6.825,-1.4 6.825,-1.2 6.825,1.2 6.825,1.4 6.825,3.7 6.825,6.5 6.825,6.7 6.825,6.7 6.625,6.7 3.125,7.7 3.125,7.8 3.125,7.8 3.025,7.8 1.475,7.8 0.225,7.8 -0.825,7.8 -2.525,7.8 -5.875,7.8 -6.075,7.6 -6.075,4.2 -6.075,3.9 -6.075,3.9 -6.725,3.9 -6.825,3.8 -6.825,1.3 -6.825,1.2 -6.825,1.2 -6.725,1.2 -4.925,0.1 -4.925,-0.1 -4.925,-1.2 -4.925,-1.2 -6.725,-1.2 -6.825,-1.3 -6.825,-3.8 -6.825))");

    PrecisionModel precisionModel = new PrecisionModel(100);
    GeometryFactory geometryFactory = new GeometryFactory(precisionModel);
    CoordinateFilter filter = precisionModel::makePrecise;
    p.apply(filter);
    Geometry geometry = geometryFactory.createGeometry(p);
    List<PlotContent> list = new ArrayList<>();
    list.add(new PlotContent(p, Color.red));
    // list.add(new PlotContent(p.buffer(5), Color.red));
    // when quadrantSegments < 0, joinStyle = JOIN_MITRE
    // buffer 的各项参数含义区别，可参考 https://postgis.net/docs/ST_Buffer.html
    // quadrantSegments 设置很大貌似也没多大意义，取个 10 就 ok 了（设为 1 确实会有区别） ？
    list.add(new PlotContent(p.buffer(5, -10), Color.pink));
    list.add(new PlotContent(p.buffer(5, -5), Color.pink));
    list.add(new PlotContent(p.buffer(5, -1), Color.blue));
    // distance 为负，则 buffer 反向变了
    list.add(new PlotContent(p.buffer(-2, -5), Color.orange));
    list.add(new PlotContent(p.buffer(-5, -5), Color.orange));
    XkPlotter xkPlotter = new XkPlotter();
    list.forEach(xkPlotter::addContent);
    xkPlotter.plot();
    System.out.println("---");
    System.out.println(p);
    System.out.println(p.buffer(-5, -5));
    System.out.println(p.buffer(-5, -5).buffer(10, -5));
    Geometry buffer = p.buffer(-5, -5).buffer(10, -5);
    buffer.apply(filter);
    System.out.println(buffer);
    System.out.println("---");
    System.out.println(geometry);
    System.out.println(geometry.buffer(-5, -5));
    System.out.println(geometry.buffer(-5, -5).buffer(10, -5));
  }

  public static void test() {
    Geometry g = XkGeometryIOUtil.fromWkt("POLYGON ((-2 0, 2 0, 2 2, 0 1, -2 2, -2 0))");
    Geometry buffer = g.buffer(1, -100);
    System.out.println(g);
    System.out.println(buffer);
    Geometry g2 = XkGeometryIOUtil.fromWkt("POLYGON ((0 0, 2 0, 2 2, 1 0.3, 0 2, 0 0))");
    Geometry buffer2 = g2.buffer(1, -100);
    Geometry bb = buffer2.buffer(10, -100);
    System.out.println(g2);
    System.out.println(buffer2);
    System.out.println(bb);
    Coordinate[] coordinates =
        Arrays.stream(g2.getCoordinates())
            .map(c -> new Coordinate(1.5 * c.getX(), 1.5 * c.getY()))
            .toArray(Coordinate[]::new);
    Geometry g3 = JtsConstant.GEOMETRY_FACTORY_TWO_DIGIT.createPolygon(coordinates);
    Geometry buffer3 = g3.buffer(1, -100);
    Geometry bb3 = buffer3.buffer(1, -100);
    System.out.println(g3);
    System.out.println(buffer3);

    System.out.println(bb3);
  }
}
