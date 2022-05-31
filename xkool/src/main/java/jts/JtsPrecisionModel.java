package jts;

import com.xkool.algo.util.geometry.XkGeometryIOUtil;
import org.locationtech.jts.geom.*;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/30
 */
public class JtsPrecisionModel {
  public static void main(String[] args) {

    Polygon p =
        (Polygon)
            XkGeometryIOUtil.fromWkt(
                "POLYGON((-3.8 -6.825,-3.9 -6.825,-3.9 -6.725,-3.9 -6.075,-4.2 -6.075,-7.6 -6.075,-7.8 -6.075,-7.8 -5.875,-7.8 -2.525,-7.8 -0.825,-7.8 0.225,-7.8 1.475,-7.8 3.125,-7.7 3.125,-6.7 3.125,-6.7 6.625,-6.7 6.825,-6.5 6.825,-3.7 6.825,-1.4 6.825,-1.2 6.825,1.2 6.825,1.4 6.825,3.7 6.825,6.5 6.825,6.7 6.825,6.7 6.625,6.7 3.125,7.7 3.125,7.8 3.125,7.8 3.025,7.8 1.475,7.8 0.225,7.8 -0.825,7.8 -2.525,7.8 -5.875,7.8 -6.075,7.6 -6.075,4.2 -6.075,3.9 -6.075,3.9 -6.725,3.9 -6.825,3.8 -6.825,1.3 -6.825,1.2 -6.825,1.2 -6.725,1.2 -4.925,0.1 -4.925,-0.1 -4.925,-1.2 -4.925,-1.2 -6.725,-1.2 -6.825,-1.3 -6.825,-3.8 -6.825))");
    // 初始 precisionModel
    System.out.println(p.getPrecisionModel());
    // new precisionModel
    PrecisionModel precisionModel = new PrecisionModel(100);
    System.out.println(precisionModel);
    GeometryFactory geometryFactory = new GeometryFactory(precisionModel);
    CoordinateFilter filter = precisionModel::makePrecise;
    p.apply(filter);
    Geometry geometry = geometryFactory.createGeometry(p);
    // 初始 p
    System.out.println(p);
    // geometry after filter
    System.out.println(geometry);
    // precisionModel after filter，重新生成的 geometry 变了
    System.out.println(geometry.getPrecisionModel());
    // 并没有变化
    System.out.println(p.getPrecisionModel());
  }
}
