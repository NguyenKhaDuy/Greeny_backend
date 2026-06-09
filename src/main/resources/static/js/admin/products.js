(function (admin) {
    const normalizeText = admin.normalizeText;

    admin.plantCatalog = admin.setupCatalogSection({
        sectionId: 'plants',
        rowSelector: '.plant-summary-row',
        formSelector: '[data-plant-filter-form]',
        emptySelector: '.plant-filter-empty',
        applySelector: '[data-apply-plant-filters]',
        resetSelector: '[data-reset-plant-filters]',
        match(row, fields) {
            const title = normalizeText(fields.get('title'));
            const sku = normalizeText(fields.get('sku'));
            const categoryId = fields.get('categoryId') || 'all';
            const visibility = fields.get('visibility') || 'all';
            const variantState = fields.get('variantState') || 'all';
            const toxicity = normalizeText(fields.get('toxicity'));
            const variantCount = Number(row.dataset.variants || 0);
            return (!title || normalizeText(row.dataset.search).includes(title))
                    && (!sku || normalizeText(row.dataset.sku).includes(sku))
                    && (categoryId === 'all' || row.dataset.category === categoryId)
                    && (visibility === 'all' || row.dataset.visibility === visibility)
                    && (variantState === 'all' || (variantState === 'has' ? variantCount > 0 : variantCount === 0))
                    && (!toxicity || normalizeText(row.dataset.toxicity).includes(toxicity));
        }
    });
})(window.GreenyAdmin || {});
