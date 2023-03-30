package com.emendes.unit.controller;

import com.emendes.controller.LibraryEventsController;
import com.emendes.domain.Book;
import com.emendes.domain.LibraryEvent;
import com.emendes.producer.LibraryEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LibraryEventsController.class)
@AutoConfigureMockMvc
class LibraryEventsControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private LibraryEventProducer libraryEventProducerMock;
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("Must return status 201 when post successfully")
  void postLibraryEvent() throws Exception {
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

    String content = mapper.writeValueAsString(libraryEvent);

    doNothing().when(libraryEventProducerMock).sendLibraryEvent_Approach2(any(LibraryEvent.class));

    mockMvc.perform(post("/v1/libraryevents")
        .content(content)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(status().isCreated());
  }

  @Test
  @DisplayName("Must return status 400 when payload is invalid")
  void postInvalidLibraryEvent() throws Exception {
    // given

    LibraryEvent libraryEvent = LibraryEvent.builder()
        .libraryEventId(null)
        .book(null)
        .build();

    String content = mapper.writeValueAsString(libraryEvent);

    doNothing().when(libraryEventProducerMock).sendLibraryEvent_Approach2(any(LibraryEvent.class));

    mockMvc.perform(post("/v1/libraryevents")
        .content(content)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Must return status 400 when book data is invalid")
  void postMustReturn400WhenBookIsInvalid() throws Exception {
    // given
    LibraryEvent libraryEvent = LibraryEvent.builder()
        .libraryEventId(null)
        .book(new Book())
        .build();

    String content = mapper.writeValueAsString(libraryEvent);

    doNothing().when(libraryEventProducerMock).sendLibraryEvent_Approach2(any(LibraryEvent.class));

    String expectedErrorMessage =
        "book.bookAuthor - must not be blank, book.bookName - must not be blank, book.bookId - must not be null";

    mockMvc.perform(post("/v1/libraryevents")
        .content(content)
        .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(status().isBadRequest()).andExpect(content().string(expectedErrorMessage));
  }
}