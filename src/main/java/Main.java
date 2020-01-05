import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    // TODO process unprocessed files
    // TODO terminal arguments

    private static String xmlFile = "/home/kirill/Downloads/film5/roll.xml";
    private static String photoDir = "/home/kirill/Downloads/film5";

    public static void main(String[] args) {

        Exposure[] exposures = parseXML();
        Map<Integer, String> photos = findPhotos();

        matchPhotosWithMetadata(photos, exposures);
    }

    private static void matchPhotosWithMetadata(Map<Integer, String> photos, Exposure[] exposures) {
        Scanner scanner = new Scanner(System.in);

        for(Exposure exposure : exposures){

            System.out.println("\n" + exposure.toString() + "\n");

            System.out.println("Choose photo for this exposure: ");

            String availablePhotos = photos.toString()
                    .replaceAll("[{}]", "")
                    .replaceAll(", ", "\n")
                    .replaceAll("=", " : ");

            System.out.println(availablePhotos);

            int photo = scanner.nextInt();

            if(photos.containsKey(photo)) {
                setPhotoMetadata(photos.get(photo), exposure);

                photos.remove(photo);
            } else {
                System.out.println("Wrong photo index, missing that exposure");
            }
        }
    }

    // based on https://github.com/apache/commons-imaging/blob/master/src/test/java/org/apache/commons/imaging/examples/WriteExifMetadataExample.java
    private static void setPhotoMetadata(String photo, Exposure exposureData) {

        String destDir = photoDir + "/result/";
        String processedDir = photoDir + "/processed/";

        new File(destDir).mkdirs();
        new File(processedDir).mkdirs();

        String photoName = String.format("%02d", exposureData.number) + "_" + exposureData.description;

        File dest = new File(destDir + photoName + ".jpg");

        File file = new File(photo);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");

        try (FileOutputStream fos = new FileOutputStream(dest);
             OutputStream os = new BufferedOutputStream(fos)){

            TiffOutputSet outputSet = null;

            final ImageMetadata metadata = Imaging.getMetadata(file);
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

            if(jpegMetadata != null){
                final TiffImageMetadata exif = jpegMetadata.getExif();

                if(exif != null){
                    outputSet = exif.getOutputSet();
                }
            }

            if(outputSet == null){
                outputSet = new TiffOutputSet();
            }

            final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();

            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, sdf.format(exposureData.time));

            new ExifRewriter().updateExifMetadataLossless(file, os,
                    outputSet);

            file.renameTo(new File(processedDir + file.getName()));

        } catch (IOException | ImageReadException | ImageWriteException e) {
            e.printStackTrace();
        }
    }

    private static Map<Integer, String> findPhotos() {

        try(Stream<Path> walk = Files.walk(Paths.get(photoDir), 1)){

            Map<Integer, String> result = new HashMap<>();
            int[] i = {1};

            walk
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(x -> x.contains(".jpg")) // TODO others
                .sorted()
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
