package fireclimb;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

/**
 * content
 *
 * @author: xin
 * @since: 2022/7/18
 */
@Data
@AllArgsConstructor
public class FireClimbPlatformInfo {
  public Polygon platform;
  public List<LineSegment> lineSegments;
}
