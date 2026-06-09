(function (admin) {
    const normalizeText = admin.normalizeText;

    admin.setupCardFilter({
        sectionId: 'reviews',
        formSelector: '[data-review-filter-form]',
        cardSelector: '.review-card',
        emptySelector: '.review-filter-empty',
        applySelector: '[data-apply-review-filters]',
        resetSelector: '[data-reset-review-filters]',
        match(card, fields) {
            const product = normalizeText(fields.get('product'));
            const customer = normalizeText(fields.get('customer'));
            const rating = fields.get('rating') || 'all';
            const status = fields.get('status') || 'all';
            const created = fields.get('created') || '';
            return (!product || normalizeText(card.dataset.product).includes(product))
                    && (!customer || normalizeText(card.dataset.customer).includes(customer))
                    && (rating === 'all' || card.dataset.rating === rating)
                    && (status === 'all' || card.dataset.status === status)
                    && admin.dateMatches(card.dataset.created || '', created);
        }
    });
})(window.GreenyAdmin || {});
