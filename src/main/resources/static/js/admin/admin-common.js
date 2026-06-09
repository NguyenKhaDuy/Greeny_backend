(function () {
    const admin = window.GreenyAdmin = window.GreenyAdmin || {};

    function normalizeText(value) {
        return (value || '')
                .toString()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '')
                .replace(/đ/g, 'd')
                .replace(/Đ/g, 'd')
                .trim()
                .toLowerCase();
    }

    function readNumber(value) {
        const text = (value || '').toString().trim();
        if (!text) {
            return null;
        }
        const compactText = text.replace(/\s/g, '');
        const directNumber = Number(compactText);
        if (Number.isFinite(directNumber)) {
            return directNumber;
        }
        const decimalCommaNumber = Number(compactText.replace(',', '.'));
        if (Number.isFinite(decimalCommaNumber)) {
            return decimalCommaNumber;
        }
        const digitOnlyNumber = Number(compactText.replace(/[^\d-]/g, ''));
        return Number.isFinite(digitOnlyNumber) ? digitOnlyNumber : null;
    }

    function closePanels(scope) {
        const root = scope || document;
        root.querySelectorAll('.collapse-panel').forEach((panel) => {
            panel.hidden = true;
        });
    }

    function dateMatches(cardDate, selectedDate) {
        return !selectedDate || cardDate === selectedDate;
    }
	
	function ensureConfirmDialog() {
	    let dialog = document.getElementById('adminConfirmDialog');

	    if (dialog) {
	        return dialog;
	    }

	    const style = document.createElement('style');
	    style.id = 'adminConfirmDialogStyle';
	    style.textContent = `
	        .admin-confirm-backdrop {
	            position: fixed;
	            inset: 0;
	            z-index: 100000;
	            display: grid;
	            place-items: center;
	            background: rgba(12, 18, 15, 0.48);
	            backdrop-filter: blur(8px);
	            padding: 18px;
	            opacity: 0;
	            pointer-events: none;
	            transition: opacity 160ms ease;
	        }

	        .admin-confirm-backdrop.is-open {
	            opacity: 1;
	            pointer-events: auto;
	        }

	        .admin-confirm-dialog {
	            width: min(460px, 100%);
	            transform: translateY(10px) scale(0.98);
	            border: 1px solid rgba(169, 67, 63, 0.18);
	            border-radius: 22px;
	            background:
	                radial-gradient(circle at 0 0, rgba(169, 67, 63, 0.10), transparent 38%),
	                linear-gradient(180deg, rgba(255,255,255,.98), rgba(250,253,251,.96)),
	                #fff;
	            box-shadow: 0 28px 80px rgba(16, 24, 20, 0.28);
	            padding: 18px;
	            transition: transform 180ms ease;
	        }

	        .admin-confirm-backdrop.is-open .admin-confirm-dialog {
	            transform: translateY(0) scale(1);
	        }

	        .admin-confirm-head {
	            display: flex;
	            align-items: flex-start;
	            gap: 13px;
	        }

	        .admin-confirm-icon {
	            display: inline-grid;
	            flex: 0 0 44px;
	            width: 44px;
	            height: 44px;
	            place-items: center;
	            border-radius: 16px;
	            background: var(--red-100);
	            color: var(--red-700);
	            font-size: 22px;
	            font-weight: 950;
	        }

	        .admin-confirm-copy {
	            min-width: 0;
	        }

	        .admin-confirm-copy h3 {
	            margin: 0;
	            color: var(--ink);
	            font-size: 20px;
	            line-height: 1.18;
	            letter-spacing: -0.03em;
	        }

	        .admin-confirm-copy p {
	            margin: 7px 0 0;
	            color: var(--muted);
	            line-height: 1.5;
	        }

	        .admin-confirm-warning {
	            margin: 14px 0 0;
	            border: 1px dashed rgba(169, 67, 63, 0.22);
	            border-radius: 16px;
	            background: rgba(254, 242, 242, 0.78);
	            color: var(--red-700);
	            padding: 11px 12px;
	            font-size: 13px;
	            font-weight: 850;
	            line-height: 1.45;
	        }

	        .admin-confirm-actions {
	            display: flex;
	            justify-content: flex-end;
	            flex-wrap: wrap;
	            gap: 9px;
	            margin-top: 16px;
	        }

	        .admin-confirm-actions button {
	            min-height: 40px;
	            border-radius: 999px;
	            padding: 0 16px;
	            font-weight: 900;
	            cursor: pointer;
	        }

	        .admin-confirm-cancel {
	            border: 1px solid var(--line);
	            background: #fff;
	            color: var(--ink-soft);
	        }

	        .admin-confirm-cancel:hover {
	            border-color: var(--line-strong);
	            background: var(--surface-soft);
	        }

	        .admin-confirm-accept {
	            border: 0;
	            background: var(--red-600, #c2413d);
	            color: #fff;
	            box-shadow: 0 14px 28px rgba(169, 67, 63, 0.22);
	        }

	        .admin-confirm-accept:hover {
	            transform: translateY(-1px);
	        }

	        @media (max-width: 560px) {
	            .admin-confirm-dialog {
	                border-radius: 18px;
	                padding: 16px;
	            }

	            .admin-confirm-actions {
	                display: grid;
	                grid-template-columns: minmax(0, 1fr);
	            }

	            .admin-confirm-actions button {
	                width: 100%;
	            }
	        }
	    `;
	    document.head.appendChild(style);

	    dialog = document.createElement('div');
	    dialog.id = 'adminConfirmDialog';
	    dialog.className = 'admin-confirm-backdrop';
	    dialog.innerHTML = `
	        <section class="admin-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="adminConfirmTitle">
	            <div class="admin-confirm-head">
	                <span class="admin-confirm-icon" aria-hidden="true">!</span>
	                <div class="admin-confirm-copy">
	                    <h3 id="adminConfirmTitle">Xác nhận thao tác</h3>
	                    <p data-confirm-message>Bạn có chắc chắn muốn tiếp tục?</p>
	                </div>
	            </div>

	            <p class="admin-confirm-warning">
	                Thao tác này có thể ảnh hưởng dữ liệu đang hiển thị trong hệ thống. Kiểm tra kỹ trước khi xác nhận, vì nút xóa sinh ra để con người hối hận.
	            </p>

	            <div class="admin-confirm-actions">
	                <button class="admin-confirm-cancel" type="button" data-confirm-cancel>Hủy</button>
	                <button class="admin-confirm-accept" type="button" data-confirm-accept>Xác nhận xóa</button>
	            </div>
	        </section>
	    `;

	    document.body.appendChild(dialog);

	    return dialog;
	}

	function showAdminConfirm(message, options = {}) {
	    const dialog = ensureConfirmDialog();
	    const messageNode = dialog.querySelector('[data-confirm-message]');
	    const titleNode = dialog.querySelector('#adminConfirmTitle');
	    const acceptButton = dialog.querySelector('[data-confirm-accept]');
	    const cancelButton = dialog.querySelector('[data-confirm-cancel]');

	    titleNode.textContent = options.title || 'Xác nhận thao tác';
	    messageNode.textContent = message || 'Bạn có chắc chắn muốn tiếp tục?';
	    acceptButton.textContent = options.acceptText || 'Xác nhận';
	    cancelButton.textContent = options.cancelText || 'Hủy';

	    return new Promise((resolve) => {
	        let resolved = false;

	        const cleanup = () => {
	            dialog.classList.remove('is-open');
	            acceptButton.removeEventListener('click', accept);
	            cancelButton.removeEventListener('click', cancel);
	            dialog.removeEventListener('click', backdropCancel);
	            document.removeEventListener('keydown', escapeCancel);
	        };

	        const finish = (value) => {
	            if (resolved) {
	                return;
	            }

	            resolved = true;
	            cleanup();
	            resolve(value);
	        };

	        const accept = () => finish(true);
	        const cancel = () => finish(false);

	        const backdropCancel = (event) => {
	            if (event.target === dialog) {
	                cancel();
	            }
	        };

	        const escapeCancel = (event) => {
	            if (event.key === 'Escape') {
	                cancel();
	            }
	        };

	        acceptButton.addEventListener('click', accept);
	        cancelButton.addEventListener('click', cancel);
	        dialog.addEventListener('click', backdropCancel);
	        document.addEventListener('keydown', escapeCancel);

	        dialog.classList.add('is-open');
	        window.setTimeout(() => cancelButton.focus(), 0);
	    });
	}

	admin.showConfirm = showAdminConfirm;

    
	function initLoadingForms() {
	    document.querySelectorAll('.js-loading-form').forEach((form) => {
	        form.addEventListener('submit', async (event) => {
	            if (event.defaultPrevented) {
	                return;
	            }

	            if (form.dataset.confirm) {
	                event.preventDefault();

	                const accepted = await showAdminConfirm(form.dataset.confirm || 'Bạn có chắc chắn muốn tiếp tục?', {
	                    title: 'Xác nhận xóa dữ liệu',
	                    acceptText: 'Xóa',
	                    cancelText: 'Hủy'
	                });

	                if (!accepted) {
	                    return;
	                }

	                form.removeAttribute('data-confirm');
	                form.requestSubmit();
	                return;
	            }

	            const button = form.querySelector('button[type="submit"]');

	            if (!button) {
	                return;
	            }

	            button.dataset.originalText = button.textContent;
	            button.textContent = button.dataset.loadingText || 'Đang xử lý...';
	            button.classList.add('is-loading');
	            button.disabled = true;
	        });
	    });

	    document.querySelectorAll('form[data-confirm]:not(.js-loading-form)').forEach((form) => {
	        form.addEventListener('submit', async (event) => {
	            event.preventDefault();

	            const accepted = await showAdminConfirm(form.dataset.confirm || 'Bạn có chắc chắn muốn tiếp tục?', {
	                title: 'Xác nhận thao tác',
	                acceptText: 'Xác nhận',
	                cancelText: 'Hủy'
	            });

	            if (!accepted) {
	                return;
	            }

	            form.removeAttribute('data-confirm');
	            form.submit();
	        });
	    });
	}
	
    function initNavigation() {
        const sections = Array.from(document.querySelectorAll('.admin-section'));
        const navLinks = Array.from(document.querySelectorAll('.side-nav a, .topbar-actions a[href^="#"]'));
        const pageTitle = document.getElementById('pageTitle');
        const pageDescription = document.getElementById('pageDescription');
        const sidebar = document.getElementById('adminSidebar');
        const sidebarToggle = document.querySelector('.sidebar-toggle');

        function activateSection(sectionId) {
            const targetId = sections.some((section) => section.id === sectionId) ? sectionId : 'dashboard';
            sections.forEach((section) => section.classList.toggle('is-active', section.id === targetId));
            navLinks.forEach((link) => {
                link.classList.toggle('active', link.getAttribute('href') === `#${targetId}`);
            });
            const activeNav = document.querySelector(`.side-nav a[href="#${targetId}"]`);
            if (activeNav && pageTitle && pageDescription) {
                pageTitle.textContent = activeNav.dataset.title || activeNav.textContent.trim();
                pageDescription.textContent = activeNav.dataset.description || '';
            }
            if (sidebar && sidebar.classList.contains('is-open')) {
                sidebar.classList.remove('is-open');
            }
        }

        navLinks.forEach((link) => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                activateSection(link.getAttribute('href').replace('#', ''));
            });
        });

        window.addEventListener('hashchange', () => activateSection(window.location.hash.replace('#', '')));
        activateSection(window.location.hash.replace('#', '') || 'dashboard');

        if (sidebarToggle && sidebar) {
            sidebarToggle.addEventListener('click', () => {
                const open = !sidebar.classList.contains('is-open');
                sidebar.classList.toggle('is-open', open);
                sidebarToggle.setAttribute('aria-expanded', String(open));
            });
        }

        admin.activateSection = activateSection;
    }

    function catalogDetailFor(row) {
        const detailRow = row ? row.nextElementSibling : null;
        return detailRow && detailRow.classList.contains('catalog-detail-row') ? detailRow : null;
    }

    function setCatalogDetailOpen(detailRow, open) {
        if (!detailRow) {
            return;
        }
        detailRow.classList.toggle('is-open', open);
        const detail = detailRow.querySelector('details');
        if (detail) {
            detail.open = open;
        }
    }

    function closeCatalogDetails(section) {
        section.querySelectorAll('.catalog-detail-row').forEach((row) => setCatalogDetailOpen(row, false));
    }

    function sortCatalogPairs(section, rows, compareFn) {
        const tbody = section.querySelector('.catalog-table tbody');
        if (!tbody || !compareFn) {
            return;
        }
        rows
                .map((row) => ({summary: row, detail: catalogDetailFor(row)}))
                .sort((left, right) => compareFn(left.summary, right.summary))
                .forEach((pair) => {
                    tbody.appendChild(pair.summary);
                    if (pair.detail) {
                        tbody.appendChild(pair.detail);
                    }
                });
    }

    function setupCatalogSection(options) {
        const section = document.getElementById(options.sectionId);
        if (!section) {
            return null;
        }
        const rows = Array.from(section.querySelectorAll(options.rowSelector));
        const form = section.querySelector(options.formSelector);
        const empty = section.querySelector(options.emptySelector);
        const applyButton = section.querySelector(options.applySelector);
        const resetButton = section.querySelector(options.resetSelector);

        function applyFilters() {
            const fields = form ? new FormData(form) : new FormData();
            if (options.sort) {
                sortCatalogPairs(section, rows, (left, right) => options.sort(left, right, fields));
            }
            let visibleCount = 0;
            rows.forEach((row) => {
                const visible = options.match(row, fields);
                row.classList.toggle('is-filtered-out', !visible);
                const detailRow = catalogDetailFor(row);
                if (detailRow) {
                    detailRow.classList.toggle('is-filtered-out', !visible);
                    if (!visible) {
                        setCatalogDetailOpen(detailRow, false);
                    }
                }
                if (visible) {
                    visibleCount += 1;
                }
            });
            if (empty) {
                empty.hidden = rows.length === 0 || visibleCount > 0;
            }
            closeCatalogDetails(section);
        }

        section.querySelectorAll('.js-edit-catalog').forEach((button) => {
            button.addEventListener('click', () => {
                const summaryRow = button.closest('.catalog-summary-row');
                const detailRow = catalogDetailFor(summaryRow);
                if (!detailRow) {
                    return;
                }
                const shouldOpen = !detailRow.classList.contains('is-open');
                closeCatalogDetails(section);
                setCatalogDetailOpen(detailRow, shouldOpen);
            });
        });

        section.querySelectorAll('.js-cancel-catalog').forEach((button) => {
            button.addEventListener('click', () => closeCatalogDetails(section));
        });

        if (form) {
            form.reset();
            form.addEventListener('submit', (event) => {
                event.preventDefault();
                applyFilters();
            });
        }
        if (applyButton) {
            applyButton.addEventListener('click', applyFilters);
        }
        if (resetButton) {
            resetButton.addEventListener('click', () => window.setTimeout(applyFilters, 0));
        }
        applyFilters();
        return {applyFilters, form};
    }

    function setupCardFilter(options) {
        const section = document.getElementById(options.sectionId);
        if (!section) {
            return null;
        }
        const form = section.querySelector(options.formSelector);
        const cards = Array.from(section.querySelectorAll(options.cardSelector));
        const empty = section.querySelector(options.emptySelector);
        const applyButton = section.querySelector(options.applySelector);
        const resetButton = section.querySelector(options.resetSelector);

        function applyFilters() {
            const fields = form ? new FormData(form) : new FormData();
            let visibleCount = 0;
            cards.forEach((card) => {
                const visible = options.match(card, fields);
                card.hidden = !visible;
                if (!visible) {
                    closePanels(card);
                }
                if (visible) {
                    visibleCount += 1;
                }
            });
            if (empty) {
                empty.hidden = cards.length === 0 || visibleCount > 0;
            }
        }

        if (form) {
            form.reset();
            form.addEventListener('submit', (event) => {
                event.preventDefault();
                applyFilters();
            });
        }
        if (applyButton) {
            applyButton.addEventListener('click', applyFilters);
        }
        if (resetButton) {
            resetButton.addEventListener('click', () => window.setTimeout(applyFilters, 0));
        }
        applyFilters();
        return {applyFilters, form};
    }

    function initPanels() {
        document.querySelectorAll('.js-toggle-panel').forEach((button) => {
            button.addEventListener('click', () => {
                const targetId = button.dataset.panelTarget;
                const panel = targetId ? document.getElementById(targetId) : null;
                if (!panel) {
                    return;
                }
                const section = button.closest('.admin-section') || document;
                const shouldOpen = panel.hidden;
                closePanels(section);
                panel.hidden = !shouldOpen;
            });
        });

        document.querySelectorAll('.js-close-panel').forEach((button) => {
            button.addEventListener('click', () => {
                const panel = button.closest('.collapse-panel');
                if (panel) {
                    panel.hidden = true;
                }
            });
        });
    }

    function initLogout() {
        const logoutButton = document.getElementById('logoutButton');
        if (!logoutButton) {
            return;
        }
        logoutButton.addEventListener('click', async () => {
            logoutButton.textContent = 'Đang xuất...';
            logoutButton.disabled = true;
            await fetch('/api/auth/logout', {method: 'POST', credentials: 'include'});
            window.location.href = '/login';
        });
    }
	
	window.addEventListener('pageshow', () => {
	    document.querySelectorAll('.js-loading-form button.is-loading').forEach((button) => {
	        if (button.dataset.originalText) {
	            button.textContent = button.dataset.originalText;
	        }

	        button.classList.remove('is-loading');
	        button.disabled = false;
	        delete button.dataset.originalText;
	    });
	});

	function ensureToastStyle() {
	    if (document.getElementById('greenyAdminToastStyle')) {
	        return;
	    }

	    const style = document.createElement('style');
	    style.id = 'greenyAdminToastStyle';
	    style.textContent = `
	        .admin-toast-stack {
	            position: fixed;
	            top: 22px;
	            right: 22px;
	            z-index: 99999;
	            display: grid;
	            gap: 12px;
	            width: min(420px, calc(100vw - 32px));
	            pointer-events: none;
	        }

	        .admin-toast {
	            pointer-events: auto;
	            display: grid;
	            grid-template-columns: 42px minmax(0, 1fr) 32px;
	            gap: 12px;
	            align-items: start;
	            border: 1px solid rgba(34, 113, 80, 0.16);
	            border-radius: 18px;
	            background:
	                linear-gradient(180deg, rgba(255,255,255,.98), rgba(250,253,251,.96)),
	                #fff;
	            box-shadow: 0 18px 44px rgba(16, 24, 20, 0.16);
	            padding: 14px;
	            transform: translateX(18px);
	            opacity: 0;
	            animation: adminToastIn 220ms ease forwards;
	        }

	        .admin-toast.is-leaving {
	            animation: adminToastOut 180ms ease forwards;
	        }

	        .admin-toast-icon {
	            display: inline-grid;
	            width: 42px;
	            height: 42px;
	            place-items: center;
	            border-radius: 15px;
	            background: var(--green-100);
	            color: var(--green-700);
	            font-weight: 950;
	            font-size: 18px;
	        }

	        .admin-toast-content strong {
	            display: block;
	            color: var(--ink);
	            font-size: 14px;
	            font-weight: 950;
	            line-height: 1.25;
	        }

	        .admin-toast-content p {
	            margin: 4px 0 0;
	            color: var(--ink-soft);
	            font-size: 13px;
	            line-height: 1.45;
	        }

	        .admin-toast-close {
	            display: inline-grid;
	            width: 30px;
	            height: 30px;
	            place-items: center;
	            border: 0;
	            border-radius: 999px;
	            background: transparent;
	            color: var(--muted);
	            cursor: pointer;
	            font-size: 18px;
	            line-height: 1;
	        }

	        .admin-toast-close:hover {
	            background: rgba(20, 31, 25, 0.06);
	            color: var(--ink);
	        }

	        .admin-toast.success .admin-toast-icon {
	            background: var(--green-100);
	            color: var(--green-700);
	        }

	        .admin-toast.warning .admin-toast-icon {
	            background: var(--amber-100);
	            color: var(--amber-700);
	        }

	        .admin-toast.danger .admin-toast-icon {
	            background: var(--red-100);
	            color: var(--red-700);
	        }

	        .admin-toast.info .admin-toast-icon {
	            background: var(--blue-100);
	            color: var(--blue-700);
	        }

	        @keyframes adminToastIn {
	            to {
	                transform: translateX(0);
	                opacity: 1;
	            }
	        }

	        @keyframes adminToastOut {
	            to {
	                transform: translateX(18px);
	                opacity: 0;
	            }
	        }

	        @media (max-width: 640px) {
	            .admin-toast-stack {
	                top: 14px;
	                right: 14px;
	                left: 14px;
	                width: auto;
	            }

	            .admin-toast {
	                grid-template-columns: 38px minmax(0, 1fr) 30px;
	                border-radius: 16px;
	                padding: 12px;
	            }

	            .admin-toast-icon {
	                width: 38px;
	                height: 38px;
	            }
	        }
	    `;
	    document.head.appendChild(style);
	}

	function ensureToastRoot() {
	    let root = document.getElementById('adminToastStack');

	    if (!root) {
	        root = document.createElement('div');
	        root.id = 'adminToastStack';
	        root.className = 'admin-toast-stack';
	        root.setAttribute('aria-live', 'polite');
	        root.setAttribute('aria-atomic', 'true');
	        document.body.appendChild(root);
	    }

	    return root;
	}

	function showToast(payload) {
	    if (!payload || !payload.message) {
	        return;
	    }

	    ensureToastStyle();

	    const root = ensureToastRoot();
	    const type = payload.type === 'warning' || payload.type === 'danger' || payload.type === 'info'
	        ? payload.type
	        : 'success';

	    const toast = document.createElement('section');
	    toast.className = `admin-toast ${type}`;
	    toast.setAttribute('role', type === 'danger' || type === 'warning' ? 'alert' : 'status');

	    const icon = type === 'warning'
	        ? '!'
	        : type === 'danger'
	            ? '×'
	            : type === 'info'
	                ? 'i'
	                : '✓';

	    const title = type === 'warning'
	        ? 'Cần kiểm tra'
	        : type === 'danger'
	            ? 'Có lỗi xảy ra'
	            : type === 'info'
	                ? 'Thông báo'
	                : 'Thành công';

	    toast.innerHTML = `
	        <span class="admin-toast-icon" aria-hidden="true">${icon}</span>
	        <div class="admin-toast-content">
	            <strong>${title}</strong>
	            <p></p>
	        </div>
	        <button class="admin-toast-close" type="button" aria-label="Đóng thông báo">×</button>
	    `;

	    toast.querySelector('p').textContent = payload.message;

	    const closeToast = () => {
	        if (toast.classList.contains('is-leaving')) {
	            return;
	        }

	        toast.classList.add('is-leaving');
	        window.setTimeout(() => toast.remove(), 190);
	    };

	    toast.querySelector('.admin-toast-close').addEventListener('click', closeToast);

	    root.appendChild(toast);

	    window.setTimeout(closeToast, payload.duration || 3600);
	}

	function showStoredFlash() {
	    const raw = sessionStorage.getItem('greenyAdminFlash');

	    if (raw) {
	        sessionStorage.removeItem('greenyAdminFlash');

	        try {
	            showToast(JSON.parse(raw));
	        } catch (error) {
	            // Flash hỏng thì bỏ qua, khỏi làm admin nổ tung vì một cái toast.
	        }
	    }

	    /*
	     * Chuyển các notice cũ đang bị render ở đầu trang thành toast.
	     * Hữu ích nếu controller/layout vẫn có chỗ sinh <section class="notice">.
	     */
	    document.querySelectorAll('.admin-shell > .notice').forEach((notice) => {
	        const message = notice.textContent ? notice.textContent.trim() : '';

	        if (message) {
	            showToast({
	                type: notice.classList.contains('warning') ? 'warning' : 'success',
	                message
	            });
	        }

	        notice.remove();
	    });
	}

	admin.showToast = showToast;

    admin.normalizeText = normalizeText;
    admin.readNumber = readNumber;
    admin.closePanels = closePanels;
    admin.dateMatches = dateMatches;
    admin.setupCatalogSection = setupCatalogSection;
    admin.setupCardFilter = setupCardFilter;

    initLoadingForms();
    initNavigation();
    initPanels();
    initLogout();
    showStoredFlash();
})();
