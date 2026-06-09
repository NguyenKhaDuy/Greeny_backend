(function (admin) {
    const normalizeText = admin.normalizeText;
    const couponFilterForm = document.querySelector('[data-coupon-filter-form]');
    const applyCouponFiltersButton = document.querySelector('[data-apply-coupon-filters]');
    const resetCouponFiltersButton = document.querySelector('[data-reset-coupon-filters]');
    const couponSummaryRows = Array.from(document.querySelectorAll('.coupon-summary-row'));
    const couponDetailRows = Array.from(document.querySelectorAll('.coupon-detail-row'));
    const couponFilterEmpty = document.querySelector('.coupon-filter-empty');

    function closeCouponDetails() {
        couponDetailRows.forEach((row) => {
            row.classList.remove('is-open');
            const detail = row.querySelector('details');
            if (detail) {
                detail.open = false;
            }
        });
    }

    function couponMatchesFilters(row) {
        const fields = couponFilterForm ? new FormData(couponFilterForm) : new FormData();
        const code = normalizeText(fields.get('code'));
        const status = fields.get('status') || 'all';
        const type = fields.get('type') || 'all';
        const startsFrom = fields.get('startsFrom') || '';
        const expiresTo = fields.get('expiresTo') || '';
        const usage = fields.get('usage') || 'all';
        const usedOut = row.dataset.usedOut === 'true';
        return (!code || normalizeText(row.dataset.couponCode).includes(code))
                && (status === 'all' || row.dataset.couponStatus === status)
                && (type === 'all' || row.dataset.couponType === type)
                && (!startsFrom || !row.dataset.starts || row.dataset.starts >= startsFrom)
                && (!expiresTo || !row.dataset.expires || row.dataset.expires <= expiresTo)
                && (usage === 'all' || (usage === 'used-out' ? usedOut : !usedOut));
    }

    function applyCouponFilters() {
        let visibleCount = 0;
        couponSummaryRows.forEach((row) => {
            const visible = couponMatchesFilters(row);
            row.classList.toggle('is-filtered-out', !visible);
            if (visible) {
                visibleCount += 1;
            }
            const detailRow = row.nextElementSibling;
            if (detailRow && detailRow.classList.contains('coupon-detail-row')) {
                detailRow.classList.toggle('is-filtered-out', !visible);
            }
        });
        if (couponFilterEmpty) {
            couponFilterEmpty.hidden = visibleCount > 0;
        }
        closeCouponDetails();
    }

    document.querySelectorAll('.js-edit-coupon').forEach((button) => {
        button.addEventListener('click', () => {
            const summaryRow = button.closest('.coupon-summary-row');
            const detailRow = summaryRow ? summaryRow.nextElementSibling : null;
            if (!detailRow || !detailRow.classList.contains('coupon-detail-row')) {
                return;
            }
            const shouldOpen = !detailRow.classList.contains('is-open');
            closeCouponDetails();
            if (shouldOpen) {
                detailRow.classList.add('is-open');
                const detail = detailRow.querySelector('details');
                if (detail) {
                    detail.open = true;
                }
            }
        });
    });

    document.querySelectorAll('.js-cancel-coupon').forEach((button) => {
        button.addEventListener('click', closeCouponDetails);
    });

    if (couponFilterForm) {
        couponFilterForm.reset();
        couponFilterForm.addEventListener('submit', (event) => {
            event.preventDefault();
            applyCouponFilters();
        });
    }
    if (applyCouponFiltersButton) {
        applyCouponFiltersButton.addEventListener('click', applyCouponFilters);
    }
    if (resetCouponFiltersButton) {
        resetCouponFiltersButton.addEventListener('click', () => {
            window.setTimeout(applyCouponFilters, 0);
        });
    }
    applyCouponFilters();
})(window.GreenyAdmin || {});
