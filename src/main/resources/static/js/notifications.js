/**
 * VNR VJIET Canteen - Student Live Notification System
 * Features: Real-time status polling, Web Audio chime, floating Bootstrap toast,
 * HTML5 browser notifications, and live navbar unread counter badge.
 */

(function () {
    let seenNotifIds = new Set();
    let isInitialized = false;
    let audioContext = null;

    // Pleasant Web Audio chime (no external audio files needed)
    function playChimeSound(type) {
        try {
            if (!audioContext) {
                audioContext = new (window.AudioContext || window.webkitAudioContext)();
            }
            if (audioContext.state === 'suspended') {
                audioContext.resume();
            }

            const ctx = audioContext;
            const now = ctx.currentTime;

            // Two-tone cheerful chime for food ready / updates
            const osc1 = ctx.createOscillator();
            const gain1 = ctx.createGain();
            osc1.type = 'sine';
            osc1.frequency.setValueAtTime(type === 'READY' ? 659.25 : 523.25, now); // E5 or C5
            gain1.gain.setValueAtTime(0.25, now);
            gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
            osc1.connect(gain1);
            gain1.connect(ctx.destination);
            osc1.start(now);
            osc1.stop(now + 0.35);

            const osc2 = ctx.createOscillator();
            const gain2 = ctx.createGain();
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(type === 'READY' ? 880.00 : 783.99, now + 0.15); // A5 or G5
            gain2.gain.setValueAtTime(0.3, now + 0.15);
            gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
            osc2.connect(gain2);
            gain2.connect(ctx.destination);
            osc2.start(now + 0.15);
            osc2.stop(now + 0.6);

            // Mobile haptic vibration feedback like Swiggy
            if (navigator.vibrate) {
                navigator.vibrate(type === 'READY' ? [300, 100, 300, 100, 400] : [150, 50, 150]);
            }
        } catch (e) {
            console.debug('Audio chime skipped:', e);
        }
    }

    // Request browser notification permission
    window.requestNotificationPermission = function () {
        if ('Notification' in window && Notification.permission !== 'granted' && Notification.permission !== 'denied') {
            Notification.requestPermission().then(permission => {
                if (permission === 'granted') {
                    showToastAlert('Notifications Enabled', 'You will now receive instant food readiness updates even when this tab is hidden.', 'INFO');
                }
            });
        }
    };

    // Trigger browser notification
    function triggerSystemNotification(notif) {
        if ('Notification' in window && Notification.permission === 'granted') {
            try {
                const n = new Notification('VNR VJIET Canteen: ' + notif.title, {
                    body: notif.message,
                    icon: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=128',
                    badge: 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=64',
                    tag: 'order-' + notif.orderNumber
                });
                n.onclick = function () {
                    window.focus();
                    if (notif.orderNumber) {
                        window.location.href = '/student/track/' + notif.orderNumber;
                    }
                };
            } catch (e) {
                console.debug('System notification error:', e);
            }
        }
    }

    // Render floating toast alert
    function showToastAlert(title, message, type, orderNumber, tokenNumber) {
        let container = document.getElementById('toastContainer');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toastContainer';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '9999';
            document.body.appendChild(container);
        }

        let bgClass = 'bg-white text-dark border-primary';
        let icon = 'bi-info-circle-fill text-primary';
        if (type === 'READY') {
            bgClass = 'bg-success text-white border-success';
            icon = 'bi-bell-fill text-warning';
        } else if (type === 'PREPARING') {
            bgClass = 'bg-primary text-white border-primary';
            icon = 'bi-fire text-warning';
        } else if (type === 'DELIVERED') {
            bgClass = 'bg-dark text-white border-secondary';
            icon = 'bi-check-all text-success';
        } else if (type === 'CANCELLED') {
            bgClass = 'bg-danger text-white border-danger';
            icon = 'bi-x-circle-fill text-white';
        }

        const toastId = 'toast-' + Date.now();
        const toastEl = document.createElement('div');
        toastEl.id = toastId;
        toastEl.className = `toast align-items-center shadow-lg rounded-4 mb-2 ${bgClass}`;
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');

        let trackBtnHtml = '';
        if (orderNumber) {
            trackBtnHtml = `<div class="mt-2 pt-2 border-top border-white border-opacity-25">
                <a href="/student/track/${orderNumber}" class="btn btn-sm btn-light rounded-pill px-3 py-1 fw-bold text-dark">
                    <i class="bi bi-eye me-1"></i> View Token #${tokenNumber || ''}
                </a>
            </div>`;
        }

        toastEl.innerHTML = `
            <div class="toast-header ${type === 'READY' || type === 'PREPARING' || type === 'DELIVERED' || type === 'CANCELLED' ? 'bg-transparent text-white border-0' : ''}">
                <i class="bi ${icon} me-2 fs-5"></i>
                <strong class="me-auto">${title}</strong>
                <small class="text-white-50">Just now</small>
                <button type="button" class="btn-close ${type === 'READY' || type === 'PREPARING' || type === 'DELIVERED' || type === 'CANCELLED' ? 'btn-close-white' : ''}" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body pt-0">
                ${message}
                ${trackBtnHtml}
            </div>
        `;

        container.appendChild(toastEl);
        if (window.bootstrap && window.bootstrap.Toast) {
            const bsToast = new bootstrap.Toast(toastEl, { delay: 8000 });
            bsToast.show();
        }
    }

    // Update Notification Dropdown & Bell Badge
    function renderNotifications(data) {
        const badge = document.getElementById('notifBadge');
        const list = document.getElementById('notifDropdownList');
        const unreadCount = data.unreadCount || 0;
        const notifications = data.notifications || [];

        // Update badge
        if (badge) {
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 9 ? '9+' : unreadCount;
                badge.classList.remove('d-none');
            } else {
                badge.classList.add('d-none');
            }
        }

        // Update dropdown list
        if (list) {
            if (notifications.length === 0) {
                list.innerHTML = `
                    <div class="text-center py-4 text-muted small">
                        <i class="bi bi-bell-slash fs-3 d-block mb-1 text-secondary opacity-50"></i>
                        No notifications yet.
                    </div>`;
            } else {
                let html = '';
                notifications.forEach(n => {
                    let typeBadge = '';
                    let itemBg = n.isRead ? '' : 'bg-danger-subtle bg-opacity-25';
                    if (n.type === 'READY') {
                        typeBadge = '<span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill">READY</span>';
                    } else if (n.type === 'PREPARING') {
                        typeBadge = '<span class="badge bg-warning-subtle text-warning border border-warning-subtle rounded-pill">PREPARING</span>';
                    } else if (n.type === 'DELIVERED') {
                        typeBadge = '<span class="badge bg-secondary-subtle text-secondary rounded-pill">DELIVERED</span>';
                    } else if (n.type === 'CANCELLED') {
                        typeBadge = '<span class="badge bg-danger-subtle text-danger rounded-pill">CANCELLED</span>';
                    } else {
                        typeBadge = '<span class="badge bg-primary-subtle text-primary rounded-pill">ORDER</span>';
                    }

                    let linkUrl = n.orderNumber ? `/student/track/${n.orderNumber}` : '#';

                    html += `
                        <a href="${linkUrl}" class="dropdown-item p-2 rounded-3 mb-1 text-wrap d-flex align-items-start gap-2 border-bottom border-light ${itemBg}" style="font-size: 0.85rem;">
                            <div class="mt-1">
                                <i class="bi ${n.type === 'READY' ? 'bi-bell-fill text-success fs-5' : (n.type === 'PREPARING' ? 'bi-fire text-warning fs-5' : 'bi-info-circle text-primary fs-5')}"></i>
                            </div>
                            <div class="flex-grow-1">
                                <div class="d-flex justify-content-between align-items-center mb-1">
                                    <strong class="text-dark">${n.title}</strong>
                                    ${typeBadge}
                                </div>
                                <p class="text-muted mb-1 small lh-sm">${n.message}</p>
                                <div class="d-flex justify-content-between align-items-center text-muted" style="font-size: 0.72rem;">
                                    <span>${n.time || ''}</span>
                                    ${n.orderNumber ? `<span class="fw-semibold text-danger">Token #${n.tokenNumber || ''}</span>` : ''}
                                </div>
                            </div>
                        </a>
                    `;
                });
                list.innerHTML = html;
            }
        }

        // Process newly arrived notifications for toasts and chime
        if (isInitialized) {
            notifications.forEach(n => {
                if (!seenNotifIds.has(n.id) && !n.isRead) {
                    seenNotifIds.add(n.id);
                    playChimeSound(n.type);
                    showToastAlert(n.title, n.message, n.type, n.orderNumber, n.tokenNumber);
                    triggerSystemNotification(n);
                }
            });
        } else {
            // First run, populate seen set
            notifications.forEach(n => seenNotifIds.add(n.id));
            isInitialized = true;
        }
    }

    // Poller function
    function pollNotifications() {
        const rollParam = window.currentStudentRollNo ? `?rollNo=${encodeURIComponent(window.currentStudentRollNo)}` : '';
        fetch(`/api/notifications/latest${rollParam}`)
            .then(res => {
                if (!res.ok) return null;
                return res.json();
            })
            .then(data => {
                if (data) {
                    renderNotifications(data);
                }
            })
            .catch(err => {
                console.debug('Notification poll error:', err);
            });
    }

    // Mark all as read
    window.markAllNotificationsRead = function () {
        const rollParam = window.currentStudentRollNo ? `?rollNo=${encodeURIComponent(window.currentStudentRollNo)}` : '';
        fetch(`/api/notifications/mark-all-read${rollParam}`, { method: 'POST' })
            .then(() => {
                pollNotifications();
            })
            .catch(err => console.debug(err));
    };

    // Initialize upon page load
    document.addEventListener('DOMContentLoaded', () => {
        // Look for student roll number in body or metadata
        const metaRoll = document.querySelector('meta[name="student-roll-no"]');
        if (metaRoll && metaRoll.content) {
            window.currentStudentRollNo = metaRoll.content.trim();
        }

        // Check localStorage if not in meta
        if (!window.currentStudentRollNo) {
            const savedRoll = localStorage.getItem('vnr_canteen_roll_no');
            if (savedRoll) {
                window.currentStudentRollNo = savedRoll;
            }
        }

        // Poll immediately and then every 4 seconds
        pollNotifications();
        setInterval(pollNotifications, 4000);

        // Prime audio context on first user click anywhere
        document.body.addEventListener('click', () => {
            if (!audioContext) {
                try {
                    audioContext = new (window.AudioContext || window.webkitAudioContext)();
                } catch(e) {}
            }
        }, { once: true });
    });
})();
