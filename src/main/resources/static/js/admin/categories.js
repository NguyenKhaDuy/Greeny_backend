(function (admin, document) {
    'use strict';

    const normalizeText = admin.normalizeText || ((value) => (value || '').toString().trim().toLowerCase());

    const section = document.getElementById('categories');

    if (!section) {
        return;
    }

    const form = section.querySelector('[data-category-filter-form]');
    const applyButton = section.querySelector('[data-apply-category-filters]');
    const resetButton = section.querySelector('[data-reset-category-filters]');
    const list = section.querySelector('.category-card-list');
    const empty = section.querySelector('.category-filter-empty');

    function getSummaryRows() {
        return Array.from(section.querySelectorAll('.category-summary-row'));
    }

    function detailFor(row) {
        const detailRow = row ? row.nextElementSibling : null;
        return detailRow && detailRow.classList.contains('catalog-detail-row') ? detailRow : null;
    }

    function setDetailOpen(detailRow, open) {
        if (!detailRow) {
            return;
        }

        detailRow.classList.toggle('is-open', open);

        const detail = detailRow.querySelector('details');
        if (detail) {
            detail.open = open;
        }
    }

    function closeDetails() {
        section.querySelectorAll('.catalog-detail-row').forEach((row) => setDetailOpen(row, false));
    }

    function readFilters() {
        const fields = form ? new FormData(form) : new FormData();

        return {
            query: normalizeText(fields.get('q')),
            status: fields.get('status') || 'all',
            sort: fields.get('sort') || 'sort-asc'
        };
    }

    function matches(row, filters) {
        const search = normalizeText(row.dataset.search);
        const status = row.dataset.status || 'hidden';

        return (!filters.query || search.includes(filters.query))
                && (filters.status === 'all' || status === filters.status);
    }

    function sortRows(rows, direction) {
        if (!list) {
            return;
        }

        rows
                .map((row) => ({
                    summary: row,
                    detail: detailFor(row)
                }))
                .sort((left, right) => {
                    const leftSort = Number(left.summary.dataset.sort || 999999);
                    const rightSort = Number(right.summary.dataset.sort || 999999);
                    const sortDirection = direction === 'sort-desc' ? -1 : 1;

                    if (leftSort !== rightSort) {
                        return (leftSort - rightSort) * sortDirection;
                    }

                    return normalizeText(left.summary.dataset.search)
                            .localeCompare(normalizeText(right.summary.dataset.search), 'vi') * sortDirection;
                })
                .forEach((pair) => {
                    list.appendChild(pair.summary);
                    if (pair.detail) {
                        list.appendChild(pair.detail);
                    }
                });
    }

    function applyFilters() {
        const filters = readFilters();
        const rows = getSummaryRows();

        sortRows(rows, filters.sort);

        let visibleCount = 0;

        getSummaryRows().forEach((row) => {
            const visible = matches(row, filters);
            const detailRow = detailFor(row);

            row.classList.toggle('is-filtered-out', !visible);

            if (detailRow) {
                detailRow.classList.toggle('is-filtered-out', !visible);

                if (!visible) {
                    setDetailOpen(detailRow, false);
                }
            }

            if (visible) {
                visibleCount += 1;
            }
        });

        if (empty) {
            empty.hidden = visibleCount > 0;
        }

        closeDetails();

        return visibleCount;
    }

    function bindEditButtons() {
        section.querySelectorAll('.js-edit-catalog').forEach((button) => {
            button.addEventListener('click', () => {
                const summaryRow = button.closest('.category-summary-row');
                const detailRow = detailFor(summaryRow);

                if (!detailRow) {
                    return;
                }

                const shouldOpen = !detailRow.classList.contains('is-open');

                closeDetails();
                setDetailOpen(detailRow, shouldOpen);
            });
        });

        section.querySelectorAll('.js-cancel-catalog').forEach((button) => {
            button.addEventListener('click', closeDetails);
        });
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
        resetButton.addEventListener('click', () => {
            window.setTimeout(applyFilters, 0);
        });
    }

    bindEditButtons();
    applyFilters();

    admin.categoryCatalog = {
        applyFilters,
        filterForm: form,
        closeDetails
    };
})(window.GreenyAdmin || {}, document);