package ga.constant;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * content
 *
 * @author luoxin
 * @since 2022/5/31
 */
public class JtsConstant {
  public static final PrecisionModel PRECISION_MODEL_TWO_DIGIT = new PrecisionModel(100);
  public static final GeometryFactory GEOMETRY_FACTORY_TWO_DIGIT =
      new GeometryFactory(PRECISION_MODEL_TWO_DIGIT);
}
