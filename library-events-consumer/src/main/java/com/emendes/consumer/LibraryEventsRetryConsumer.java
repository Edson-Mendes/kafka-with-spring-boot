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
public class LibraryEventsRetryConsumer {

  private final LibraryEventService libraryEventService;

  @KafkaListener(
      autoStartup = "${retryListener.startup:true}",
      topics = {"${topics.retry}"},
      groupId = "retry-listener-group")
  public void onMessage(ConsumerRecord<Integer, String> consumerRecord) {
    log.info("ConsumerRecord in RETRY Consumer: {}", consumerRecord);

    libraryEventService.processLibraryEvent(consumerRecord);
  }

}
