package com.emendes.service.impl;

import com.emendes.entity.LibraryEvent;
import com.emendes.repository.LibraryEventRepository;
import com.emendes.service.LibraryEventService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class LibraryEventServiceImpl implements LibraryEventService {

  private final ObjectMapper mapper;
  private final LibraryEventRepository libraryEventRepository;

  @Override
  public void processLibraryEvent(ConsumerRecord<Integer, String> consumerRecord) {
    try {
      LibraryEvent libraryEvent = mapper.readValue(consumerRecord.value(), LibraryEvent.class);
      log.info("LibraryEvent : {}", libraryEvent);

      switch (libraryEvent.getLibraryEventType()) {
        case NEW -> save(libraryEvent);
        case UPDATE -> {
        } // update opetation;
        default -> {
          log.error("Invalid Library Event Type");
        }
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private void save(LibraryEvent libraryEvent) {
    libraryEvent.getBook().setLibraryEvent(libraryEvent);
    libraryEventRepository.save(libraryEvent);
    log.info("Successfully persist LibraryEvent : {}", libraryEvent);
  }

}
