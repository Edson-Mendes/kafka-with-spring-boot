package com.emendes.integration.consumer;

import com.emendes.consumer.LibraryEventsConsumer;
import com.emendes.entity.Book;
import com.emendes.entity.FailureRecord;
import com.emendes.entity.LibraryEvent;
import com.emendes.entity.LibraryEventType;
import com.emendes.repository.FailureRecordRepository;
import com.emendes.repository.LibraryEventRepository;
import com.emendes.service.LibraryEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@EmbeddedKafka(topics = {"library-events", "library-events.RETRY", "library-events.DLT"}, partitions = 3)
@TestPropertySource(properties = {
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "retryListener.startup=false"
})
@ActiveProfiles("test")
class LibraryEventsConsumerTest {

  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;
  @Autowired
  private KafkaTemplate<Integer, String> kafkaTemplate;
  @Autowired
  private KafkaListenerEndpointRegistry endpointRegistry;
  @Autowired
  private LibraryEventRepository libraryEventRepository;
  @Autowired
  private ObjectMapper mapper;
  @Autowired
  private FailureRecordRepository failureRecordRepository;

  @SpyBean
  private LibraryEventsConsumer libraryEventsConsumerSpy;
  @SpyBean
  private LibraryEventService libraryEventsServiceSpy;

  private Consumer<Integer, String> consumer;

  @Value("${topics.retry}")
  private String retryTopic;

  @Value("${topics.dlt}")
  private String deadLetterTopic;

  @BeforeEach
  void setUp() {
    MessageListenerContainer container = endpointRegistry.getListenerContainers()
        .stream().filter(mlc -> Objects.equals(mlc.getGroupId(), "library-events-listener-group"))
        .toList().get(0);

    ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
//    for (MessageListenerContainer messageListenerContainer : endpointRegistry.getListenerContainers()) {
//      ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
//    }
  }

  @AfterEach
  void tearDown() {
    libraryEventRepository.deleteAll();
    failureRecordRepository.deleteAll();
  }

  @Test
  void publishNewLibraryEvent() throws ExecutionException, InterruptedException {
    //given
    String json = "{\"libraryEventId\":null,\"libraryEventType\":\"NEW\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
    kafkaTemplate.sendDefault(json).get();

    //when
    CountDownLatch latch = new CountDownLatch(1);
    // Espera 3 segundos antes de verificar se o consumer consumiu a mensagem.
    latch.await(3, TimeUnit.SECONDS);

    //then
    verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

    List<LibraryEvent> libraryEventList = libraryEventRepository.findAll();

    Assertions.assertThat(libraryEventList).hasSize(1);
    Assertions.assertThat(libraryEventList.get(0).getLibraryEventId()).isNotNull();
    Assertions.assertThat(libraryEventList.get(0).getBook().getBookId()).isNotNull().isEqualTo(456);
  }

  @Test
  void publishUpdateLibraryEvent() throws JsonProcessingException, ExecutionException, InterruptedException {
    //given
    Book book = Book.builder()
        .bookId(456)
        .bookName("Kafka Using Spring Boot")
        .bookAuthor("Dilip")
        .build();

    LibraryEvent libraryEvent = LibraryEvent.builder()
        .libraryEventType(LibraryEventType.NEW)
        .book(book)
        .build();

    book.setLibraryEvent(libraryEvent);
    LibraryEvent savedLibraryEvent = libraryEventRepository.save(libraryEvent);

    String json = String.format(
        "{\"libraryEventId\":%d,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot 2.x\",\"bookAuthor\":\"Edson Mendes\"}}",
        savedLibraryEvent.getLibraryEventId());
    kafkaTemplate.sendDefault(savedLibraryEvent.getLibraryEventId(), json).get();

    //when
    CountDownLatch latch = new CountDownLatch(1);
    // Espera 3 segundos antes de verificar se o consumer consumiu a mensagem.
    latch.await(3, TimeUnit.SECONDS);

    //then
    verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

    Optional<LibraryEvent> libraryEventOptional = libraryEventRepository.findById(savedLibraryEvent.getLibraryEventId());

    Assertions.assertThat(libraryEventOptional).isPresent();
    Assertions.assertThat(libraryEventOptional.get().getLibraryEventType()).isEqualByComparingTo(LibraryEventType.UPDATE);
    Assertions.assertThat(libraryEventOptional.get().getBook().getBookName()).isEqualTo("Kafka Using Spring Boot 2.x");
    Assertions.assertThat(libraryEventOptional.get().getBook().getBookAuthor()).isEqualTo("Edson Mendes");
  }

  @Test
  void publishModifyLibraryEvent_Null_LibraryEventId() throws JsonProcessingException, InterruptedException, ExecutionException {
    //given
    Integer libraryEventId = null;
    String json = "{\"libraryEventId\":" + libraryEventId + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
    kafkaTemplate.sendDefault(libraryEventId, json).get();
    //when
    CountDownLatch latch = new CountDownLatch(1);
    latch.await(3, TimeUnit.SECONDS);


    verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

    Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group3", "true", embeddedKafkaBroker));
    consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
    embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, deadLetterTopic);

    ConsumerRecords<Integer, String> consumerRecord = KafkaTestUtils.getRecords(consumer);

    var deadletterList = new ArrayList<ConsumerRecord<Integer, String>>();
    consumerRecord.forEach((record) -> {
      if (record.topic().equals(deadLetterTopic)) {
        deadletterList.add(record);
      }
    });

    var finalList = deadletterList.stream()
        .filter(record -> record.value().equals(json))
        .collect(Collectors.toList());

    assert finalList.size() == 1;
  }

  @Test
  void publishModifyLibraryEvent_999_LibraryEventId_deadletterTopic() throws JsonProcessingException, InterruptedException, ExecutionException {
    //given
    Integer libraryEventId = 999;
    String json = "{\"libraryEventId\":" + libraryEventId + ",\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
    kafkaTemplate.sendDefault(libraryEventId, json).get();
    //when
    CountDownLatch latch = new CountDownLatch(1);
    latch.await(3, TimeUnit.SECONDS);

    verify(libraryEventsConsumerSpy, atLeast(2)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventsServiceSpy, atLeast(2)).processLibraryEvent(isA(ConsumerRecord.class));


    Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
    consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
    embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, retryTopic);

    ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, retryTopic);

    System.out.println("consumer Record in deadletter topic : " + consumerRecord.value());

    Assertions.assertThat(consumerRecord.value()).isEqualTo(json);

    consumerRecord.headers()
        .forEach(header -> {
          System.out.println("Header Key : " + header.key() + ", Header Value : " + new String(header.value()));
        });
  }

  @Test
  void publishModifyLibraryEvent_null_LibraryEventId_failureRecord() throws JsonProcessingException, InterruptedException, ExecutionException {
    //given
    String json = "{\"libraryEventId\": null,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot\",\"bookAuthor\":\"Dilip\"}}";
    kafkaTemplate.sendDefault(json).get();

    //when
    CountDownLatch latch = new CountDownLatch(1);
    latch.await(5, TimeUnit.SECONDS);


    verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventsServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

    FailureRecord actualFailureRecord = failureRecordRepository.findById(1).orElse(null);

    Assertions.assertThat(actualFailureRecord).isNotNull();
    log.info("FailureRecord : {}", actualFailureRecord);
  }
}