import java.io.FileWriter; // Helps create and write files onto the disk, so in a particular folder.
import java.io.PrintWriter; // Makes writing formatted text into files.
import java.io.IOException; // Helps when creating the file fails.
import java.time.Instant; 
import java.time.ZoneOffset; // Helps to define a timezone.
import java.time.format.DateTimeFormatter; // Helps convert time javas time structer into something readable.
import java.time.ZonedDateTime; // Represents a date + time + timezone.
import java.util.Random;

public class WeatherBalloonSimulation {
    public static void main (String[] args) throws IOException { // Crashes the programs instead of making the user fix it.
        // Change Lat and Lon to specific cords.
        double startLat = 34.2501;
        double startLon = -119.0302;
        double ascentRate_mps = 5.0; // How fast the balloon rises in m/s.
        double maxAltitude_m = 32500.0; // Max altitude the balloon can burst at generally between [30 - 35] km
        int timeStepSec = 30; // Every time a data point is taken in seconds.
        int totalSecondsCap = 3 * 60 * 60; // How long the balloon is in the air for. This is for saftey purposes, so the simulation does not run infinitly.
        double descentMultiplier = 2.0; // Multiplier used along with ascent speed to simulate faster decent.
        double latDriftPerStep = 0.00008; // Uses the rough degree of latitude of 111 km to the equator, to calculate the smallest effect of wind on the balloon.
        double lonDriftPerStep = 0.00012; // ^ This is also used for the longitude which is 111.32 km
        long startEpochSecond = Instant.parse("2026-03-01T10:00:00Z").getEpochSecond();
        boolean addRandomNoise = true; //Decides if to simulate "noise," which is things that could happen in the sky."
        
        // Chosen to simulate sensor error. used with plus or minus
        double altitudeNoiseStd = 2.5; // 1 <= x <= 5
        double tempNoiseStd = 0.2; // 0.1 <= x <= 0.5
        double pressureNoiseStd = 0.4; // 0.3 <= x <= 1

        // Another limiter to stop inifinity possibilities.
        int stepCaps = Math.max((int)(totalSecondsCap / (double) timeStepSec), 1000);

        // outputs CSV data which is read by the web map.
        PrintWriter out = new PrintWriter(new FileWriter ("weather_balloon_telemetry.csv"));
        out.println("timestamp, latitude, longitude, altitude_m, temperature_c, pressure_hpa");

        /*==================States Predetermined Variables======================= */
        double lat = startLat;
        double lon = startLon;
        double altitude = 0.0;
        boolean burstOccurred = false;
        int step = 0;
        Random rnd = new Random(12345);

        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
        /*======================================================================== */

        while (step < stepCaps) { 
            long currentEpoch = startEpochSecond + (long) step * timeStepSec;
            ZonedDateTime sdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(currentEpoch), ZoneOffset.UTC);
            String timestamp = fmt.format(sdt);

            // Altitude
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

            // Noise Production
            double altitudeNoisy = altitude;
            if (addRandomNoise) {
                altitudeNoisy += rnd.nextGaussian() * altitudeNoiseStd;
            }

            double temperature = 15.0 - 0.0065 * altitudeNoisy; //Temperature decreases 6.5 C for every 1000 m of altitude [Environmental Lapse Rate]
            if (addRandomNoise) {
                temperature += rnd.nextGaussian() * tempNoiseStd;
            }

            // Uses Exponensial Pressure Drop Model, since pressure decreases as altitud increase.
            // 1013.25 is pressure at sea level & 8400.0 standard approximation of scale height
            double pressure = 1013.25 * Math.exp(- altitudeNoisy / 8400.0);
            if (addRandomNoise) {
                pressure += rnd.nextGaussian() * pressureNoiseStd;
            }

            // Made to simulate tinest of drift due to wind.
            lat += latDriftPerStep + (rnd.nextGaussian() * 0.00001);
            lon += lonDriftPerStep + (rnd.nextGaussian() * 0.00001);

            out.printf("%s,%.6f,%.6f,%.2f,%.2f,%.2f%n",
                timestamp,lat,lon,altitudeNoisy,temperature,pressure
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
