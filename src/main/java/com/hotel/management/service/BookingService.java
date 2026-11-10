package com.hotel.management.service;

import com.hotel.management.dto.BookingRequest;
import com.hotel.management.exception.BadRequestException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.model.Booking;
import com.hotel.management.model.Room;
import com.hotel.management.repository.BookingRepository;
import com.hotel.management.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Booking createBooking(BookingRequest request) {
        // Validate guest details
        if (request.getGuestName() == null || request.getGuestName().trim().isEmpty()) {
            throw new BadRequestException("Guest name is required");
        }
        if (request.getGuestEmail() == null || request.getGuestEmail().trim().isEmpty()) {
            throw new BadRequestException("Guest email is required");
        }
        if (request.getGuestPhone() == null || request.getGuestPhone().trim().isEmpty()) {
            throw new BadRequestException("Guest phone number is required");
        }
        if (request.getRoomId() == null) {
            throw new BadRequestException("Room ID is required");
        }
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new BadRequestException("Check-in and check-out dates are required");
        }
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new BadRequestException("Check-out date must be strictly after check-in date");
        }

        // Fetch Room
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + request.getRoomId()));

        // Check availability
        if (Boolean.FALSE.equals(room.getIsAvailable())) {
            throw new BadRequestException("Room '" + room.getRoomNumber() + "' is currently occupied or unavailable");
        }

        // Calculate total stay duration & price
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            nights = 1;
        }
        double totalPrice = nights * room.getPricePerNight();

        // Mark room as unavailable
        room.setIsAvailable(false);
        roomRepository.save(room);

        // Create and persist booking
        Booking booking = new Booking(
                request.getGuestName().trim(),
                request.getGuestEmail().trim(),
                request.getGuestPhone().trim(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                totalPrice,
                "CONFIRMED",
                room
        );

        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
    }

    @Transactional
    public Booking cancelBooking(Long id) {
        Booking booking = getBookingById(id);

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new BadRequestException("Booking with ID " + id + " is already cancelled");
        }

        // Release room back to available
        Room room = booking.getRoom();
        if (room != null) {
            room.setIsAvailable(true);
            roomRepository.save(room);
        }

        booking.setStatus("CANCELLED");
        return bookingRepository.save(booking);
    }
}
