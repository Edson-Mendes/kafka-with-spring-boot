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

import java.util.Optional;

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
          // validate libraryevent
          validate(libraryEvent);
          save(libraryEvent);
          // save libraryevent
        }
        default -> {
          log.error("Invalid Library Event Type");
        }
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private void validate(LibraryEvent libraryEvent) {
    if (libraryEvent.getLibraryEventId() == null)
      throw new IllegalArgumentException("LibraryEvent#libraryEventId is missing");

    Optional<LibraryEvent> libraryEventOptional = libraryEventRepository.findById(libraryEvent.getLibraryEventId());

    if (libraryEventOptional.isEmpty())
      throw new IllegalArgumentException("Not a valid LibraryEvent");

    log.info("Validation is successful for the LibraryEvent : {}", libraryEventOptional.get());
  }

  private void save(LibraryEvent libraryEvent) {
    libraryEvent.getBook().setLibraryEvent(libraryEvent);
    libraryEventRepository.save(libraryEvent);
    log.info("Successfully persist LibraryEvent : {}", libraryEvent);
  }

}
