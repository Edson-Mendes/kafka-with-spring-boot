package com.emendes.producer;

import com.emendes.domain.LibraryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Component
@Slf4j
public class LibraryEventProducer {

  private final KafkaTemplate<Integer, String> kafkaTemplate;
  private final ObjectMapper mapper;
  private final String topic = "library-events";

  public void sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
    Integer key = libraryEvent.getLibraryEventId();
    String value = mapper.writeValueAsString(libraryEvent);

    ListenableFuture<SendResult<Integer, String>> listenableFuture = kafkaTemplate.sendDefault(key, value);
    listenableFuture.addCallback(new ListenableFutureCallback<>() {
      @Override
      public void onFailure(Throwable ex) {
        handleFailure(key, value, ex);
      }

      @Override
      public void onSuccess(SendResult<Integer, String> result) {
        handleSuccess(key, value, result);
      }
    });
  }

  public ListenableFuture<SendResult<Integer, String>> sendLibraryEvent_Approach2(LibraryEvent libraryEvent) throws JsonProcessingException {
    Integer key = libraryEvent.getLibraryEventId();
    String value = mapper.writeValueAsString(libraryEvent);

    ProducerRecord<Integer, String> producerRecord = buildProducerRecord(key, value, topic);

    ListenableFuture<SendResult<Integer, String>> listenableFuture =
            kafkaTemplate.send(producerRecord);
    listenableFuture.addCallback(new ListenableFutureCallback<>() {
      @Override
      public void onFailure(Throwable ex) {
        handleFailure(key, value, ex);
      }

      @Override
      public void onSuccess(SendResult<Integer, String> result) {
        handleSuccess(key, value, result);
      }
    });

    return listenableFuture;
  }

  private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value, String topic) {
    List<Header> headers = List.of(new RecordHeader("event-source", "scanner".getBytes()));

    return new ProducerRecord<>(topic, null, key, value, headers);
  }

  public SendResult<Integer, String> sendLibraryEventSynchronous(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {
    Integer key = libraryEvent.getLibraryEventId();
    String value = mapper.writeValueAsString(libraryEvent);
    SendResult<Integer, String> sendResult = null;

    try {
      sendResult = kafkaTemplate.sendDefault(key, value).get();
    } catch (InterruptedException | ExecutionException ex) {
      log.error("InterruptedException/ExecutionException sending the message and the exception is {}", ex.getMessage());
      throw ex;
    } catch (Exception ex) {
      log.error("Exception sending the message and the exception is {}", ex.getMessage());
      throw ex;
    }

    return sendResult;
  }

  private void handleFailure(Integer key, String value, Throwable ex) {
    log.error("Error sending the message and the exception is {}", ex.getMessage());
    try {
      throw ex;
    } catch (Throwable e) {
      log.error("Error in OnFailure: {}", e.getMessage());
    }
  }

  private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
    log.info("Message sent successfully for the key : {} and the value is {}, partition is {}",
            key, value, result.getRecordMetadata().partition());
  }

}
