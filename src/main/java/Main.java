import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    // TODO read photo file names
    // TODO add exif
    // TODO terminal arguments

    private static String xmlFile = "/home/kirill/Downloads/film5/roll.xml";
    private static String photoDir = "/home/kirill/Downloads/film5";

    public static void main(String[] args) {

        Exposure[] exposures = parseXML();
        List<String> photos = findPhotos(); // TODO HashMap

        return;
    }

    private static List<String> findPhotos() {

        try(Stream<Path> walk = Files.walk(Paths.get(photoDir))){

            return walk
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(x -> x.contains(".jpg")) // TODO others
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Exposure[] parseXML() {

        File file = new File(xmlFile);

        try {
            Document doc = DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(file);

            //System.out.println("Root element : " + doc.getDocumentElement().getNodeName());

            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement(); // Exif4Film

            NodeList exposures = root.getElementsByTagName("Exposure").item(0).getChildNodes();

            int numberOfExposures = exposures.getLength();
            Exposure[] exposuresData = new Exposure[numberOfExposures];


            System.out.println("Number of exposures: " + numberOfExposures);

            for(int i = 0; i < numberOfExposures; ++i){
                Element exposure = (Element) exposures.item(i);

                exposuresData[i] = new Exposure();

                String number = exposure.getElementsByTagName("exposure_number").item(0).getFirstChild().getTextContent();

                exposuresData[i].number = Integer.parseInt(number);
                exposuresData[i].time = exposure.getElementsByTagName("exposure_time_taken").item(0).getFirstChild().getTextContent();
                exposuresData[i].description = exposure.getElementsByTagName("exposure_description").item(0).getFirstChild().getTextContent();
            }

            return exposuresData;

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }


        return null;
    }
}
