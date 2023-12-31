package clients;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.json.JSONObject;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class Consumer {

  static final String PROPERTIES_FILE = (System.getenv("PROPERTIES_FILE") != null) ? System.getenv("PROPERTIES_FILE") : "./consumer.config";
  static final String KAFKA_TOPIC  = (System.getenv("TOPIC") != null) ?
                                      System.getenv("TOPIC") : "confluent-audit-log-events";

  /**
   * Java consumer.
   */
  public static void main(String[] args) throws IOException {
    System.out.println("Starting Java Consumer.");

    String clientId  = System.getenv("CLIENT_ID");
    clientId = (clientId != null) ? clientId : "audit-log-consumer";

    // Creating the Kafka Consumer
    final Properties settings = loadPropertiesFile();
    settings.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
    settings.put(ConsumerConfig.GROUP_ID_CONFIG, "audit-log-consumer-group");
    settings.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(settings);

    //
    try {
      // Subscribe to our topic
      consumer.subscribe(List.of(KAFKA_TOPIC));
      while (true) {
        final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, String> record : records) {
          FileWriter fw = new FileWriter("../audit-log-events-consumer.json", true);
          BufferedWriter bw = new BufferedWriter(fw);
          bw.write(record.value());
          bw.newLine();
          bw.close();
          System.out.println(new JSONObject(record.value()).toString(2));
        }
      }
    } finally {
      // Clean up when the application exits or errors
      System.out.println("Closing consumer.");
      consumer.close();
    }
  }

  public static Properties loadPropertiesFile() throws IOException {
    if (!Files.exists(Paths.get(PROPERTIES_FILE))) {
      throw new IOException(PROPERTIES_FILE + " not found.");
    }
    final Properties properties = new Properties();
    try (InputStream inputStream = new FileInputStream(PROPERTIES_FILE)) {
      properties.load(inputStream);
    }
    return properties;
  }
}
