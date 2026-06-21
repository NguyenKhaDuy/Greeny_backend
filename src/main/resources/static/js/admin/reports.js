(function (window, document) {
    'use strict';

    const admin = window.GreenyAdmin = window.GreenyAdmin || {};

    function normalizeText(value) {
        return String(value || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim();
    }

    function initProductPerformanceSearch() {
        const section = document.getElementById('product-stats');

        if (!section) {
            return;
        }

        const form = section.querySelector('[data-performance-search-form]');
        const input = section.querySelector('[data-performance-search]');
        const count = section.querySelector('[data-performance-search-count]');
        const empty = section.querySelector('[data-performance-filter-empty]');
        const rows = Array.from(section.querySelectorAll('[data-performance-row]'));

        if (!form || !input || rows.length === 0) {
            return;
        }

        const applySearch = () => {
            const query = normalizeText(input.value);
            let visible = 0;

            rows.forEach((row) => {
                const matched = !query || normalizeText(row.textContent).includes(query);
                row.hidden = !matched;
                row.classList.toggle('is-filter-hidden', !matched);

                if (matched) {
                    visible += 1;
                }
            });

            if (count) {
                count.textContent = `${visible} / ${rows.length} cây`;
            }

            if (empty) {
                empty.hidden = visible !== 0;
            }
        };

        if (form.dataset.performanceSearchBound === '1') {
            applySearch();
            return;
        }

        form.dataset.performanceSearchBound = '1';

        input.addEventListener('input', applySearch);
        form.addEventListener('submit', (event) => {
            event.preventDefault();
            applySearch();
        });
        form.addEventListener('reset', () => {
            window.setTimeout(applySearch, 0);
        });

        applySearch();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initProductPerformanceSearch);
    } else {
        initProductPerformanceSearch();
    }

    admin.initProductPerformanceSearch = initProductPerformanceSearch;

})(window, document);
