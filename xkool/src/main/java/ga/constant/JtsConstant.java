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

  public static final PrecisionModel PRECISION_MODEL_FLOATING =
      new PrecisionModel(PrecisionModel.FLOATING);
  public static final GeometryFactory GEOMETRY_FACTORY_FLOATING =
      new GeometryFactory(PRECISION_MODEL_FLOATING);

  public static final PrecisionModel PRECISION_MODEL_TWO_DIGIT = new PrecisionModel(1000);
  public static final GeometryFactory GEOMETRY_FACTORY_TWO_DIGIT =
      new GeometryFactory(PRECISION_MODEL_TWO_DIGIT);
}
