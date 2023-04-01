package com.emendes.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface LibraryEventService {

  public void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord);

}
