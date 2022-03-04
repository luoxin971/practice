package mesh;

import com.xkool.algo.util.constant.PolygonConstant;
import com.xkool.algo.util.geometry.XkLineStringUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * content
 *
 * @author luoxin
 * @since 2022/3/3
 */
public class MockData {
  
  static Map<Coordinate, List<LineString>> getMap(List<LineString> ls) {
    Map<Coordinate, List<LineString>> map = new HashMap<>(ls.size() * 2);
    ls.forEach(
        lineString -> {
          // 起点
          Coordinate startCoordinate = lineString.getCoordinateN(0);
          List<LineString> ls1 = map.getOrDefault(startCoordinate, new ArrayList<>());
          if(ls1.stream().noneMatch(line -> line.equalsExact(lineString, 1e-3) || line.reverse().equalsExact(lineString, 1e-3))) {
            ls1.add(lineString);
            map.put(startCoordinate, ls1);
          }
          // 终点
          Coordinate endCoordinate = lineString.getCoordinateN(lineString.getNumPoints() - 1);
          List<LineString> ls2 = map.getOrDefault(endCoordinate, new ArrayList<>());
          if(ls2.stream().noneMatch(line -> line.equalsExact(lineString, 1e-3) || line.reverse().equalsExact(lineString, 1e-3))) {
            ls2.add(lineString);
            map.put(endCoordinate, ls2);
          }
        });
    return map;
  }
  
  
  public static void main(String[] args) throws ParseException {
    List<LineString> lineStrings = MockData.generateLineStrings();
    LineMerger lineMerger = new LineMerger();
    lineStrings.forEach(lineMerger::add);
    List<LineString> firstMergedLineStrings = (ArrayList<LineString>) lineMerger.getMergedLineStrings();
    Map<Coordinate, List<LineString>> map = getMap(firstMergedLineStrings);
    
    LineMerger m2 = new LineMerger();
    Set<LineString> aaa = new HashSet<>();
  
    Optional<Map.Entry<Coordinate, List<LineString>>> any = map.entrySet().stream().filter(entry -> entry.getValue().size() == 2).findAny();
    while(any.isPresent()) {
      List<LineString> temp = XkLineStringUtil.mergeLines(any.get().getValue());
      Coordinate toUpdate1 = any.get().getValue().get(0).getStartPoint().getCoordinate().equals(any.get().getKey()) ? any.get().getValue().get(0).getEndPoint().getCoordinate() : any.get().getValue().get(0).getStartPoint().getCoordinate();
      Coordinate toUpdate2 = any.get().getValue().get(1).getStartPoint().getCoordinate().equals(any.get().getKey()) ? any.get().getValue().get(1).getEndPoint().getCoordinate() : any.get().getValue().get(1).getStartPoint().getCoordinate();
      map.get(toUpdate1).remove(any.get().getValue().get(0));
      map.get(toUpdate1).add(temp.get(0));
      map.get(toUpdate2).remove(any.get().getValue().get(1));
      map.get(toUpdate2).add(temp.get(0));
      map.put(any.get().getKey(), new ArrayList<>());
      any = map.entrySet().stream().filter(entry -> entry.getValue().size() == 2).findAny();
    }
    List<LineString> afterList = map.values().stream().flatMap(Collection::stream).distinct().collect(Collectors.toList());
    Polygon polygon = PolygonConstant.POLYGON_EMPTY;
    LineString l = afterList.get(0);
    
    Map<Coordinate, List<LineString>> map1 = getMap(afterList);
    System.out.println(map1);
    // map.values().stream()
    //     .flatMap(
    //         list -> {
    //           if (list.size() == 2) {
    //             List<LineString> temp = XkLineStringUtil.mergeLines(list);
    //             aaa.addAll(temp);
    //             if (temp.size() > 1) {
    //               System.out.println("fuck");
    //             }
    //             return temp.stream();
    //           } else {
    //             aaa.addAll(list);
    //             return list.stream();
    //           }
    //         })
    //     .forEach(m2::add);
    System.out.println(aaa);
    System.out.println(m2.getMergedLineStrings());
  }
  
