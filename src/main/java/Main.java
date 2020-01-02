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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

    // TODO add exif
    // TODO terminal arguments

    private static String xmlFile = "/home/kirill/Downloads/film5/roll.xml";
    private static String photoDir = "/home/kirill/Downloads/film5";

    public static void main(String[] args) {

        Exposure[] exposures = parseXML();
        Map<Integer, String> photos = findPhotos();

        return;
    }

    private static Map<Integer, String> findPhotos() {

        try(Stream<Path> walk = Files.walk(Paths.get(photoDir))){

            Map<Integer, String> result = new HashMap<>();
            int[] i = {0};

            walk
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(x -> x.contains(".jpg")) // TODO others
                .forEach(str -> result.put(i[0]++, str));

            return result;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Exposure[] parseXML() {

        File file = new File(xmlFile);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

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
                String date = exposure.getElementsByTagName("exposure_time_taken").item(0).getFirstChild().getTextContent();
                date = date.replace("T", " ");
                date = date.replaceAll("Z...", "");

                exposuresData[i].number = Integer.parseInt(number);
                exposuresData[i].time = sdf.parse(date);
                exposuresData[i].description = exposure.getElementsByTagName("exposure_description").item(0).getFirstChild().getTextContent();
            }

            return exposuresData;

        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


        return null;
    }
}
