import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class Main {

    // TODO cut photos names in output

    // TODO process unprocessed files
        // TODO another round with leftovers
        // TODO save unused xml data
        // TODO stop cycle when there is no more photos left
        // TODO read camera data
        // TODO offer filling photo exif data manually

    private static String photoDir;
    private static String xmlFile;

    private static String destinationDir;
    private static String processedDir;

    public static void main(String[] args) {

        if(initPaths()) {

            // Prepare data
            Exposure[] exposures = parseXML();
            Map<Integer, String> photos = findPhotos();
            if(exposures == null || photos == null){ return; }

            // Match data
            matchingCycle(exposures, photos);

        }
    }

    private static void matchingCycle(Exposure[] exposures, Map<Integer, String> photos) {

        while (true) {
            matchPhotosWithMetadata(photos, exposures);

            // TODO delete used exposures
            if(exposures.length > 0 && photos.size() > 0){
                System.out.println(
                        "There is " + exposures.length + " exposures and " + photos.size() + " photos left. " +
                                "\nMatch them? [y/n]");

                // FIXME for some reason throws NoSuchElementException
                Scanner scanner = new Scanner(System.in);
                String answer = scanner.nextLine();
                scanner.close();

                if(!answer.equals("y")){
                    break;
                }
            } else {
                break;
            }
        }
    }

    private static boolean initPaths() {

        // Reading init file

        String initFileName = "init";

        try(FileReader fileReader = new FileReader(initFileName);
            BufferedReader reader = new BufferedReader(fileReader)){

            reader.lines()
                    .forEach(str -> {
                        String[] tokens = str.split(" ");
                        switch (tokens[0]){
                            case "photoDir:":
                                photoDir = tokens[1];
                                break;
                            case "xmlFile:":
                                xmlFile = tokens[1];
                                break;
                        }
                    });


        } catch (IOException e) {
            System.out.println("Couldn't read init file");
            e.printStackTrace();

            return false;
        }

        // Checking files

        if(! new File(photoDir).isDirectory()){
            System.out.println(photoDir + " doesn't look like a directory");
            return false;
        }

        if(! ( new File(xmlFile).isFile() && xmlFile.endsWith(".xml") ) ){
            System.out.println(xmlFile + " doesn't look line an xml file");
            return false;
        }

        // Preparing other paths

        destinationDir = photoDir + "result/";
        processedDir = photoDir + "processed/";

        File dest = new File(destinationDir);
        File proc = new File(processedDir);

        //noinspection ResultOfMethodCallIgnored
        dest.mkdirs();
        //noinspection ResultOfMethodCallIgnored
        proc.mkdirs();

        if( ! ( dest.isDirectory() && proc.isDirectory() )){
            System.out.println("Couldn't create output dirs");
            return false;
        }

        return true;
    }

    private static void matchPhotosWithMetadata(Map<Integer, String> photos, Exposure[] exposures) {
        Scanner scanner = new Scanner(System.in);

        for(Exposure exposure : exposures){

            System.out.println("\n" + exposure.toString() + "\n");

            System.out.println("Choose photo for this exposure: \n0 to skip\n");

            printPhotoNamesWithIndexes(photos);

            int photo = Integer.parseInt(scanner.nextLine());

            if(photos.containsKey(photo)) {
                setPhotoMetadata(photos.get(photo), exposure);

                photos.remove(photo);
            } else {
                System.out.println("Wrong photo index, missing that exposure");
            }
        }

        scanner.close();
    }

    private static void printPhotoNamesWithIndexes(Map<Integer, String> photos) {
        // FIXME this transformation should be done only once
        // TODO also why not store files in that map?
        photos.entrySet().stream().forEach(entry ->
            System.out.println(
                String.format("%02d", entry.getKey()) + " : " +
                entry.getValue().substring(entry.getValue().lastIndexOf("/") + 1))
        );
    }

    // based on https://github.com/apache/commons-imaging/blob/master/src/test/java/org/apache/commons/imaging/examples/WriteExifMetadataExample.java
    private static void setPhotoMetadata(String photo, Exposure exposureData) {

        String photoName = String.format("%02d", exposureData.getNumber()) + "_" + exposureData.getDescription();

        File dest = new File(destinationDir + photoName + ".jpg");

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
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, sdf.format(exposureData.getTime()));

            if(exposureData.getAperture() != null) {
                RationalNumber aperture = RationalNumber.valueOf(exposureData.getAperture());
                exifDirectory.add(ExifTagConstants.EXIF_TAG_APERTURE_VALUE, aperture);
            }

            if(exposureData.getShutterSpeed() != null) {
                RationalNumber shutterSpeed = RationalNumber.valueOf(exposureData.getShutterSpeed());
                exifDirectory.add(ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE, shutterSpeed);
            }

            new ExifRewriter().updateExifMetadataLossless(file, os,
                    outputSet);

            if(!file.renameTo(new File(processedDir + file.getName()))){
                System.out.println("Couldn't move " + file.getName() + " to processed/");
            }

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

        System.out.println("Couldn't parse photo paths");

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

                String date = getElementText(exposure, "exposure_time_taken");

                date = date.replace("T", " ");
                date = date.replaceAll("Z...", "");

                exposuresData[i] = new Exposure(
                        i,
                        sdf.parse(date),
                        getElementText(exposure, "exposure_description"),
                        getElementDouble(exposure, "exposure_aperture"),
                        getElementDouble(exposure, "exposure_shutter_speed"));
            }

            return exposuresData;

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Couldn't read exposures");

        return null;
    }

    private static String getElementText(Element exposure, String tagName) {
        try{
            return exposure.getElementsByTagName(tagName).item(0).getFirstChild().getTextContent();
        } catch (Exception e){
            return "";
        }
    }

    private static Double getElementDouble(Element exposure, String tagName) {
        String elementText = getElementText(exposure, tagName);

        if(elementText.contains("/")){
            String[] parts = elementText.split("/");
            return Double.parseDouble(parts[0])/Double.parseDouble(parts[1]);
        }

        try {
            return Double.parseDouble(elementText);
        } catch (Exception e){
            return null;
        }
    }
}
