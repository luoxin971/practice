package practice;


import java.util.*;
import java.util.stream.Collectors;

/**
 * content
 *
 * @author luoxin
 * @since 2022/1/25
 */
public class Practice {
  void test() {
    List<String> list = new ArrayList<>(Arrays.asList("COLLADA", "asset", "contributor", "authoring_tool", "created", "modified", "up_axis", "library_images", "library_effects", "effect", "profile_COMMON", "technique", "lambert", "emission", "color", "diffuse", "color", "bump", "reflective", "color", "reflectivity", "float", "transparent", "float", "effect", "profile_COMMON", "technique", "lambert", "emission", "color", "diffuse", "color", "bump", "reflective", "color", "reflectivity", "float", "transparent", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "transparent", "float", "transparency", "float", "effect", "profile_COMMON", "technique", "lambert", "emission", "color", "diffuse", "color", "bump", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "extra", "technique", "double_sided", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "extra", "technique", "double_sided", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "extra", "technique", "double_sided", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "extra", "technique", "double_sided", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "effect", "profile_COMMON", "technique", "phong", "emission", "color", "diffuse", "color", "bump", "specular", "color", "shininess", "float", "reflective", "color", "reflectivity", "float", "library_materials", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "material", "instance_effect", "library_geometries", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "geometry", "mesh", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "vertices", "input", "source", "float_array", "technique_common", "accessor", "param", "param", "param", "source", "float_array", "technique_common", "accessor", "param", "param", "triangles", "input", "input", "input", "p", "triangles", "input", "input", "input", "p", "library_visual_scenes", "visual_scene", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "node", "matrix", "instance_geometry", "bind_material", "technique_common", "instance_material", "bind_vertex_input", "instance_material", "bind_vertex_input", "node", "matrix", "node", "matrix", "node", "matrix", "scene", "instance_visual_scene"));
    List<String> unrepeated = list.stream().distinct().collect(Collectors.toList());
    System.out.println(unrepeated);
    System.out.println(unrepeated.size());
  }
  
  public static void main(String[] args) {
    new Practice().test();
  }
}
