package com.hotel.management.service;

import com.hotel.management.exception.BadRequestException;
import com.hotel.management.exception.ResourceNotFoundException;
import com.hotel.management.model.Room;
import com.hotel.management.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room addRoom(Room room) {
        if (room.getRoomNumber() == null || room.getRoomNumber().trim().isEmpty()) {
            throw new BadRequestException("Room number cannot be empty");
        }
        if (roomRepository.existsByRoomNumber(room.getRoomNumber().trim())) {
            throw new BadRequestException("Room number '" + room.getRoomNumber() + "' already exists");
        }
        if (room.getPricePerNight() == null || room.getPricePerNight() <= 0) {
            throw new BadRequestException("Price per night must be greater than 0");
        }
        if (room.getIsAvailable() == null) {
            room.setIsAvailable(true);
        }
        return roomRepository.save(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByIsAvailable(true);
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + id));
    }

    public Room updateRoom(Long id, Room updatedRoom) {
        Room existingRoom = getRoomById(id);

        if (updatedRoom.getRoomNumber() != null && !updatedRoom.getRoomNumber().trim().isEmpty()) {
            if (!existingRoom.getRoomNumber().equals(updatedRoom.getRoomNumber()) &&
                    roomRepository.existsByRoomNumber(updatedRoom.getRoomNumber().trim())) {
                throw new BadRequestException("Room number '" + updatedRoom.getRoomNumber() + "' is already taken");
            }
            existingRoom.setRoomNumber(updatedRoom.getRoomNumber().trim());
        }

        if (updatedRoom.getRoomType() != null) {
            existingRoom.setRoomType(updatedRoom.getRoomType());
        }

        if (updatedRoom.getPricePerNight() != null) {
            if (updatedRoom.getPricePerNight() <= 0) {
                throw new BadRequestException("Price per night must be greater than 0");
            }
            existingRoom.setPricePerNight(updatedRoom.getPricePerNight());
        }

        if (updatedRoom.getIsAvailable() != null) {
            existingRoom.setIsAvailable(updatedRoom.getIsAvailable());
        }

        if (updatedRoom.getDescription() != null) {
            existingRoom.setDescription(updatedRoom.getDescription());
        }

        return roomRepository.save(existingRoom);
    }

    public void deleteRoom(Long id) {
        Room room = getRoomById(id);
        roomRepository.delete(room);
    }
}
