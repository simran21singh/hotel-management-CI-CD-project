package com.hotel.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: POST /api/rooms should persist and return 201 Created")
    void testAddRoomEndpoint() throws Exception {
        Room room = new Room("301", "Deluxe", 120.0, true, "City view");

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(room)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.roomNumber").value("301"))
                .andExpect(jsonPath("$.roomType").value("Deluxe"))
                .andExpect(jsonPath("$.pricePerNight").value(120.0))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    @DisplayName("Integration Test: GET /api/rooms should return all stored rooms")
    void testGetAllRoomsEndpoint() throws Exception {
        roomRepository.save(new Room("302", "Single", 70.0, true, "Standard single"));
        roomRepository.save(new Room("303", "Double", 100.0, true, "Standard double"));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].roomNumber").value("302"))
                .andExpect(jsonPath("$[1].roomNumber").value("303"));
    }

    @Test
    @DisplayName("Integration Test: GET /api/rooms/{id} returns 404 when ID does not exist")
    void testGetRoomById_NotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("Room not found with ID: 999")));
    }

    @Test
    @DisplayName("Integration Test: PUT /api/rooms/{id} updates room details")
    void testUpdateRoomEndpoint() throws Exception {
        Room original = roomRepository.save(new Room("304", "Standard", 80.0, true, "Original"));

        Room update = new Room("304", "Suite", 180.0, false, "Upgraded to Suite");

        mockMvc.perform(put("/api/rooms/" + original.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomType").value("Suite"))
                .andExpect(jsonPath("$.pricePerNight").value(180.0))
                .andExpect(jsonPath("$.isAvailable").value(false));
    }

    @Test
    @DisplayName("Integration Test: DELETE /api/rooms/{id} deletes room and returns confirmation")
    void testDeleteRoomEndpoint() throws Exception {
        Room room = roomRepository.save(new Room("305", "Economy", 50.0, true, "Economy room"));

        mockMvc.perform(delete("/api/rooms/" + room.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }
}
