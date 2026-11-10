/**
 * Grand Horizon Hotel - Frontend Application Controller
 * Handles UI interactions, rendering, state, and form submissions.
 */

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

let cachedRooms = [];
let cachedBookings = [];

function initApp() {
    setupNavigation();
    setupMobileMenu();
    setupModals();
    setupForms();
    checkSystemHealth();
    setInterval(checkSystemHealth, 30000);

    // Initial load
    loadDashboard();
}

/* ===================================================================
   Navigation & View Switching
   =================================================================== */
function setupNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const viewTarget = item.getAttribute('data-view');
            switchView(viewTarget);

            // On mobile, close sidebar after clicking
            const sidebar = document.querySelector('.sidebar');
            if (sidebar.classList.contains('open')) {
                sidebar.classList.remove('open');
            }
        });
    });
}

function switchView(viewName) {
    // Update active nav link
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.getAttribute('data-view') === viewName);
    });

    // Update visible view section
    document.querySelectorAll('.view-section').forEach(sec => {
        sec.classList.remove('active');
    });

    const targetSection = document.getElementById(`view-${viewName}`);
    if (targetSection) {
        targetSection.classList.add('active');
    }

    // Update header title
    const titleMap = {
        'dashboard': 'Dashboard Overview',
        'rooms': 'Rooms Management',
        'bookings': 'Bookings Management',
        'new-booking': 'Create Reservation'
    };
    document.getElementById('current-page-title').textContent = titleMap[viewName] || 'Dashboard';

    // Trigger view-specific data refresh
    if (viewName === 'dashboard') loadDashboard();
    if (viewName === 'rooms') loadRooms();
    if (viewName === 'bookings') loadBookings();
    if (viewName === 'new-booking') prepareNewBookingForm();
}

