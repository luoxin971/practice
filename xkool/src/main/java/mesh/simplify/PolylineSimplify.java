package mesh.simplify;

import com.goebl.simplify.PointExtractor;
import com.goebl.simplify.Simplify;

/**
 * https://github.com/hgoebl/simplify-java
 *
 * <p>貌似是简化折线的
 *
 * @author luoxin
 * @since 2022/5/10
 */
public class PolylineSimplify {

  public static void main(String[] args) {
    LatLng[] coords =
        new LatLng[] {
          new LatLng(-1d, 1d),
          new LatLng(0, 0.98),
          new LatLng(1, 1),
          new LatLng(1, 0),
          new LatLng(-1, 0)
        };
    Simplify<LatLng> simplify = new Simplify<>(new LatLng[0], latLngPointExtractor);

    LatLng[] simplified = simplify.simplify(coords, 0.1, false);
    System.out.println(simplified);
  }

  private static PointExtractor<LatLng> latLngPointExtractor =
      new PointExtractor<LatLng>() {
        @Override
        public double getX(LatLng point) {
          return point.getLat();
        }

        @Override
        public double getY(LatLng point) {
          return point.getLng();
        }
      };

  public static class LatLng {
    private final double lat;
    private final double lng;

    public LatLng(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
    }

    public double getLat() {
      return lat;
    }

    public double getLng() {
      return lng;
    }
  }
}
