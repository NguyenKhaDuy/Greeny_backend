(function (admin, document) {
    'use strict';

    const normalizeText = admin.normalizeText || ((value) => (value || '').toString().trim().toLowerCase());
    const readNumber = admin.readNumber || ((value) => {
        if (value === null || value === undefined || value === '') {
            return null;
        }

        const number = Number(String(value).replace(/[^\d.-]/g, ''));

        return Number.isNaN(number) ? null : number;
    });

    const section = document.getElementById('variants');

    if (!section) {
        return;
    }

    const form = section.querySelector('[data-variant-filter-form]');
    const applyButton = section.querySelector('[data-apply-variant-filters]');
    const resetButton = section.querySelector('[data-reset-variant-filters]');
    const list = section.querySelector('.variant-card-list');
    const empty = section.querySelector('.variant-filter-empty');

    function getSummaryRows() {
        return Array.from(section.querySelectorAll('.variant-summary-row'));
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
            name: normalizeText(fields.get('name')),
            sku: normalizeText(fields.get('sku')),
            plantId: fields.get('plantId') || 'all',
            category: fields.get('categoryTitle') || 'all',
            stock: fields.get('stock') || 'all',
            status: fields.get('status') || 'all',
            minPrice: readNumber(fields.get('minPrice')),
            maxPrice: readNumber(fields.get('maxPrice'))
        };
    }

    function matches(row, filters) {
        const rowPriceValue = readNumber(row.dataset.price);
        const rowPrice = rowPriceValue === null ? 0 : rowPriceValue;
        const rowCategory = normalizeText(row.dataset.category);
        const selectedCategory = normalizeText(filters.category);

        const categoryMatches = filters.category === 'all'
                || (filters.category === 'none'
                        ? rowCategory === normalizeText('Chưa chọn danh mục')
                        : rowCategory === selectedCategory);

        return (!filters.name || normalizeText(row.dataset.search).includes(filters.name))
                && (!filters.sku || normalizeText(row.dataset.sku).includes(filters.sku))
                && (filters.plantId === 'all' || row.dataset.plant === filters.plantId)
                && categoryMatches
                && (filters.stock === 'all' || row.dataset.stock === filters.stock)
                && (filters.status === 'all' || row.dataset.status === filters.status)
                && (filters.minPrice === null || rowPrice >= filters.minPrice)
                && (filters.maxPrice === null || rowPrice <= filters.maxPrice);
    }

    function applyFilters() {
        const filters = readFilters();
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
                const summaryRow = button.closest('.variant-summary-row');
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

    function jumpToPlantVariants(link) {
        if (admin.activateSection) {
            admin.activateSection('variants');
        }

        if (!form) {
            return;
        }

        const plantSelect = form.querySelector('[name="plantId"]');

        if (!plantSelect) {
            return;
        }

        form.reset();
        plantSelect.value = link.dataset.jumpVariantPlant || 'all';
        applyFilters();
    }

    document.querySelectorAll('[data-jump-variant-plant]').forEach((link) => {
        link.addEventListener('click', (event) => {
            event.preventDefault();
            jumpToPlantVariants(link);
        });
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
        resetButton.addEventListener('click', () => {
            window.setTimeout(applyFilters, 0);
        });
    }

    bindEditButtons();
    applyFilters();

    admin.variantCatalog = {
        applyFilters,
        filterForm: form,
        form,
        closeDetails
    };
})(window.GreenyAdmin || {}, document);