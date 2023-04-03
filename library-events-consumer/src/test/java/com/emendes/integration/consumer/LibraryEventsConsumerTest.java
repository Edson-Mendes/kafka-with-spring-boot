package com.emendes.integration.consumer;

import com.emendes.consumer.LibraryEventsConsumer;
import com.emendes.entity.Book;
import com.emendes.entity.LibraryEvent;
import com.emendes.entity.LibraryEventType;
import com.emendes.repository.LibraryEventRepository;
import com.emendes.service.LibraryEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}"
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

  @SpyBean
  private LibraryEventsConsumer libraryEventsConsumerSpy;
  @SpyBean
  private LibraryEventService libraryEventServiceSpy;

  @BeforeEach
  void setUp() {
    for (MessageListenerContainer messageListenerContainer : endpointRegistry.getListenerContainers()) {
      ContainerTestUtils.waitForAssignment(messageListenerContainer, embeddedKafkaBroker.getPartitionsPerTopic());
    }
  }

  @AfterEach
  void tearDown() {
    libraryEventRepository.deleteAll();
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
    verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

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
    verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));

    Optional<LibraryEvent> libraryEventOptional = libraryEventRepository.findById(savedLibraryEvent.getLibraryEventId());

    Assertions.assertThat(libraryEventOptional).isPresent();
    Assertions.assertThat(libraryEventOptional.get().getLibraryEventType()).isEqualByComparingTo(LibraryEventType.UPDATE);
    Assertions.assertThat(libraryEventOptional.get().getBook().getBookName()).isEqualTo("Kafka Using Spring Boot 2.x");
    Assertions.assertThat(libraryEventOptional.get().getBook().getBookAuthor()).isEqualTo("Edson Mendes");
  }

  @Test
  void publishUpdateLibraryEvent_withInvalidData() throws JsonProcessingException, ExecutionException, InterruptedException {
    //given
    String json =
        "{\"libraryEventId\":null,\"libraryEventType\":\"UPDATE\",\"book\":{\"bookId\":456,\"bookName\":\"Kafka Using Spring Boot 2.x\",\"bookAuthor\":\"Edson Mendes\"}}";
    kafkaTemplate.sendDefault(json).get();

    //when
    CountDownLatch latch = new CountDownLatch(1);
    // Espera 3 segundos antes de verificar se o consumer consumiu a mensagem.
    latch.await(5, TimeUnit.SECONDS);

    //then
    verify(libraryEventsConsumerSpy, times(1)).onMessage(isA(ConsumerRecord.class));
    verify(libraryEventServiceSpy, times(1)).processLibraryEvent(isA(ConsumerRecord.class));
  }
}