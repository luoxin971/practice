package fireclimb;

import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import com.xkool.algo.util.plotter.PlotContent;
import com.xkool.algo.util.plotter.XkPlotter;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/25
 */
public class JTSBufferTest {
  public static void main(String[] args) {
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
}
