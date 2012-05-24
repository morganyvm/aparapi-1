package detection;

/**
This project is based on the open source jviolajones project created by Simon
Houllier and is used with his permission. Simon's jviolajones project offers 
a pure Java implementation of the Viola-Jones algorithm.

http://en.wikipedia.org/wiki/Viola%E2%80%93Jones_object_detection_framework

The original Java source code for jviolajones can be found here
http://code.google.com/p/jviolajones/ and is subject to the
gnu lesser public license  http://www.gnu.org/licenses/lgpl.html

Many thanks to Simon for his excellent project and for permission to use it 
as the basis of an Aparapi example.
**/

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

public class DetectorNoJDOM{

   /** The list of classifiers that the test image should pass to be considered as an image.*/
   Detector.Stage[] stages;

   Point size;

   /**Factory method. Builds a detector from an XML file.
    * @param filename The XML file (generated by OpenCV) describing the Haar Cascade.
    * @return The corresponding detector.
    */
   public static DetectorNoJDOM create(String filename) {

      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      try {
         DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
         try {
            Document document = documentBuilder.parse(new File(filename));
            return (new DetectorNoJDOM(document));
         } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
      } catch (ParserConfigurationException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      return (null);
   }

   /** Detector constructor.
    * Builds, from a XML document (i.e. the result of parsing an XML file, the corresponding Haar cascade.
    * @param document The XML document (parsing of file generated by OpenCV) describing the Haar cascade.
    * 
    * http://code.google.com/p/jjil/wiki/ImplementingHaarCascade
    */

   public static <T extends org.w3c.dom.Node> T getNode(org.w3c.dom.Node rootNode, Class<T> clazz, String xpathString) {

      if (rootNode != null && xpathString != null && !xpathString.equals("")) {
         XPath xpath = XPathFactory.newInstance().newXPath();
         try {
            org.w3c.dom.Node node = (org.w3c.dom.Node) xpath.evaluate(xpathString, rootNode, XPathConstants.NODE);
            if (node != null) {
               node.getParentNode().removeChild(node);
            }
            return ((T) node);

         } catch (XPathExpressionException xpathException) {
            System.out.println("Exception " + xpathException + " with xpath '" + xpathString + "'");
         }
      }

      return (null);
   }

   public static <T extends org.w3c.dom.Node> Collection<T> getNodes(org.w3c.dom.Node rootNode, Class<T> clazz, String xpathString) {
      List<T> nodes = new ArrayList<T>();
      if (rootNode != null && xpathString != null && !xpathString.equals("")) {
         XPath xpath = XPathFactory.newInstance().newXPath();
         try {

            //  StopWatch sw = new StopWatch();
            //  sw.start();
            NodeList nodeList = (NodeList) xpath.evaluate(xpathString, rootNode, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
               org.w3c.dom.Node node = nodeList.item(i);
               nodes.add((T) node);
               node.getParentNode().removeChild(node);
            }
            //  sw.print("time "+xpathString);

         } catch (XPathExpressionException xpathException) {
            System.out.println("Exception " + xpathException + " with xpath '" + xpathString + "'");
         }
      }

      return (nodes);
   }

   public DetectorNoJDOM(org.w3c.dom.Document document) {
      List<Detector.Stage> stageList = new LinkedList<Detector.Stage>();

      org.w3c.dom.Element racine = getNode(document.getDocumentElement(), org.w3c.dom.Element.class, "/opencv_storage/*[1]"); // first element under opencv_storage
      //getChild( getChild( document.getDocumentElement(), org.w3c.dom.Element.class, 0), org.w3c.dom.Element.class, 0);

      String sizeStr = getNode(racine, org.w3c.dom.Text.class, "size/text()").getNodeValue();
      Scanner scanner = new Scanner(sizeStr);
      size = new Point(scanner.nextInt(), scanner.nextInt());

      for (org.w3c.dom.Element stage : getNodes(racine, org.w3c.dom.Element.class, "stages/_")) {
         float thres = Float.parseFloat(getNode(stage, org.w3c.dom.Text.class, "stage_threshold/text()").getNodeValue());
         Detector.Stage st = new Detector.Stage(0, thres);
         //  System.out.println("create stage "+thres);

         for (org.w3c.dom.Element tree : getNodes(stage, org.w3c.dom.Element.class, "trees/_")) {
            Detector.Tree t = new Detector.Tree(0, st);
            for (org.w3c.dom.Element feature : getNodes(tree, org.w3c.dom.Element.class, "_")) {
               float thres2 = Float.parseFloat(getNode(feature, org.w3c.dom.Text.class, "threshold/text()").getNodeValue());
               //  System.out.println(thres2);
               int left_node = -1;
               float left_val = 0;
               boolean has_left_val = false;

               Text leftValNode = getNode(feature, org.w3c.dom.Text.class, "left_val/text()");
               if (leftValNode != null) {
                  left_val = Float.parseFloat(leftValNode.getNodeValue());
                  has_left_val = true;
               } else {
                  left_node = Integer.parseInt(getNode(feature, org.w3c.dom.Text.class, "left_node/text()").getNodeValue());
                  has_left_val = false;
               }
               int right_node = -1;
               float right_val = 0;
               boolean has_right_val = false;
               Text rightValNode = getNode(feature, org.w3c.dom.Text.class, "right_val/text()");
               if (rightValNode != null) {
                  right_val = Float.parseFloat(rightValNode.getNodeValue());
                  has_right_val = true;
               } else {
                  right_node = Integer.parseInt(getNode(feature, org.w3c.dom.Text.class, "right_node/text()").getNodeValue());
                  has_right_val = false;
               }
               Detector.Feature f = new Detector.Feature(0, t, thres2, left_val, left_node, has_left_val, right_val, right_node,
                     has_right_val, size);
               for (org.w3c.dom.Text txt : getNodes(feature, org.w3c.dom.Text.class, "feature/rects/_/text()")) {
                  String s = txt.getNodeValue().trim();
                  //System.out.println(s);
                  String[] tab = s.split(" ");
                  int x1 = Integer.parseInt(tab[0]);
                  int x2 = Integer.parseInt(tab[1]);
                  int y1 = Integer.parseInt(tab[2]);
                  int y2 = Integer.parseInt(tab[3]);
                  float w = Float.parseFloat(tab[4]);

                  Detector.Rect r = new Detector.Rect(0, x1, x2, y1, y2, w);

                  f.add(r);
               }
               t.addFeature(f);
            }
            st.addTree(t);
            // System.out.println("Number of nodes in tree " + t.features.size());
         }
         //  System.out.println("Number of trees : " + st.trees.size());
         stageList.add(st);
         //   System.out.println("Stages : " + stageList.size());
      }

      stages = stageList.toArray(new Detector.Stage[0]);

      //System.out.println(stages.length);

   }

}
