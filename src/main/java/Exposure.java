import java.util.Date;

class Exposure {
    int number;

    Date time;
    String description;

    @Override
    public String toString() {
        return "Exposure № " + String.format("%02d", number) + "\n" +
                time.toString() + "\n" +
                description;
    }
}