  public static List<LineString> generateLineStrings() throws ParseException {
    List<LineString> ans = new ArrayList<>();
    WKTReader wktReader = new WKTReader();
    ans.add((LineString) wktReader.read("LINESTRING( 3680.3810217 3387.9669413, 3680.3810217 3303.321287)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3680.3810217 3387.9669413, 3680.3810217 3303.321287)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3506.0771869, 3522.9007068 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3841.7983502 3880.0929256, 3629.1999213 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3038.6487402 3441.1165626, 3188.2550469 3441.1165626)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3506.0771869, 3629.1999213 3884.0299485)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3310.3022872 3856.4708821, 3310.3022872 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3506.0771869, 3481.562126 3506.0771869)"));
    ans.add((LineString) wktReader.read("LINESTRING( 4113.4518973 3677.3370257, 4078.0188039 3677.3370257)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3670.5385021 3385.9984486, 3841.7983502 3385.9984486)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3841.7983502 3880.0929256, 3841.7983502 3858.4393935)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3670.5385021 3506.0771869, 3629.1999213 3506.0771869)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3310.3022872 3387.9669413, 3310.3022872 3419.4630118)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3670.5385021 3385.9984486, 3670.5385021 3506.0771869)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3680.3810217 3387.9669413, 3670.5385021 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3310.3022872 3856.4708821, 3050.4597713 3856.4708821)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3050.4597713 3750.171677, 3050.4597713 3856.4708821)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3471.7196063 3303.321287, 3680.3810217 3303.321287)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3880.0929256, 3310.3022872 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3481.562126 3387.9669413, 3471.7196063 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3841.7983502 3385.9984486, 3841.7983502 3419.4630118)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3506.0771869, 3629.1999213 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3481.562126 3387.9669413, 3481.562126 3506.0771869)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3821.0378075, 3629.1999213 3716.7071044)"));
    ans.add((LineString) wktReader.read("LINESTRING( 4078.0188039 3858.4393935, 4078.0188039 3677.3370257)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3188.2550469 3419.4630118, 3310.3022872 3419.4630118)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3670.5385021 3387.9669413, 3670.5385021 3506.0771869)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3884.0299485, 3522.9007068 3884.0299485)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3310.3022872 3387.9669413, 3471.7196063 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3716.7071044, 3522.9007068 3821.0378075)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3471.7196063 3303.321287, 3471.7196063 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 4113.4518973 3677.3370257, 4113.4518973 3441.1165626)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3188.2550469 3419.4630118, 3188.2550469 3441.1165626)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3821.0378075, 3522.9007068 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3670.5385021 3506.0771869, 3670.5385021 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3841.7983502 3419.4630118, 3963.8455906 3419.4630118)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3310.3022872 3387.9669413, 3481.562126 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 4113.4518973 3441.1165626, 4113.4518973 3677.3370257)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3506.0771869, 3522.9007068 3884.0299485)"));
    ans.add((LineString) wktReader.read("LINESTRING( 4078.0188039 3677.3370257, 4078.0188039 3858.4393935)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3506.0771869, 3522.9007068 3716.7071044)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3716.7071044, 3629.1999213 3506.0771869)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3963.8455906 3441.1165626, 3963.8455906 3419.4630118)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3963.8455906 3419.4630118, 3963.8455906 3441.1165626)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3680.3810217 3303.321287, 3680.3810217 3387.9669413)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3963.8455906 3441.1165626, 4113.4518973 3441.1165626)"));
    ans.add((LineString) wktReader.read("LINESTRING( 4078.0188039 3858.4393935, 3841.7983502 3858.4393935)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3880.0929256, 3629.1999213 3821.0378075)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3038.6487402 3441.1165626, 3038.6487402 3750.171677)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3841.7983502 3858.4393935, 3841.7983502 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3629.1999213 3884.0299485, 3629.1999213 3880.0929256)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3050.4597713 3750.171677, 3038.6487402 3750.171677)"));
    ans.add((LineString) wktReader.read("LINESTRING( 3522.9007068 3880.0929256, 3522.9007068 3884.0299485)"));
    return ans;
  }

}
