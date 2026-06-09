(function (window, document) {
    'use strict';

    const admin = window.GreenyAdmin = window.GreenyAdmin || {};

    const WEB_ONLY_PATTERNS = [
        /^\/admin\/categories\/[^/]+\/delete$/,
        /^\/admin\/coupons\/[^/]+\/delete$/
    ];

    const SECTION_BY_PREFIX = [
        ['categories', 'categories'],
        ['plants', 'plants'],
        ['variants', 'variants'],
        ['care-profiles', 'care'],
        ['inventory', 'inventory'],
        ['notifications', 'notifications'],
        ['reviews', 'reviews'],
        ['users', 'users'],
        ['articles', 'articles']
    ];

    async function api(path, options = {}) {
        const init = {
            method: options.method || 'GET',
            credentials: 'include',
            headers: options.headers ? {...options.headers} : {}
        };

        if (options.body !== undefined) {
            if (options.body instanceof URLSearchParams) {
                init.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8';
                init.body = options.body.toString();
            } else {
                init.headers['Content-Type'] = 'application/json';
                init.body = JSON.stringify(options.body);
            }
        }

        const response = await fetch(path, init);
        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch (error) {
                data = {message: text};
            }
        }

        if (!response.ok) {
            const message = data && data.message ? data.message : 'Xử lý thất bại';
            throw new Error(message);
        }

        return data;
    }

    function parsePath(action) {
        const url = new URL(action, window.location.origin);
        return url.pathname;
    }

    function isWebOnly(path) {
        return WEB_ONLY_PATTERNS.some((pattern) => pattern.test(path));
    }

    function resolveSection(path) {
        for (const [prefix, section] of SECTION_BY_PREFIX) {
            if (path.startsWith(`/admin/${prefix}`)) {
                return section;
            }
        }
        return null;
    }

    function readCheckbox(form, name, defaultValue = false) {
        const field = form.elements[name];
        if (!field) {
            return defaultValue;
        }
        if (field instanceof RadioNodeList) {
            return field.value === 'true' || field.value === 'on';
        }
        return field.checked;
    }

    function readValue(form, name, defaultValue = null) {
        const field = form.elements[name];
        if (!field) {
            return defaultValue;
        }
        const value = field.value;
        return value === '' ? defaultValue : value;
    }

    function readInteger(form, name, defaultValue = null) {
        const value = readValue(form, name, null);
        if (value === null || value === undefined || value === '') {
            return defaultValue;
        }
        const parsed = Number.parseInt(value, 10);
        return Number.isFinite(parsed) ? parsed : defaultValue;
    }

    function readDecimal(form, name, defaultValue = null) {
        const value = readValue(form, name, null);
        if (value === null || value === undefined || value === '') {
            return defaultValue;
        }
        const parsed = Number(value);
        return Number.isFinite(parsed) ? value : defaultValue;
    }

    function readBoolean(form, name, defaultValue = false) {
        const field = form.elements[name];
        if (!field) {
            return defaultValue;
        }
        if (field instanceof RadioNodeList) {
            if (field.type === 'checkbox') {
                return field.checked;
            }
            return field.value === 'true';
        }
        if (field.type === 'checkbox') {
            return field.checked;
        }
        return field.value === 'true';
    }

    function formToParams(form, names) {
        const params = new URLSearchParams();
        names.forEach((name) => {
            const field = form.elements[name];
            if (!field) {
                return;
            }
            if (field instanceof RadioNodeList && field.type === 'checkbox' && !field.checked) {
                return;
            }
            const value = field.value;
            if (value !== '') {
                params.set(name, value);
            }
        });
        return params;
    }

    function categoryBody(form) {
        return {
            title: readValue(form, 'title', ''),
            description: readValue(form, 'description'),
            imageUrl: readValue(form, 'imageUrl'),
            isActive: readBoolean(form, 'isActive', false),
            sortOrder: readInteger(form, 'sortOrder')
        };
    }

    function plantBody(form) {
        return {
            title: readValue(form, 'title', ''),
            sku: readValue(form, 'sku'),
            description: readValue(form, 'description'),
            scientificName: readValue(form, 'scientificName'),
            commonName: readValue(form, 'commonName'),
            plantType: readInteger(form, 'plantType'),
            origin: readValue(form, 'origin'),
            toxicity: readValue(form, 'toxicity'),
            petFriendly: readCheckbox(form, 'petFriendly'),
            airPurifying: readCheckbox(form, 'airPurifying'),
            categoryId: readValue(form, 'categoryId')
        };
    }

    function variantBody(form) {
        return {
            plantId: readValue(form, 'plantId', ''),
            name: readValue(form, 'name', ''),
            sku: readValue(form, 'sku'),
            heightCm: readInteger(form, 'heightCm'),
            potSize: readInteger(form, 'potSize'),
            price: readDecimal(form, 'price', '0'),
            salePrice: readDecimal(form, 'salePrice'),
            quantity: readInteger(form, 'quantity'),
            attribute: readValue(form, 'attribute'),
            isActive: readBoolean(form, 'isActive', false),
            seoDescription: readValue(form, 'seoDescription'),
            seoTitle: readValue(form, 'seoTitle')
        };
    }

    function couponBody(form) {
        return {
            code: readValue(form, 'code', ''),
            type: readInteger(form, 'type', 0),
            value: readDecimal(form, 'value', '0'),
            minOrderAmount: readDecimal(form, 'minOrderAmount'),
            maxDiscountAmount: readDecimal(form, 'maxDiscountAmount'),
            maxUses: readInteger(form, 'maxUses'),
            perUserLimit: readInteger(form, 'perUserLimit'),
            isActive: readBoolean(form, 'isActive', true),
            startsAt: readValue(form, 'startsAt'),
            expiresAt: readValue(form, 'expiresAt')
        };
    }

    function articleBody(form) {
        return {
            title: readValue(form, 'title', ''),
            slug: readValue(form, 'slug'),
            excerpt: readValue(form, 'excerpt'),
            content: readValue(form, 'content', ''),
            thumbnail: readValue(form, 'thumbnail')
        };
    }

    function orderStatusBody(form) {
        return {
            status: readInteger(form, 'status'),
            paymentStatus: readInteger(form, 'paymentStatus'),
            note: readValue(form, 'note'),
            estimatedDelivery: readValue(form, 'estimatedDelivery')
        };
    }

    function paymentBody(form) {
        return {
            transactionId: readValue(form, 'transactionId'),
            amount: readDecimal(form, 'amount'),
            method: readValue(form, 'method'),
            status: readInteger(form, 'status'),
            gatewayResponse: readValue(form, 'gatewayResponse'),
            paidAt: readValue(form, 'paidAt')
        };
    }

    function notificationBody(form) {
        return {
            targetType: readValue(form, 'targetType', 'ALL'),
            role: readInteger(form, 'role'),
            userId: readValue(form, 'userId'),
            userEmail: readValue(form, 'userEmail'),
            type: readInteger(form, 'type', 0),
            title: readValue(form, 'title', ''),
            messageText: readValue(form, 'messageText', ''),
            data: readValue(form, 'data')
        };
    }

    function reviewModerationBody(form) {
        return {
            isApproved: readBoolean(form, 'isApproved'),
            replyMessage: readValue(form, 'replyMessage')
        };
    }

    function userUpdateBody(form) {
        return {
            title: readValue(form, 'title'),
            phone: readValue(form, 'phone'),
            role: readInteger(form, 'role'),
            status: readInteger(form, 'status')
        };
    }

    function resolveApiRequest(form) {
        const path = parsePath(form.getAttribute('action') || '');
        if (!path.startsWith('/admin/') || isWebOnly(path)) {
            return null;
        }

        let match;

        match = path.match(/^\/admin\/categories$/);
        if (match) {
            return {method: 'POST', url: '/api/admin/categories', body: categoryBody(form)};
        }

        match = path.match(/^\/admin\/categories\/([^/]+)$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/categories/${match[1]}`, body: categoryBody(form)};
        }

        match = path.match(/^\/admin\/categories\/([^/]+)\/visibility$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/categories/${match[1]}`, body: categoryBody(form)};
        }

        match = path.match(/^\/admin\/plants$/);
        if (match) {
            return {method: 'POST', url: '/api/admin/plants', body: plantBody(form)};
        }

        match = path.match(/^\/admin\/plants\/([^/]+)$/);
        if (match) {
            return {
                method: 'PUT',
                url: `/api/admin/plants/${match[1]}`,
                body: plantBody(form),
                after: async () => {
                    if (form.elements.isVisible) {
                        const isVisible = readBoolean(form, 'isVisible', true);
                        await api(`/api/admin/plants/${match[1]}/visibility?isVisible=${isVisible}`, {method: 'PATCH'});
                    }
                }
            };
        }

        match = path.match(/^\/admin\/plants\/([^/]+)\/visibility$/);
        if (match) {
            const isVisible = readBoolean(form, 'isVisible', true);
            return {method: 'PATCH', url: `/api/admin/plants/${match[1]}/visibility?isVisible=${isVisible}`};
        }

        match = path.match(/^\/admin\/plants\/([^/]+)\/delete$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/plants/${match[1]}`};
        }

        match = path.match(/^\/admin\/variants$/);
        if (match) {
            return {method: 'POST', url: '/api/admin/variants', body: variantBody(form)};
        }

        match = path.match(/^\/admin\/variants\/([^/]+)$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/variants/${match[1]}`, body: variantBody(form)};
        }

        match = path.match(/^\/admin\/variants\/([^/]+)\/visibility$/);
        if (match) {
            const isActive = readBoolean(form, 'isActive', false);
            return {method: 'PATCH', url: `/api/admin/variants/${match[1]}/visibility?isActive=${isActive}`};
        }

        match = path.match(/^\/admin\/care-profiles$/);
        if (match) {
            return {
                method: 'POST',
                url: '/api/admin/care-profiles',
                body: formToParams(form, ['plantId', 'lightRequirement', 'wateringFrequency', 'humidityRequirement', 'careLevel', 'careInstruction'])
            };
        }

        match = path.match(/^\/admin\/care-profiles\/([^/]+)\/delete$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/care-profiles/${match[1]}`};
        }

        match = path.match(/^\/admin\/inventory\/stock$/);
        if (match) {
            const variantId = readValue(form, 'variantId', '');
            return {
                method: 'POST',
                url: `/api/admin/inventory/${variantId}/stock`,
                body: formToParams(form, ['movementType', 'amount'])
            };
        }

        match = path.match(/^\/admin\/inventory\/([^/]+)\/visibility$/);
        if (match) {
            const isActive = readBoolean(form, 'isActive', false);
            return {method: 'PATCH', url: `/api/admin/inventory/${match[1]}/visibility?isActive=${isActive}`};
        }

        match = path.match(/^\/admin\/orders\/([^/]+)\/status$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/orders/${match[1]}/status`, body: orderStatusBody(form)};
        }

        match = path.match(/^\/admin\/orders\/([^/]+)\/payment$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/orders/${match[1]}/payment`, body: paymentBody(form)};
        }

        match = path.match(/^\/admin\/notifications$/);
        if (match) {
            return {method: 'POST', url: '/api/admin/notifications', body: notificationBody(form)};
        }

        match = path.match(/^\/admin\/notifications\/([^/]+)\/delete$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/notifications/${match[1]}`};
        }

        match = path.match(/^\/admin\/reviews\/([^/]+)\/moderation$/);
        if (match) {
            return {method: 'POST', url: `/api/admin/reviews/${match[1]}/moderation`, body: reviewModerationBody(form)};
        }

        match = path.match(/^\/admin\/reviews\/([^/]+)\/delete$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/reviews/${match[1]}`};
        }

        match = path.match(/^\/admin\/users\/([^/]+)$/);
        if (match) {
            return {method: 'POST', url: `/api/admin/users/${match[1]}`, body: userUpdateBody(form)};
        }

        match = path.match(/^\/admin\/users\/([^/]+)\/delete$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/users/${match[1]}`};
        }

        match = path.match(/^\/admin\/coupons$/);
        if (match) {
            return {method: 'POST', url: '/api/admin/coupons', body: couponBody(form)};
        }

        match = path.match(/^\/admin\/coupons\/([^/]+)$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/coupons/${match[1]}`, body: couponBody(form)};
        }

        match = path.match(/^\/admin\/coupons\/([^/]+)\/deactivate$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/coupons/${match[1]}`};
        }

        match = path.match(/^\/admin\/articles$/);
        if (match) {
            return {method: 'POST', url: '/api/admin/articles', body: articleBody(form)};
        }

        match = path.match(/^\/admin\/articles\/([^/]+)$/);
        if (match) {
            return {method: 'PUT', url: `/api/admin/articles/${match[1]}`, body: articleBody(form)};
        }

        match = path.match(/^\/admin\/articles\/([^/]+)\/delete$/);
        if (match) {
            return {method: 'DELETE', url: `/api/admin/articles/${match[1]}`};
        }

        return null;
    }

    function defaultSuccessMessage(method) {
        switch (method) {
            case 'POST':
                return 'Đã lưu thành công.';
            case 'PUT':
                return 'Đã cập nhật.';
            case 'PATCH':
                return 'Đã cập nhật.';
            case 'DELETE':
                return 'Đã xóa.';
            default:
                return 'Đã xử lý xong.';
        }
    }

	function redirectAfterSuccess(form, response, request) {
	    const path = parsePath(form.getAttribute('action') || '');
	    const section = resolveSection(path);
	    const message = response && response.message ? response.message : defaultSuccessMessage(request.method);

	    sessionStorage.setItem('greenyAdminFlash', JSON.stringify({
	        type: 'success',
	        message
	    }));

	    const targetHash = section ? `#${section}` : '';
	    const targetPath = '/admin';

	    if (window.location.pathname === targetPath) {
	        if (targetHash && window.location.hash !== targetHash) {
	            window.location.hash = targetHash;
	        }

	        window.location.reload();
	        return;
	    }

	    window.location.href = `${targetPath}${targetHash}`;
	}

    function setSubmitting(form, submitting) {
        const button = form.querySelector('button[type="submit"]');
        if (!button) {
            return;
        }
        if (submitting) {
            button.dataset.originalText = button.textContent;
            button.textContent = button.dataset.loadingText || 'Đang xử lý...';
            button.classList.add('is-loading');
            button.disabled = true;
            return;
        }
        if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
        }
        button.classList.remove('is-loading');
        button.disabled = false;
    }

    async function handleApiFormSubmit(event) {
        const form = event.target;
        if (!(form instanceof HTMLFormElement) || !form.classList.contains('js-loading-form')) {
            return;
        }

        const request = resolveApiRequest(form);
        if (!request) {
            return;
        }

		if (form.dataset.confirm) {
		    event.preventDefault();
		    event.stopImmediatePropagation();

		    const accepted = admin.showConfirm
		        ? await admin.showConfirm(form.dataset.confirm || 'Bạn có chắc chắn muốn tiếp tục?', {
		            title: 'Xác nhận xóa dữ liệu',
		            acceptText: 'Xóa',
		            cancelText: 'Hủy'
		        })
		        : window.confirm(form.dataset.confirm || 'Bạn có chắc chắn muốn tiếp tục?');

		    if (!accepted) {
		        return;
		    }

		    form.removeAttribute('data-confirm');
		    form.requestSubmit();
		    return;
		}

        event.preventDefault();
        event.stopImmediatePropagation();
        setSubmitting(form, true);

        try {
            const response = await api(request.url, {
                method: request.method,
                body: request.body
            });
            if (typeof request.after === 'function') {
                await request.after();
            }
            redirectAfterSuccess(form, response, request);
        } catch (error) {
            setSubmitting(form, false);
            sessionStorage.setItem('greenyAdminFlash', JSON.stringify({
                type: 'warning',
                message: error.message || 'Xử lý thất bại'
            }));
            window.location.reload();
        }
    }

    document.addEventListener('submit', handleApiFormSubmit, true);

    admin.api = api;
    admin.resolveApiRequest = resolveApiRequest;
})(window, document);
