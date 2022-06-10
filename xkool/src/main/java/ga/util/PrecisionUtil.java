package ga.util;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * content
 *
 * @author luoxin
 * @since 2022/6/8
 */
public class PrecisionUtil {
  /**
   * 将 geometry 转换为特定精度
   *
   * @param geometry 待转换的geometry
   * @param precision 精度，具体可参考{@link PrecisionModel#PrecisionModel(double)}类注释中的 scale
   * @return 符合相应精度的 geometry
   */
  public static Geometry applyToPrecision(Geometry geometry, int precision) {
    PrecisionModel precisionModel = new PrecisionModel(precision);
    GeometryFactory geometryFactory = new GeometryFactory(precisionModel);

    return geometryFactory.createGeometry(geometry);
  }
}
