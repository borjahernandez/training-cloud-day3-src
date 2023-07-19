package clients;

import clients.avro.PositionValue;
import clients.avro.SensorValue;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.json.JSONObject;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class Producer {
  static final String VEHICLE_POSITION_FILE_PREFIX = "./data/vehicle-positions/";
  static final String VEHICLE_POSITION_KAFKA_TOPIC = "vehicle-positions";
  static final String VEHICLE_SENSOR_FILE_PREFIX = "./data/vehicle-sensors/";
  static final String VEHICLE_SENSOR_KAFKA_TOPIC = "vehicle-sensors";
  static final String CONFIG_FILE = (System.getenv("CONFIG_FILE") != null) ? System.getenv("CONFIG_FILE") : "/producer.config";


  /**
   * Java producer.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println("Starting Java Avro producer.");

    // Load a vehicle id from an environment variable
    // if it isn't present use "vehicle-1"
    final String vehicleFile  = (System.getenv("VEHICLE_FILE") != null) ? System.getenv("VEHICLE_FILE") : "vehicle-3";

    // Configure the location of the bootstrap server, default serializers,
    // Confluent interceptors, schema registry location
    final Properties positionSettings = loadConfigFile();
    positionSettings.put(ProducerConfig.CLIENT_ID_CONFIG, "position-" + vehicleFile);
    positionSettings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    positionSettings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

    final Properties sensorSettings = loadConfigFile();
    sensorSettings.put(ProducerConfig.CLIENT_ID_CONFIG, "sensor-" + vehicleFile);
    sensorSettings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    sensorSettings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
  
    final KafkaProducer<String, PositionValue> positionProducer = new KafkaProducer<>(positionSettings);

    Thread positionProducerThread = new Thread(() -> {
      try {
        int positionPos = 0;
        final String[] rowsPosition = Files.readAllLines(Paths.get(VEHICLE_POSITION_FILE_PREFIX + vehicleFile + ".json"),
         Charset.forName("UTF-8")).toArray(new String[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          System.out.println("Closing producer.");
          positionProducer.close();
        }));
         
        while (true) {

          // Code related to the position Producer.
          final JSONObject positionJson = new JSONObject(rowsPosition[positionPos]);
          final String positionKey = positionJson.optString("VEHICLE_ID", "-1");
          final PositionValue positionValue = getPositionValue(positionJson);
          
          final ProducerRecord<String, PositionValue> positionRecord = new ProducerRecord<>(
            VEHICLE_POSITION_KAFKA_TOPIC, positionKey, positionValue);
    
          positionProducer.send(positionRecord, (md, e) -> {
            System.out.println(String.format("Sent Vehicle_id:%s Latitude:%.2f Longitude:%.2f Speed:%.2f",
            positionValue.getVehicleId(), positionValue.getLatitude(), positionValue.getLongitude(), positionValue.getSpeed()));
          });
           // Increment the position to loop over the file
          Thread.sleep(1000);
          positionPos = (positionPos + 1) % rowsPosition.length;
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      } finally {
        positionProducer.close();
      }
    });
    

    final KafkaProducer<String, SensorValue> sensorProducer = new KafkaProducer<>(sensorSettings);

    Thread sensorProducerThread = new Thread(() -> {
      try {
        int sensorPos = 0;
        final String[] rowsSensor = Files.readAllLines(Paths.get(VEHICLE_SENSOR_FILE_PREFIX + vehicleFile + ".json"),
         Charset.forName("UTF-8")).toArray(new String[0]);

         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          System.out.println("Closing producer.");
          sensorProducer.close();
        }));
         
        while (true) {

          // Code related to the sensor Producer.
          final JSONObject sensorJson = new JSONObject(rowsSensor[sensorPos]);
          final String sensorKey = sensorJson.optString("vehicle_id", "-1");
          final SensorValue sensorValue = getSensorValue(sensorJson);
          
          final ProducerRecord<String, SensorValue> sensorRecord = new ProducerRecord<>(
            VEHICLE_SENSOR_KAFKA_TOPIC, sensorKey, sensorValue);

          sensorProducer.send(sensorRecord, (md, e) -> {
            System.out.println(String.format("Sent Vehicle_id:%s EngineTemp:%d AverageRPM:%d",
            sensorValue.getVehicleId(), sensorValue.getEngineTemperature(), sensorValue.getAverageRpm()));
          });
        
           // Increment the position to loop over the file
          Thread.sleep(1000);
          sensorPos = (sensorPos + 1) % rowsSensor.length;
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (IOException e1) {
        e1.printStackTrace();
      } finally {
        sensorProducer.close();
      }
    });
    

    // Start the producer threads
    positionProducerThread.start();
    sensorProducerThread.start();
  }

  

  public static Properties loadConfigFile() throws IOException {
    if (!Files.exists(Paths.get(CONFIG_FILE))) {
      throw new IOException(CONFIG_FILE + " not found.");
    }
    final Properties config = new Properties();
    try (InputStream inputStream = new FileInputStream(CONFIG_FILE)) {
      config.load(inputStream);
    }
    return config;
  }

  public static PositionValue getPositionValue(JSONObject obj) {
    String vehicleId = obj.optString("VEHICLE_ID", "-1");
    double acceleration = obj.optDouble("ACCELERATION", -1.0);
    double latitude = obj.optDouble("LATITUDE", -1.0);
    double longitude = obj.optDouble("LONGITUDE", -1.0); 
    String driverId = obj.optString("DRIVER_ID", "-1");
    Integer odometer = obj.optInt("ODOMETER", -1);
    double speed = obj.optDouble("SPEED", -1.0); 
    long timestamp = System.currentTimeMillis(); 

    return new PositionValue(vehicleId, acceleration, latitude, longitude, driverId, odometer, speed, timestamp);
  }

  public static SensorValue getSensorValue(JSONObject obj) {
    String vehicleId = obj.optString("vehicle_id", "-1");
    int engineTemperature = obj.optInt("engine_temperature", -1);
    int averageRpm = obj.optInt("average_rpm", -1);
    double pressureTyre1 = obj.optDouble("pressure_tyre_1", -1.0); 
    double pressureTyre2 = obj.optDouble("pressure_tyre_2", -1.0);
    double pressureTyre3 = obj.optDouble("pressure_tyre_3", -1.0);
    double pressureTyre4 = obj.optDouble("pressure_tyre_4", -1.0); 
    long timestamp = System.currentTimeMillis(); 

    return new SensorValue(vehicleId, engineTemperature, averageRpm, pressureTyre1, pressureTyre2, pressureTyre3, pressureTyre4, timestamp);
  }
}
