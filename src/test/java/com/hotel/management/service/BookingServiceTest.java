package com.hotel.management.service;

import com.hotel.management.dto.BookingRequest;
import com.hotel.management.exception.BadRequestException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.model.Booking;
import com.hotel.management.model.Room;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private BookingService bookingService;

    private Room sampleRoom;
    private BookingRequest validRequest;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        sampleRoom = new Room("201", "Deluxe", 100.0, true, "Standard deluxe");
        sampleRoom.setId(1L);

        validRequest = new BookingRequest(
                "Alice Smith",
                "alice@example.com",
                "+1234567890",
                1L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(4) // 3 nights
        );

        sampleBooking = new Booking(
                validRequest.getGuestName(),
                validRequest.getGuestEmail(),
                validRequest.getGuestPhone(),
                validRequest.getCheckInDate(),
                validRequest.getCheckOutDate(),
                300.0,
                "CONFIRMED",
                sampleRoom
        );
        sampleBooking.setId(10L);
    }

    @Test
    @DisplayName("Should successfully create a booking, calculate total price and mark room unavailable")
    void testCreateBooking_Success() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(sampleRoom);
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        Booking created = bookingService.createBooking(validRequest);

        assertNotNull(created);
        assertEquals("CONFIRMED", created.getStatus());
        assertEquals(300.0, created.getTotalPrice());
        assertEquals("Alice Smith", created.getGuestName());
        assertFalse(sampleRoom.getIsAvailable(), "Room should be marked unavailable after booking");

        verify(roomRepository, times(1)).save(sampleRoom);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking with non-existent room ID")
    void testCreateBooking_RoomNotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());
        validRequest.setRoomId(999L);

        assertThrows(ResourceNotFoundException.class, () -> bookingService.createBooking(validRequest));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when booking a room that is already occupied")
    void testCreateBooking_RoomUnavailable() {
        sampleRoom.setIsAvailable(false);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> bookingService.createBooking(validRequest));
        assertTrue(ex.getMessage().contains("currently occupied or unavailable"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when check-out date is before or equal to check-in date")
    void testCreateBooking_InvalidDates() {
        validRequest.setCheckOutDate(validRequest.getCheckInDate()); // Same day

        BadRequestException ex = assertThrows(BadRequestException.class, () -> bookingService.createBooking(validRequest));
        assertTrue(ex.getMessage().contains("must be strictly after"));
    }

    @Test
    @DisplayName("Should throw BadRequestException when required guest fields are missing")
    void testCreateBooking_MissingGuestName() {
        validRequest.setGuestName("");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> bookingService.createBooking(validRequest));
        assertTrue(ex.getMessage().contains("Guest name is required"));
    }

    @Test
    @DisplayName("Should return all bookings")
    void testGetAllBookings() {
        when(bookingRepository.findAll()).thenReturn(List.of(sampleBooking));

        List<Booking> bookings = bookingService.getAllBookings();

        assertEquals(1, bookings.size());
        verify(bookingRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return booking by valid ID")
    void testGetBookingById_Success() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));

        Booking found = bookingService.getBookingById(10L);

        assertNotNull(found);
        assertEquals(10L, found.getId());
        assertEquals("Alice Smith", found.getGuestName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for invalid booking ID")
    void testGetBookingById_NotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingById(999L));
    }

    @Test
    @DisplayName("Should cancel booking successfully and release room back to available")
    void testCancelBooking_Success() {
        sampleRoom.setIsAvailable(false);
        sampleBooking.setStatus("CONFIRMED");

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking cancelled = bookingService.cancelBooking(10L);

        assertNotNull(cancelled);
        assertEquals("CANCELLED", cancelled.getStatus());
        assertTrue(sampleRoom.getIsAvailable(), "Room should be made available again after cancellation");
        verify(roomRepository, times(1)).save(sampleRoom);
        verify(bookingRepository, times(1)).save(sampleBooking);
    }

    @Test
    @DisplayName("Should throw BadRequestException when cancelling an already cancelled booking")
    void testCancelBooking_AlreadyCancelled() {
        sampleBooking.setStatus("CANCELLED");
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(sampleBooking));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> bookingService.cancelBooking(10L));
        assertTrue(ex.getMessage().contains("already cancelled"));
        verify(roomRepository, never()).save(any(Room.class));
    }
}
