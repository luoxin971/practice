package xmlparse;

import lombok.Getter;
import lombok.Setter;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Collections;
import java.util.Objects;

/**
 * content
 *
 * @author luoxin
 * @since 2022/1/25
 */
@Getter
@Setter
public class DaeParse {
  private Document doc;

  public void t() {
    SAXReader xmlReader = new SAXReader();
    try {
      doc = xmlReader.read(new File("xkool/resources/abc.dae"));
    } catch (DocumentException e) {
      e.printStackTrace();
    }
  }

  public void printNode(Element element, int depth) {
    if (Objects.isNull(element)) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(String.join("", Collections.nCopies(depth * 2, " ")));
    sb.append(element.getName()).append(" [");
    element
        .attributes()
        .forEach(
            attribute ->
                sb.append(attribute.getName())
                    .append(": ")
                    .append(attribute.getValue())
                    .append(" "));
    sb.append("]");
    System.out.println(sb);
    element.elements().forEach(e -> printNode(e, depth + 1));
  }

  public void printRoot() {
    if (doc == null) {
      return;
    }
    this.getDoc().getRootElement().elements().forEach(element -> printNode(element, 0));
  }

  public static void main(String[] args) {
    DaeParse daeParse = new DaeParse();
    daeParse.t();
    daeParse.printRoot();
  }
}
