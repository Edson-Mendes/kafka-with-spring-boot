package com.emendes.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface FailureService {

  void saveFailedRecord(ConsumerRecord<Integer, String> consumerRecord, Exception e, String status);

}
