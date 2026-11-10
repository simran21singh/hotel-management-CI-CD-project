-- ===================================================================
-- MySQL Database Initialization & Seed Data
-- Grand Horizon Hotel Management System - Academic DevOps Project
-- ===================================================================

CREATE DATABASE IF NOT EXISTS hotel_db;
USE hotel_db;

-- -------------------------------------------------------------------
-- Table Structure: rooms
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(50) NOT NULL UNIQUE,
    room_type VARCHAR(100) NOT NULL,
    price_per_night DOUBLE NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------
-- Table Structure: bookings
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guest_name VARCHAR(150) NOT NULL,
    guest_email VARCHAR(150) NOT NULL,
    guest_phone VARCHAR(50) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price DOUBLE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    room_id BIGINT NOT NULL,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------------
-- Seed Sample Rooms (Provides instant demo data for college viva)
-- -------------------------------------------------------------------
INSERT INTO rooms (room_number, room_type, price_per_night, is_available, description)
VALUES 
    ('101', 'Single Standard', 75.00, TRUE, 'Cozy single room with high-speed Wi-Fi, workspace desk, and city view.'),
    ('102', 'Double Standard', 110.00, TRUE, 'Comfortable queen-bed room with smart TV, coffee maker, and modern bathroom.'),
    ('201', 'Deluxe King', 160.00, TRUE, 'Spacious king-bed room with private balcony, luxury bedding, and minibar.'),
    ('202', 'Executive Suite', 240.00, TRUE, 'Two-room suite featuring living lounge, scenic view, and complimentary breakfast.'),
    ('301', 'Presidential Suite', 450.00, TRUE, 'Top-floor penthouse suite with panoramic view, jacuzzi, and dedicated concierge.')
ON DUPLICATE KEY UPDATE room_number = room_number;
