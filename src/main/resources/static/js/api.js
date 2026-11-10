/**
 * Grand Horizon Hotel - Backend API Service
 * Handles all HTTP requests to Spring Boot REST endpoints using the Fetch API.
 */

const API_BASE_URL = window.location.origin;

const API = {
    // Health check endpoint
    async checkHealth() {
        try {
            const res = await fetch(`${API_BASE_URL}/api/health`);
            if (!res.ok) throw new Error('Health check failed');
            return await res.json();
        } catch (err) {
            console.error('API Error (checkHealth):', err);
            return null;
        }
    },

    // --- Rooms Endpoints ---
    async getRooms(availableOnly = false) {
        const url = availableOnly 
            ? `${API_BASE_URL}/api/rooms?available=true` 
            : `${API_BASE_URL}/api/rooms`;
        const res = await fetch(url);
        return this._handleResponse(res);
    },

    async getRoomById(id) {
        const res = await fetch(`${API_BASE_URL}/api/rooms/${id}`);
        return this._handleResponse(res);
    },

    async createRoom(roomData) {
        const res = await fetch(`${API_BASE_URL}/api/rooms`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(roomData)
        });
        return this._handleResponse(res);
    },

    async updateRoom(id, roomData) {
        const res = await fetch(`${API_BASE_URL}/api/rooms/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(roomData)
        });
        return this._handleResponse(res);
    },

    async deleteRoom(id) {
        const res = await fetch(`${API_BASE_URL}/api/rooms/${id}`, {
            method: 'DELETE'
        });
        return this._handleResponse(res);
    },

    // --- Bookings Endpoints ---
    async getBookings() {
        const res = await fetch(`${API_BASE_URL}/api/bookings`);
        return this._handleResponse(res);
    },

    async getBookingById(id) {
        const res = await fetch(`${API_BASE_URL}/api/bookings/${id}`);
        return this._handleResponse(res);
    },

    async createBooking(bookingData) {
        const res = await fetch(`${API_BASE_URL}/api/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(bookingData)
        });
        return this._handleResponse(res);
    },

    async cancelBooking(id) {
        const res = await fetch(`${API_BASE_URL}/api/bookings/${id}`, {
            method: 'DELETE'
        });
        return this._handleResponse(res);
    },

    // Helper to process JSON response and bubble up backend exception messages
    async _handleResponse(res) {
        let data = null;
        try {
            data = await res.json();
        } catch (e) {
            // In case of empty body
        }

        if (!res.ok) {
            const errorMessage = (data && data.message) ? data.message : `HTTP Error ${res.status}: ${res.statusText}`;
            const error = new Error(errorMessage);
            error.status = res.status;
            error.data = data;
            throw error;
        }

        return data;
    }
};

window.API = API;
