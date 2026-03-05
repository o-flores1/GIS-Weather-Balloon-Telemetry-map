import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.Random;

public class WeatherBalloonSimulation {
    public static void main (String[] args) throws IOException {
        double startLat = 34.2501;
        double startLon = -119.0302;
        double ascentRate_mps = 5.0;
        double maxAltitude_m = 25000.0;
        int timeStepSec = 30;
        int totalSecondsCap = 3 * 60 * 60;
        double descentMultiplier = 2.0;
        double latDriftPerStep = 0.00008;
        double lonDriftPerStep = 0.00012;
        long startEpochSecond = Instant.parse("2026-03-01T10:00:00Z").getEpochSecond();
        boolean addRandomNoise = true;
        double altitudeNoiseStd = 2.5;
        double tempNoiseStd = 0.2;
        double pressureNoiseStd = 0.4;

        int stepCaps = Math.max((int)(totalSecondsCap / (double) timeStepSec), 1000);
        PrintWriter out = new PrintWriter(new FileWriter (weather_balloon_telemetry.csv));
        out.println("timestamp, latitude, longitude, altitude_m, temperature_c, pressure_hpa");

        double lat = startLat;
        double lon = startLon;
        double altitude = 0.0;
        boolean burstOccurred = false;
        int step = 0;
        Random rnd = new Random(12345);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

        while (step < stepCaps) { 
            long currentEpoch = startEpochSecond + (long) step * timeStepSec;
            ZonedDateTime sdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentEpoch), ZoneOffset.UTC);
            String timestamp = fmt.format(zdt);

            if (!burstOccurred) {
                altitude += ascentRate_mps * timeStepSec;
                if (altitude >= maxAltitude_m) {
                    altitude = maxAltitude_m;
                    if (rnd.nextDouble() < 0.6) { 
                        burstOccurred = true;
                    }
                }
            } else {
                altitude -= ascentRate_mps * timeStepSec * descentMultiplier;
                if (altitude < 0) altitude = 0;
            }

            double altitudeNoisy = altitude;
            double tempNoisy;
            double pressureNoisy;
            if (addRandomNoise) {
                altitudeNoisy += rnd.nextGaussian() * altitudeNoiseStd;
            }

            double temperature = 15.0 - 0.0065 * altitudeNoisy;
            if (addRandomNoise) {
                temperature += rnd.nextGaussian() * tempNoiseStd;
            }

            double pressure = 1013.25 * Math.exp(- altitudeNoisy / 8400.0);
            if (addRandomNoise) {
                pressure += rnd.nextGaussian() * pressureNoiseStd;
            }

            lat += latDriftPerStep + (rnd.nextGaussian() * 0.00001);
            lon += lonDriftPerStep + (rnd.nextGaussian() * 0.00001);

            out.printf("%s,%.6f,%.6f,%.2f,%.2f,%.2f%n",
                timestamp,
                lat,
                lon,
                altitudeNoisy,
                temperature,
                pressure
            );

            step++;

            if (burstOccurred && altitude <= 0.0) {
                break;
            }


        }

        out.close();
        System.out.println("Generated weather_balloon_telemetry.csv with " + step + " rows.");
    }
}
