package com.hotel.management.service;

import com.hotel.management.exception.BadRequestException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.model.Room;
import com.hotel.management.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("101", "Deluxe", 150.0, true, "Sea view room");
        sampleRoom.setId(1L);
    }

    @Test
    @DisplayName("Should successfully add a new room")
    void testAddRoom_Success() {
        when(roomRepository.existsByRoomNumber("101")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);

        Room savedRoom = roomService.addRoom(sampleRoom);

        assertNotNull(savedRoom);
        assertEquals("101", savedRoom.getRoomNumber());
        assertEquals("Deluxe", savedRoom.getRoomType());
        assertTrue(savedRoom.getIsAvailable());
        verify(roomRepository, times(1)).save(sampleRoom);
    }

    @Test
    @DisplayName("Should throw BadRequestException when adding room with duplicate room number")
    void testAddRoom_DuplicateRoomNumber() {
        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> roomService.addRoom(sampleRoom));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when adding room with invalid price")
    void testAddRoom_InvalidPrice() {
        sampleRoom.setPricePerNight(0.0);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> roomService.addRoom(sampleRoom));
        assertTrue(ex.getMessage().contains("must be greater than 0"));
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when adding room with empty room number")
    void testAddRoom_EmptyRoomNumber() {
        sampleRoom.setRoomNumber("  ");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> roomService.addRoom(sampleRoom));
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    @DisplayName("Should return all rooms")
    void testGetAllRooms() {
        Room room2 = new Room("102", "Single", 80.0, true, "Standard");
        room2.setId(2L);

        when(roomRepository.findAll()).thenReturn(Arrays.asList(sampleRoom, room2));

        List<Room> rooms = roomService.getAllRooms();

        assertEquals(2, rooms.size());
        verify(roomRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return only available rooms")
    void testGetAvailableRooms() {
        when(roomRepository.findByIsAvailable(true)).thenReturn(List.of(sampleRoom));

        List<Room> availableRooms = roomService.getAvailableRooms();

        assertEquals(1, availableRooms.size());
        assertTrue(availableRooms.get(0).getIsAvailable());
        verify(roomRepository, times(1)).findByIsAvailable(true);
    }

    @Test
    @DisplayName("Should return room when valid ID is provided")
    void testGetRoomById_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));

        Room foundRoom = roomService.getRoomById(1L);

        assertNotNull(foundRoom);
        assertEquals(1L, foundRoom.getId());
        assertEquals("101", foundRoom.getRoomNumber());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when room ID is invalid")
    void testGetRoomById_NotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.getRoomById(999L));
    }

    @Test
    @DisplayName("Should update room details successfully")
    void testUpdateRoom_Success() {
        Room updateDetails = new Room("101", "Executive Suite", 250.0, false, "Renovated suite");

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Room updated = roomService.updateRoom(1L, updateDetails);

        assertNotNull(updated);
        assertEquals("Executive Suite", updated.getRoomType());
        assertEquals(250.0, updated.getPricePerNight());
        assertFalse(updated.getIsAvailable());
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    @DisplayName("Should delete an existing room")
    void testDeleteRoom_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        doNothing().when(roomRepository).delete(sampleRoom);

        assertDoesNotThrow(() -> roomService.deleteRoom(1L));
        verify(roomRepository, times(1)).delete(sampleRoom);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent room")
    void testDeleteRoom_NotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roomService.deleteRoom(999L));
        verify(roomRepository, never()).delete(any(Room.class));
    }
}
