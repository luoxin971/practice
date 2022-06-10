package mesh;

import com.xkool.algo.util.geometry.XkPolygonUtil;
import com.xkool.algo.util.plotter.XkPlotter;
import org.locationtech.jts.geom.Geometry;

import java.awt.*;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/17
 */
public class PlotGeo {
  public static void main(String[] args) {
    test();
  }

  static void test() {
    XkPlotter xkPlotter = new XkPlotter();
    Geometry g1 = XkPolygonUtil.createPolygon2d(0, 0, 1, 1, 2, 0, 0, 0);
    Geometry g2 = XkPolygonUtil.createPolygon2d(10, 10, 11, 11, 12, 10, 10, 10);
    xkPlotter.addContent(g1, Color.blue);
    xkPlotter.plot();
    xkPlotter.clearContent();

    xkPlotter.addContent(g2, Color.blue);
    xkPlotter.plot();
  }
}
