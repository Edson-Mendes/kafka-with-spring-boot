package com.emendes.unit.producer;

import com.emendes.domain.Book;
import com.emendes.domain.LibraryEvent;
import com.emendes.producer.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryEventProducerTest {

  @InjectMocks
  private LibraryEventProducer libraryEventProducer;
  @Mock
  private KafkaTemplate<Integer, String> kafkaTemplateMock;
  @Spy
  private ObjectMapper mapper = new ObjectMapper();

  @Test
  void sendLibraryEvent_Approach2_failure() throws JsonProcessingException, ExecutionException, InterruptedException {
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

    SettableListenableFuture future = new SettableListenableFuture();
    future.setException(new RuntimeException("Exception calling Kafka"));
    when(kafkaTemplateMock.send(isA(ProducerRecord.class)))
        .thenReturn(future);

    //when
    Assertions.assertThatExceptionOfType(ExecutionException.class)
        .isThrownBy(() -> libraryEventProducer.sendLibraryEvent_Approach2(libraryEvent).get());
  }

  @Test
  void sendLibraryEvent_Approach2_success() throws JsonProcessingException, ExecutionException, InterruptedException {
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

    SettableListenableFuture future = new SettableListenableFuture();

    String record = mapper.writeValueAsString(libraryEvent);

    ProducerRecord<Integer, String> producerRecord = new ProducerRecord(
        "library-events", libraryEvent.getLibraryEventId(), record);

    RecordMetadata recordMetadata = new RecordMetadata(
        new TopicPartition("library-events", 1),
        1, 1, 342, System.currentTimeMillis(), 1, 2
    );
    SendResult<Integer, String> sendResult = new SendResult<>(producerRecord, recordMetadata);
    future.set(sendResult);
    when(kafkaTemplateMock.send(isA(ProducerRecord.class)))
        .thenReturn(future);

    ListenableFuture<SendResult<Integer, String>> actualListenableFuture = libraryEventProducer
        .sendLibraryEvent_Approach2(libraryEvent);

    SendResult<Integer, String> actualSendResult = actualListenableFuture.get();
    Assertions.assertThat(actualSendResult.getRecordMetadata().partition()).isEqualTo(1);
  }
}