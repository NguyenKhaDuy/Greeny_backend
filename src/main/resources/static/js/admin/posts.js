(function (admin) {
    const normalizeText = admin.normalizeText;

    admin.setupCardFilter({
        sectionId: 'articles',
        formSelector: '[data-article-filter-form]',
        cardSelector: '.article-row',
        emptySelector: '.article-filter-empty',
        applySelector: '[data-apply-article-filters]',
        resetSelector: '[data-reset-article-filters]',
        match(card, fields) {
            const title = normalizeText(fields.get('title'));
            const slug = normalizeText(fields.get('slug'));
            const status = fields.get('status') || 'all';
            const created = fields.get('created') || '';
            return (!title || normalizeText(card.dataset.title).includes(title))
                    && (!slug || normalizeText(card.dataset.slug).includes(slug))
                    && (status === 'all' || card.dataset.status === status)
                    && admin.dateMatches(card.dataset.created || '', created);
        }
    });
})(window.GreenyAdmin || {});
