(function (admin) {
    const normalizeText = admin.normalizeText;

    admin.setupCardFilter({
        sectionId: 'care',
        formSelector: '[data-care-filter-form]',
        cardSelector: '.care-card',
        emptySelector: '.care-filter-empty',
        applySelector: '[data-apply-care-filters]',
        resetSelector: '[data-reset-care-filters]',
        match(card, fields) {
            const plant = normalizeText(fields.get('plant'));
            const level = fields.get('level') || 'all';
            const light = normalizeText(fields.get('light'));
            const water = normalizeText(fields.get('water'));
            const humidity = normalizeText(fields.get('humidity'));
            return (!plant || normalizeText(card.dataset.plant).includes(plant))
                    && (level === 'all' || card.dataset.level === level)
                    && (!light || normalizeText(card.dataset.light).includes(light))
                    && (!water || normalizeText(card.dataset.water).includes(water))
                    && (!humidity || normalizeText(card.dataset.humidity).includes(humidity));
        }
    });
})(window.GreenyAdmin || {});
