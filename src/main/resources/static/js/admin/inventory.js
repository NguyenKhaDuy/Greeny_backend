(function (admin) {
    const normalizeText = admin.normalizeText;
    const readNumber = admin.readNumber;

    admin.setupCardFilter({
        sectionId: 'inventory',
        formSelector: '[data-inventory-filter-form]',
        cardSelector: '.inventory-card',
        emptySelector: '.inventory-filter-empty',
        applySelector: '[data-apply-inventory-filters]',
        resetSelector: '[data-reset-inventory-filters]',
        match(card, fields) {
            const plant = normalizeText(fields.get('plant'));
            const sku = normalizeText(fields.get('sku'));
            const categoryId = fields.get('categoryId') || 'all';
            const status = fields.get('status') || 'all';
            const minQuantity = readNumber(fields.get('minQuantity'));
            const maxQuantity = readNumber(fields.get('maxQuantity'));
            const quantityValue = readNumber(card.dataset.quantity);
            const quantity = quantityValue === null ? 0 : quantityValue;
            return (!plant || normalizeText(card.dataset.plant).includes(plant))
                    && (!sku || normalizeText(card.dataset.sku).includes(sku))
                    && (categoryId === 'all' || card.dataset.category === categoryId)
                    && (status === 'all' || card.dataset.status === status)
                    && (minQuantity === null || quantity >= minQuantity)
                    && (maxQuantity === null || quantity <= maxQuantity);
        }
    });
})(window.GreenyAdmin || {});
