(function (admin) {
    const normalizeText = admin.normalizeText;
    const readNumber = admin.readNumber;
    const orderFilterButtons = Array.from(document.querySelectorAll('[data-order-filter]'));
    const orderSummaryRows = Array.from(document.querySelectorAll('.order-summary-row'));
    const orderDetailRows = Array.from(document.querySelectorAll('.order-detail-row'));
    const orderFilterEmpty = document.querySelector('.order-filter-empty');
    const orderFilterForm = document.querySelector('[data-order-filter-form]');
    const applyOrderFiltersButton = document.querySelector('[data-apply-order-filters]');
    const resetOrderFiltersButton = document.querySelector('[data-reset-order-filters]');
    let activeOrderStatus = 'all';
    let orderViewMode = 'quick';

    function readOrderFilterValues() {
        const fields = orderFilterForm ? new FormData(orderFilterForm) : new FormData();
        return {
            orderId: normalizeText(fields.get('orderId')),
            email: normalizeText(fields.get('email')),
            phone: normalizeText(fields.get('phone')),
            dateFrom: fields.get('dateFrom') || '',
            dateTo: fields.get('dateTo') || '',
            minTotal: readNumber(fields.get('minTotal')),
            maxTotal: readNumber(fields.get('maxTotal'))
        };
    }

    function readOrderDate(row) {
        const dataDate = row.dataset.created || '';
        if (/^\d{4}-\d{2}-\d{2}$/.test(dataDate)) {
            return dataDate;
        }
        const visibleDate = row.querySelector('td small') ? row.querySelector('td small').textContent : '';
        const dateParts = visibleDate.match(/(\d{2})\/(\d{2})\/(\d{4})/);
        return dateParts ? `${dateParts[3]}-${dateParts[2]}-${dateParts[1]}` : '';
    }

    function readOrderTotal(row) {
        const totalFromDataset = readNumber(row.dataset.total);
        if (totalFromDataset !== null) {
            return totalFromDataset;
        }
        const totalCell = row.querySelector('td:nth-child(3)');
        const totalFromCell = readNumber(totalCell ? totalCell.textContent : '');
        return totalFromCell !== null ? totalFromCell : 0;
    }

    function readOrderSearchText(row) {
        const detailRow = row.nextElementSibling;
        return normalizeText([
            row.dataset.orderId,
            row.dataset.customer,
            row.dataset.email,
            row.dataset.phone,
            row.textContent,
            detailRow && detailRow.classList.contains('order-detail-row') ? detailRow.textContent : ''
        ].join(' '));
    }

    function resetOrderFilterFields() {
        if (orderFilterForm) {
            orderFilterForm.reset();
        }
    }

    function closeOrderDetails() {
        orderDetailRows.forEach((row) => {
            row.classList.remove('is-open');
            const detail = row.querySelector('details');
            if (detail) {
                detail.open = false;
            }
        });
    }

    function syncOrderFilterCounts() {
        const counts = {all: orderSummaryRows.length};
        orderSummaryRows.forEach((row) => {
            const status = row.dataset.orderStatus || 'unknown';
            counts[status] = (counts[status] || 0) + 1;
        });
        orderFilterButtons.forEach((button) => {
            const counter = button.querySelector('span');
            if (counter) {
                counter.textContent = counts[button.dataset.orderFilter] || 0;
            }
        });
    }

    function orderMatchesFilters(row, filters) {
        const rowSearchText = readOrderSearchText(row);
        const rowTotal = readOrderTotal(row);
        const rowDate = readOrderDate(row);
        return (!filters.orderId || rowSearchText.includes(filters.orderId))
                && (!filters.email || rowSearchText.includes(filters.email))
                && (!filters.phone || rowSearchText.includes(filters.phone))
                && (!filters.dateFrom || rowDate >= filters.dateFrom)
                && (!filters.dateTo || rowDate <= filters.dateTo)
                && (filters.minTotal === null || rowTotal >= filters.minTotal)
                && (filters.maxTotal === null || rowTotal <= filters.maxTotal);
    }

    function orderMatchesStatus(row) {
        return activeOrderStatus === 'all' || row.dataset.orderStatus === activeOrderStatus;
    }

    function setOrderTabActive(status) {
        orderFilterButtons.forEach((button) => {
            button.classList.toggle('active', orderViewMode === 'quick' && button.dataset.orderFilter === status);
        });
    }

    function renderOrderRows(matchFn) {
        let visibleCount = 0;
        orderSummaryRows.forEach((row) => {
            const visible = matchFn(row);
            row.classList.toggle('is-filtered-out', !visible);
            if (visible) {
                visibleCount += 1;
            }
            const detailRow = row.nextElementSibling;
            if (detailRow && detailRow.classList.contains('order-detail-row')) {
                detailRow.classList.toggle('is-filtered-out', !visible);
            }
        });
        if (orderFilterEmpty) {
            orderFilterEmpty.hidden = visibleCount > 0;
        }
        closeOrderDetails();
        return visibleCount;
    }

    function applyOrderQuickStatus(status) {
        orderViewMode = 'quick';
        activeOrderStatus = status || 'all';
        resetOrderFilterFields();
        renderOrderRows(orderMatchesStatus);
        setOrderTabActive(activeOrderStatus);
    }

    function applyOrderFieldFilters() {
        const filters = readOrderFilterValues();
        orderViewMode = 'filter';
        activeOrderStatus = null;
        renderOrderRows((row) => orderMatchesFilters(row, filters));
        setOrderTabActive(null);
    }

    function clearOrderFilters() {
        resetOrderFilterFields();
        applyOrderQuickStatus('all');
    }

    function initOrders() {
        document.querySelectorAll('.js-edit-order').forEach((button) => {
            button.addEventListener('click', () => {
                const summaryRow = button.closest('.order-summary-row');
                const detailRow = summaryRow ? summaryRow.nextElementSibling : null;
                if (!detailRow || !detailRow.classList.contains('order-detail-row')) {
                    return;
                }
                const shouldOpen = !detailRow.classList.contains('is-open');
                closeOrderDetails();
                if (shouldOpen) {
                    detailRow.classList.add('is-open');
                    const detail = detailRow.querySelector('details');
                    if (detail) {
                        detail.open = true;
                    }
                }
            });
        });

        orderFilterButtons.forEach((button) => {
            button.addEventListener('click', () => applyOrderQuickStatus(button.dataset.orderFilter));
        });
        if (applyOrderFiltersButton) {
            applyOrderFiltersButton.addEventListener('click', applyOrderFieldFilters);
        }
        if (orderFilterForm) {
            orderFilterForm.addEventListener('submit', (event) => {
                event.preventDefault();
                applyOrderFieldFilters();
            });
        }
        if (resetOrderFiltersButton) {
            resetOrderFiltersButton.addEventListener('click', () => {
                window.setTimeout(clearOrderFilters, 0);
            });
        }
        resetOrderFilterFields();
        syncOrderFilterCounts();
        applyOrderQuickStatus('all');
    }

    admin.orders = {
        applyFieldFilters: applyOrderFieldFilters,
        filterForm: orderFilterForm
    };

    initOrders();
})(window.GreenyAdmin || {});
