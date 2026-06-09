(function (admin) {
    const normalizeText = admin.normalizeText;
    const readNumber = admin.readNumber;

    admin.setupCardFilter({
        sectionId: 'notifications',
        formSelector: '[data-notification-filter-form]',
        cardSelector: '.notification-row',
        emptySelector: '.notification-filter-empty',
        applySelector: '[data-apply-notification-filters]',
        resetSelector: '[data-reset-notification-filters]',
        match(card, fields) {
            const title = normalizeText(fields.get('title'));
            const type = fields.get('type') || 'all';
            const recipient = normalizeText(fields.get('recipient'));
            const readStatus = fields.get('readStatus') || 'all';
            const minRecipients = readNumber(fields.get('minRecipients'));
            const recipientCountValue = readNumber(card.dataset.recipientCount);
            const recipientCount = recipientCountValue === null ? 0 : recipientCountValue;
            const created = fields.get('created') || '';
            const text = normalizeText(`${card.dataset.title || ''} ${card.textContent || ''}`);
            return (!title || text.includes(title))
                    && (type === 'all' || card.dataset.type === type)
                    && (!recipient || normalizeText(card.textContent).includes(recipient))
                    && (readStatus === 'all' || card.dataset.readStatus === readStatus)
                    && (minRecipients === null || recipientCount >= minRecipients)
                    && admin.dateMatches(card.dataset.created || '', created);
        }
    });

    const notificationTargetType = document.getElementById('notificationTargetType');
    const notificationRoleField = document.querySelector('.notification-role-field');
    const notificationUserField = document.querySelector('.notification-user-field');
    const notificationAllWarning = document.querySelector('.notification-all-warning');

    function syncNotificationTargetFields() {
        if (!notificationTargetType || !notificationRoleField || !notificationUserField) {
            return;
        }
        notificationRoleField.classList.toggle('is-hidden', notificationTargetType.value !== 'ROLE');
        notificationUserField.classList.toggle('is-hidden', notificationTargetType.value !== 'USER');
        if (notificationAllWarning) {
            notificationAllWarning.hidden = notificationTargetType.value !== 'ALL';
        }
    }

    if (notificationTargetType) {
        notificationTargetType.addEventListener('change', syncNotificationTargetFields);
        syncNotificationTargetFields();
    }
})(window.GreenyAdmin || {});