function setupMobileMenu() {
    const toggleBtn = document.getElementById('menu-toggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }
}

/* ===================================================================
   Health Check Status
   =================================================================== */
async function checkSystemHealth() {
    const badge = document.getElementById('system-status-badge');
    const health = await API.checkHealth();
    if (health && health.status === 'UP') {
        badge.innerHTML = `<span class="status-dot"></span> System Online`;
        badge.style.color = 'var(--success)';
        badge.style.background = 'var(--success-soft)';
    } else {
        badge.innerHTML = `<span class="status-dot" style="background:var(--danger)"></span> Offline / Error`;
        badge.style.color = 'var(--danger)';
        badge.style.background = 'var(--danger-soft)';
    }
}

/* ===================================================================
   Dashboard View
   =================================================================== */
async function loadDashboard() {
    try {
        const [rooms, bookings] = await Promise.all([
            API.getRooms(),
            API.getBookings()
        ]);

        cachedRooms = rooms || [];
        cachedBookings = bookings || [];

        const totalRooms = cachedRooms.length;
        const availableRooms = cachedRooms.filter(r => r.isAvailable).length;
        const occupiedRooms = totalRooms - availableRooms;
        const totalBookings = cachedBookings.length;

        document.getElementById('metric-total-rooms').textContent = totalRooms;
        document.getElementById('metric-available-rooms').textContent = availableRooms;
        document.getElementById('metric-occupied-rooms').textContent = occupiedRooms;
        document.getElementById('metric-total-bookings').textContent = totalBookings;

        renderRecentBookings(cachedBookings.slice(-5).reverse());
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderRecentBookings(bookings) {
    const container = document.getElementById('recent-bookings-tbody');
    if (!container) return;

    if (!bookings || bookings.length === 0) {
        container.innerHTML = `
            <tr>
                <td colspan="6" class="empty-state">
                    <div class="empty-state-icon">📅</div>
                    <h4>No Bookings Yet</h4>
                    <p>Start by creating a new guest reservation.</p>
                </td>
            </tr>`;
        return;
    }

    container.innerHTML = bookings.map(b => `
        <tr>
            <td><strong>#${b.id}</strong></td>
            <td>${escapeHtml(b.guestName)}</td>
            <td>Room ${b.room ? b.room.roomNumber : 'N/A'} (${b.room ? b.room.roomType : ''})</td>
            <td>${formatDate(b.checkInDate)} &rarr; ${formatDate(b.checkOutDate)}</td>
            <td><strong>$${b.totalPrice ? b.totalPrice.toFixed(2) : '0.00'}</strong></td>
            <td>${getStatusBadge(b.status)}</td>
        </tr>
    `).join('');
}

/* ===================================================================
   Rooms View
   =================================================================== */
async function loadRooms() {
    const tbody = document.getElementById('rooms-table-tbody');
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state"><div class="loading-spinner"></div><p style="margin-top:10px;">Loading rooms...</p></td></tr>`;

    try {
        cachedRooms = await API.getRooms();
        filterAndRenderRooms();
    } catch (err) {
        showToast(err.message, 'error');
        tbody.innerHTML = `<tr><td colspan="6" class="empty-state" style="color:var(--danger)">Failed to load rooms: ${escapeHtml(err.message)}</td></tr>`;
    }
}

function filterAndRenderRooms() {
    const filter = document.getElementById('room-status-filter').value;
    const tbody = document.getElementById('rooms-table-tbody');

    let filtered = cachedRooms;
    if (filter === 'available') {
        filtered = cachedRooms.filter(r => r.isAvailable);
    } else if (filter === 'occupied') {
        filtered = cachedRooms.filter(r => !r.isAvailable);
    }

    if (!filtered || filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="empty-state">
                    <div class="empty-state-icon">🛏️</div>
                    <h4>No Rooms Found</h4>
                    <p>Add a new room or change your filter.</p>
                </td>
            </tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(r => `
        <tr>
            <td><strong>Room ${escapeHtml(r.roomNumber)}</strong></td>
            <td>${escapeHtml(r.roomType)}</td>
            <td><strong>$${r.pricePerNight ? r.pricePerNight.toFixed(2) : '0.00'}</strong> / night</td>
            <td>${r.isAvailable 
                ? '<span class="badge badge-success">&#10003; Available</span>' 
                : '<span class="badge badge-danger">&#10007; Occupied</span>'}</td>
            <td>${escapeHtml(r.description || 'No description')}</td>
            <td>
                <div style="display:flex; gap:8px;">
                    <button class="btn btn-secondary btn-sm" onclick="openEditRoomModal(${r.id})">&#9998; Edit</button>
                    <button class="btn btn-danger-outline btn-sm" onclick="handleDeleteRoom(${r.id}, '${escapeHtml(r.roomNumber)}')">&#128465; Delete</button>
                </div>
            </td>
        </tr>
    `).join('');
}

window.handleDeleteRoom = async function(id, roomNumber) {
    if (!confirm(`Are you sure you want to delete Room ${roomNumber}? This cannot be undone.`)) {
        return;
    }

    try {
        await API.deleteRoom(id);
        showToast(`Room ${roomNumber} deleted successfully`, 'success');
        loadRooms();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

/* ===================================================================
   Bookings View
   =================================================================== */
async function loadBookings() {
    const tbody = document.getElementById('bookings-table-tbody');
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state"><div class="loading-spinner"></div><p style="margin-top:10px;">Loading bookings...</p></td></tr>`;

    try {
        cachedBookings = await API.getBookings();
        filterAndRenderBookings();
    } catch (err) {
        showToast(err.message, 'error');
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state" style="color:var(--danger)">Failed to load bookings: ${escapeHtml(err.message)}</td></tr>`;
    }
}

function filterAndRenderBookings() {
    const filter = document.getElementById('booking-status-filter').value;
    const tbody = document.getElementById('bookings-table-tbody');

    let filtered = cachedBookings;
    if (filter !== 'all') {
        filtered = cachedBookings.filter(b => b.status && b.status.toUpperCase() === filter.toUpperCase());
    }

    if (!filtered || filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    <div class="empty-state-icon">📋</div>
                    <h4>No Bookings Found</h4>
                    <p>There are no bookings matching the selected filter.</p>
                </td>
            </tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(b => `
        <tr>
            <td><strong>#${b.id}</strong></td>
            <td>
                <strong>${escapeHtml(b.guestName)}</strong><br>
                <small style="color:var(--text-secondary)">${escapeHtml(b.guestEmail)} | ${escapeHtml(b.guestPhone)}</small>
            </td>
            <td>Room ${b.room ? escapeHtml(b.room.roomNumber) : 'N/A'} <br><small style="color:var(--text-secondary)">${b.room ? escapeHtml(b.room.roomType) : ''}</small></td>
            <td>${formatDate(b.checkInDate)} &rarr; ${formatDate(b.checkOutDate)}</td>
            <td><strong>$${b.totalPrice ? b.totalPrice.toFixed(2) : '0.00'}</strong></td>
            <td>${getStatusBadge(b.status)}</td>
            <td>
                ${b.status === 'CONFIRMED' 
                    ? `<button class="btn btn-danger-outline btn-sm" onclick="handleCancelBooking(${b.id})">&#10006; Cancel</button>` 
                    : '<span style="color:var(--text-muted); font-size:12px;">Completed / Cancelled</span>'}
            </td>
        </tr>
    `).join('');
}

window.handleCancelBooking = async function(id) {
    if (!confirm(`Are you sure you want to cancel Booking #${id}? The assigned room will be made available again.`)) {
        return;
    }

    try {
        await API.cancelBooking(id);
        showToast(`Booking #${id} cancelled successfully`, 'success');
        loadBookings();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

/* ===================================================================
   New Booking Reservation Form
   =================================================================== */
async function prepareNewBookingForm() {
    const select = document.getElementById('booking-room-select');
    select.innerHTML = `<option value="">Loading available rooms...</option>`;

    try {
        const availableRooms = await API.getRooms(true);
        if (!availableRooms || availableRooms.length === 0) {
            select.innerHTML = `<option value="">No rooms currently available</option>`;
            document.getElementById('booking-submit-btn').disabled = true;
            return;
        }

        document.getElementById('booking-submit-btn').disabled = false;
        select.innerHTML = `<option value="">-- Choose an Available Room --</option>` + 
            availableRooms.map(r => `
                <option value="${r.id}" data-price="${r.pricePerNight}">
                    Room ${escapeHtml(r.roomNumber)} - ${escapeHtml(r.roomType)} ($${r.pricePerNight}/night)
                </option>
            `).join('');

        // Set min dates to today
        const today = new Date().toISOString().split('T')[0];
        const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
        document.getElementById('booking-checkin').min = today;
        document.getElementById('booking-checkout').min = tomorrow;

        if (!document.getElementById('booking-checkin').value) {
            document.getElementById('booking-checkin').value = today;
            document.getElementById('booking-checkout').value = tomorrow;
        }

        updatePriceEstimation();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function updatePriceEstimation() {
    const select = document.getElementById('booking-room-select');
    const checkinVal = document.getElementById('booking-checkin').value;
    const checkoutVal = document.getElementById('booking-checkout').value;
    const estimateSpan = document.getElementById('estimated-total-price');

    const selectedOption = select.options[select.selectedIndex];
    if (!selectedOption || !selectedOption.dataset.price || !checkinVal || !checkoutVal) {
        estimateSpan.textContent = '$0.00';
        return;
    }

    const pricePerNight = parseFloat(selectedOption.dataset.price);
    const d1 = new Date(checkinVal);
    const d2 = new Date(checkoutVal);
    const timeDiff = d2.getTime() - d1.getTime();
    const days = Math.ceil(timeDiff / (1000 * 3600 * 24));

    if (days > 0) {
        const total = days * pricePerNight;
        estimateSpan.textContent = `$${total.toFixed(2)} (${days} ${days === 1 ? 'night' : 'nights'})`;
    } else {
        estimateSpan.textContent = '$0.00 (Check-out must be after Check-in)';
    }
}

/* ===================================================================
   Modals & Forms Handlers
   =================================================================== */
function setupModals() {
    const roomModal = document.getElementById('room-modal');
    const closeBtns = document.querySelectorAll('.modal-close-trigger');
    closeBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            roomModal.classList.remove('active');
        });
    });

    document.getElementById('open-add-room-btn').addEventListener('click', () => {
        document.getElementById('room-modal-title').textContent = 'Add New Room';
        document.getElementById('room-form').reset();
        document.getElementById('room-id-hidden').value = '';
        document.getElementById('room-available').checked = true;
        roomModal.classList.add('active');
    });
}

window.openEditRoomModal = async function(id) {
    try {
        const room = await API.getRoomById(id);
        document.getElementById('room-modal-title').textContent = `Edit Room ${room.roomNumber}`;
        document.getElementById('room-id-hidden').value = room.id;
        document.getElementById('room-number').value = room.roomNumber;
        document.getElementById('room-type').value = room.roomType;
        document.getElementById('room-price').value = room.pricePerNight;
        document.getElementById('room-available').checked = room.isAvailable;
        document.getElementById('room-description').value = room.description || '';

        document.getElementById('room-modal').classList.add('active');
    } catch (err) {
        showToast(err.message, 'error');
    }
};

function setupForms() {
    // Room Form Submit (Create or Update)
    const roomForm = document.getElementById('room-form');
    roomForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('room-id-hidden').value;
        const roomData = {
            roomNumber: document.getElementById('room-number').value.trim(),
            roomType: document.getElementById('room-type').value,
            pricePerNight: parseFloat(document.getElementById('room-price').value),
            isAvailable: document.getElementById('room-available').checked,
            description: document.getElementById('room-description').value.trim()
        };

        try {
            if (id) {
                await API.updateRoom(id, roomData);
                showToast(`Room ${roomData.roomNumber} updated successfully!`, 'success');
            } else {
                await API.createRoom(roomData);
                showToast(`Room ${roomData.roomNumber} created successfully!`, 'success');
            }
            document.getElementById('room-modal').classList.remove('active');
            loadRooms();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Booking Form Submit
    const bookingForm = document.getElementById('new-booking-form');
    bookingForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const bookingData = {
            guestName: document.getElementById('booking-guest-name').value.trim(),
            guestEmail: document.getElementById('booking-guest-email').value.trim(),
            guestPhone: document.getElementById('booking-guest-phone').value.trim(),
            roomId: parseInt(document.getElementById('booking-room-select').value),
            checkInDate: document.getElementById('booking-checkin').value,
            checkOutDate: document.getElementById('booking-checkout').value
        };

        try {
            const created = await API.createBooking(bookingData);
            showToast(`Reservation created successfully for ${created.guestName}!`, 'success');
            bookingForm.reset();
            switchView('bookings');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Price recalculation triggers
    document.getElementById('booking-room-select').addEventListener('change', updatePriceEstimation);
    document.getElementById('booking-checkin').addEventListener('change', updatePriceEstimation);
    document.getElementById('booking-checkout').addEventListener('change', updatePriceEstimation);

    // Filters
    document.getElementById('room-status-filter').addEventListener('change', filterAndRenderRooms);
    document.getElementById('booking-status-filter').addEventListener('change', filterAndRenderBookings);
}

/* ===================================================================
   Helper Utilities
   ================================================================== */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    const iconMap = {
        'success': '&#10003;',
        'error': '&#10007;',
        'info': '&#8505;'
    };

    toast.innerHTML = `
        <span style="font-weight:700; font-size:16px;">${iconMap[type] || ''}</span>
        <span>${escapeHtml(message)}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function getStatusBadge(status) {
    if (!status) return '';
    const upper = status.toUpperCase();
    if (upper === 'CONFIRMED') {
        return '<span class="badge badge-success">&#10003; Confirmed</span>';
    } else if (upper === 'CANCELLED') {
        return '<span class="badge badge-danger">&#10007; Cancelled</span>';
    }
    return `<span class="badge badge-secondary">${escapeHtml(status)}</span>`;
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
