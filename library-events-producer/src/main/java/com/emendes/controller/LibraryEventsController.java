package com.emendes.controller;

import com.emendes.domain.LibraryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@Slf4j
public class LibraryEventsController {

  private final Random random = new Random();

  @PostMapping("/v1/libraryevents")
  public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent) {

    // Invoke Kafka Producer
    log.info("A LibraryEvent was created!");
    libraryEvent.setLibraryEventId(random.nextInt(100, 1000));
    return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
  }

}
