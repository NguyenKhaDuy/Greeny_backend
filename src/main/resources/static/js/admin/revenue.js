(function (window, document, admin) {
    'use strict';

    function getRevenueFilterParams() {
        const filterForm = document.querySelector('#revenue .revenue-filter');

        if (!filterForm) {
            return new URLSearchParams();
        }

        return new URLSearchParams(new FormData(filterForm));
    }

    function exportRevenueExcel() {
        const params = getRevenueFilterParams();
        const query = params.toString();

        window.location.href = query
            ? `/admin/revenue/export/excel?${query}`
            : '/admin/revenue/export/excel';
    }

    function printRevenueReport() {
        if (admin && typeof admin.activateSection === 'function') {
            admin.activateSection('revenue');
        }

        window.setTimeout(() => {
            window.print();
        }, 120);
    }

    function initRevenueActions() {
        const exportExcelButton = document.querySelector('#revenue [data-export-excel]');
        const printReportButton = document.querySelector('#revenue [data-print-report]');

        if (exportExcelButton) {
            exportExcelButton.addEventListener('click', exportRevenueExcel);
        }

        if (printReportButton) {
            printReportButton.addEventListener('click', printRevenueReport);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initRevenueActions);
    } else {
        initRevenueActions();
    }
})(window, document, window.GreenyAdmin || {});