package com.emendes.integration.controller;

import com.emendes.domain.Book;
import com.emendes.domain.LibraryEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"
})
class LibraryEventsControllerTest {

  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;
  private Consumer<Integer, String> consumer;

  @BeforeEach
  void setUp() {
    Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps(
        "group1", "true", embeddedKafkaBroker));

    consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer())
        .createConsumer();

    embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
  }

  @AfterEach
  void tearDown() {
    consumer.close();
  }

  @Test
  @DisplayName("Must return LibraryEvent when post successfully")
  void postLibraryEvent() {
    // given
    Book book = Book.builder()
        .bookId(100)
        .bookName("Spring Boot")
        .bookAuthor("Edson Mendes")
        .build();

    LibraryEvent libraryEvent = LibraryEvent.builder()
        .libraryEventId(null)
        .book(book)
        .build();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent, headers);

    // when
    ResponseEntity<LibraryEvent> actualResponse = restTemplate
        .exchange("/v1/libraryevents", HttpMethod.POST, request, LibraryEvent.class);

    // then
    Assertions.assertThat(actualResponse.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);

    ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events");
    String expectedRecord = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":100,\"bookName\":\"Spring Boot\",\"bookAuthor\":\"Edson Mendes\"}}";
    String actualRecord = singleRecord.value();

    Assertions.assertThat(actualRecord).isNotNull().isEqualTo(expectedRecord);
  }

  @Test
  @DisplayName("Must return LibraryEvent when put successfully")
  void updateLibraryEvent() {
    // given
    Book book = Book.builder()
        .bookId(100)
        .bookName("Spring Boot")
        .bookAuthor("Edson Mendes")
        .build();

    LibraryEvent libraryEvent = LibraryEvent.builder()
        .libraryEventId(1000)
        .book(book)
        .build();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent, headers);

    // when
    ResponseEntity<LibraryEvent> actualResponse = restTemplate
        .exchange("/v1/libraryevents", HttpMethod.PUT, request, LibraryEvent.class);

    // then
    ConsumerRecord<Integer, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events");
    String expectedRecord = "{\"libraryEventId\":1000,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":100,\"bookName\":\"Spring Boot\",\"bookAuthor\":\"Edson Mendes\"}}";
    String actualRecord = singleRecord.value();

    Assertions.assertThat(actualResponse.getStatusCode()).isEqualByComparingTo(HttpStatus.OK);
    Assertions.assertThat(actualRecord).isNotNull().isEqualTo(expectedRecord);
  }
}