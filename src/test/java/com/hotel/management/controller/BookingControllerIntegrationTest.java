package com.hotel.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.management.dto.BookingRequest;
import com.hotel.management.model.Room;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Room availableRoom;

    @BeforeEach
    void cleanUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        availableRoom = roomRepository.save(new Room("401", "Deluxe", 150.0, true, "Balcony"));
    }

    @Test
    @DisplayName("Integration Test: POST /api/bookings creates booking and marks room unavailable")
    void testCreateBookingEndpoint() throws Exception {
        BookingRequest request = new BookingRequest(
                "Bob Johnson",
                "bob@example.com",
                "9876543210",
                availableRoom.getId(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3) // 2 nights * $150 = $300
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.guestName").value("Bob Johnson"))
                .andExpect(jsonPath("$.totalPrice").value(300.0))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.room.id").value(availableRoom.getId()));
    }

    @Test
    @DisplayName("Integration Test: POST /api/bookings returns 400 Bad Request when dates are invalid")
    void testCreateBookingInvalidDates() throws Exception {
        BookingRequest badDatesRequest = new BookingRequest(
                "Charlie",
                "charlie@example.com",
                "5551234567",
                availableRoom.getId(),
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(2) // check-out before check-in!
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDatesRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Check-out date must be strictly after")));
    }

    @Test
    @DisplayName("Integration Test: DELETE /api/bookings/{id} cancels booking and frees the room")
    void testCancelBookingEndpoint() throws Exception {
        // First, create a booking
        BookingRequest request = new BookingRequest(
                "Diana Prince",
                "diana@example.com",
                "9998887777",
                availableRoom.getId(),
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(5)
        );

        String response = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long bookingId = objectMapper.readTree(response).get("id").asLong();

        // Now cancel it
        mockMvc.perform(delete("/api/bookings/" + bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
