package com.emendes.consumer;

import com.emendes.service.LibraryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class LibraryEventsConsumer {

  private final LibraryEventService libraryEventService;

  @KafkaListener(topics = {"library-events"})
  public void onMessage(ConsumerRecord<Integer, String> consumerRecord) {
    log.info("ConsumerRecord : {}", consumerRecord);

    libraryEventService.processLibraryEvent(consumerRecord);
  }

}
