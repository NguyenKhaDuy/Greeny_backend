const state = {
    user: null,
    categories: [],
    page: 0,
    plantsPage: null,
    selectedPlant: null,
    cart: null,
    couponDiscount: 0,
    lastOrder: null,
    addresses: [],
    orders: [],
    notifications: [],
    reviews: [],
    selectedCartItemIds: [],
    aiChat: {
        open: false,
        conversationId: null,
        messages: [],
        limits: null,
        loading: false
    }
};

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
    const init = {
        method: options.method || 'GET',
        credentials: 'include',
        headers: options.headers ? {...options.headers} : {}
    };
    if (options.body !== undefined) {
        init.headers['Content-Type'] = 'application/json';
        init.body = JSON.stringify(options.body);
    }

    const response = await fetch(path, init);
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new Error(data && data.message ? data.message : 'Xu ly that bai');
    }
    return data;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function formatMoney(value) {
    const number = Number(value || 0);
    return number.toLocaleString('vi-VN') + ' VND';
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString('vi-VN');
}

function setStatus(id, text, kind = '') {
    const element = $(id);
    if (!element) {
        return;
    }
    element.textContent = text || '';
    element.className = `state-line ${kind}`.trim();
}

function showToast(message, kind = '') {
    const toast = $('toast');
    toast.textContent = message;
    toast.className = `toast ${kind}`.trim();
    toast.hidden = false;
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => {
        toast.hidden = true;
    }, 3200);
}

function badge(text, status) {
    return `<span class="badge ${status || ''}">${escapeHtml(text)}</span>`;
}

function imageBlock(url, label, className = 'product-image') {
    if (url) {
        return `<div class="${className}"><img src="${escapeHtml(url)}" alt="${escapeHtml(label || '')}"></div>`;
    }
    const initial = (label || 'G').trim().slice(0, 1).toUpperCase();
    return `<div class="${className}" aria-hidden="true">${escapeHtml(initial)}</div>`;
}

function bindTabs() {
    document.querySelectorAll('[data-view]').forEach((button) => {
        button.addEventListener('click', () => activateView(button.dataset.view));
    });
}

async function activateView(view) {
    document.querySelectorAll('[data-view]').forEach((button) => {
        button.classList.toggle('active', button.dataset.view === view);
    });
    document.querySelectorAll('.view').forEach((section) => {
        section.classList.toggle('is-active', section.id === `${view}View`);
    });
    if (view !== 'catalog' && !state.user) {
        showToast('Can dang nhap de dung chuc nang nay', 'error');
        return;
    }
    if (view === 'cart') await loadCart();
    if (view === 'checkout') await loadCheckoutData();
    if (view === 'account') await loadAccountData();
    if (view === 'orders') await loadOrders();
    if (view === 'notifications') await loadNotifications();
    if (view === 'reviews') await loadReviews();
}

async function init() {
    bindTabs();
    bindAiChat();
    bindStaticForms();
    await loadSession();
    await loadCategories();
    await loadProducts();
}

async function loadSession() {
    try {
        const user = await api('/api/auth/me');
        state.user = user;
        $('sessionLabel').textContent = user.title || user.email || 'Da dang nhap';
        $('logoutButton').hidden = false;
        $('loginPanel').classList.add('is-hidden');
        await Promise.allSettled([loadCart(), loadUnreadCount(), loadAiHistory(), loadAiLimits()]);
    } catch (error) {
        state.user = null;
        $('sessionLabel').textContent = 'Chua dang nhap';
        $('logoutButton').hidden = true;
        $('loginPanel').classList.remove('is-hidden');
        updateCartCount(0);
        $('unreadCount').textContent = '0';
        resetAiChat();
    }
}

async function loadCategories() {
    try {
        state.categories = await api('/api/shop/categories');
        const options = ['<option value="">Tat ca</option>'].concat(
            state.categories.map((category) => `<option value="${escapeHtml(category.categoryId)}">${escapeHtml(category.title)}</option>`)
        );
        $('categoryFilter').innerHTML = options.join('');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadProducts() {
    const form = $('catalogFilters');
    const data = new FormData(form);
    const params = new URLSearchParams();
    for (const [key, value] of data.entries()) {
        if (key === 'inStock') {
            continue;
        }
        if (value !== null && String(value).trim() !== '') {
            params.set(key, value);
        }
    }
    if (form.inStock.checked) {
        params.set('inStock', 'true');
    }
    params.set('page', state.page);
    params.set('size', '12');

    setStatus('catalogStatus', 'Dang tai san pham...');
    $('productGrid').innerHTML = '';
    try {
        state.plantsPage = await api(`/api/shop/plants?${params.toString()}`);
        renderProducts();
    } catch (error) {
        setStatus('catalogStatus', error.message, 'error');
    }
}

function renderProducts() {
    const page = state.plantsPage;
    const products = page ? page.items : [];
    if (!products.length) {
        setStatus('catalogStatus', 'Khong co san pham phu hop.');
        $('productGrid').innerHTML = '';
    } else {
        setStatus('catalogStatus', `${page.totalItems} san pham`);
        $('productGrid').innerHTML = products.map((plant) => `
            <article class="product-card">
                ${imageBlock(plant.imageUrl, plant.title)}
                <div class="product-body">
                    <h3>${escapeHtml(plant.title)}</h3>
                    <p class="muted">${escapeHtml((plant.description || '').slice(0, 110))}</p>
                    <div class="product-meta">
                        ${badge(plant.categoryTitle || 'Chua phan loai')}
                        ${badge(`${plant.totalStock || 0} ton`, plant.totalStock > 0 ? 'success' : 'danger')}
                        ${badge(`${plant.averageRating || 0}/5 (${plant.totalReviews || 0})`, 'warning')}
                    </div>
                    <strong>${formatMoney(plant.minPrice)} - ${formatMoney(plant.maxPrice)}</strong>
                    <button class="primary-button" type="button" data-detail="${escapeHtml(plant.plantId)}">Chi tiet</button>
                </div>
            </article>
        `).join('');
    }
    $('pageLabel').textContent = page ? `${page.page + 1} / ${Math.max(page.totalPages, 1)}` : '0 / 0';
    $('prevPage').disabled = !page || page.first;
    $('nextPage').disabled = !page || page.last;
    document.querySelectorAll('[data-detail]').forEach((button) => {
        button.addEventListener('click', () => openProduct(button.dataset.detail));
    });
}

async function openProduct(plantId) {
    $('productDetail').hidden = false;
    $('productDetail').innerHTML = '<p class="state-line">Dang tai chi tiet...</p>';
    try {
        state.selectedPlant = await api(`/api/shop/plants/${plantId}`);
        renderProductDetail();
        $('productDetail').scrollIntoView({behavior: 'smooth', block: 'start'});
    } catch (error) {
        $('productDetail').innerHTML = `<p class="state-line error">${escapeHtml(error.message)}</p>`;
    }
}

function renderProductDetail() {
    const plant = state.selectedPlant;
    const variants = plant.variants || [];
    const defaultVariant = variants.find((variant) => variant.quantity > 0) || variants[0];
    state.selectedVariantId = defaultVariant ? defaultVariant.variantId : null;
    $('productDetail').innerHTML = `
        <div class="detail-grid">
            ${imageBlock(plant.imageUrl, plant.title, 'detail-image')}
            <div class="detail-content">
                <div>
                    <h2>${escapeHtml(plant.title)}</h2>
                    <p class="muted">${escapeHtml(plant.description || '')}</p>
                </div>
                <div class="product-meta">
                    ${badge(plant.categoryTitle || 'Chua phan loai')}
                    ${badge(`${plant.totalStock || 0} ton`, plant.totalStock > 0 ? 'success' : 'danger')}
                    ${badge(`${plant.averageRating || 0}/5 (${plant.totalReviews || 0})`, 'warning')}
                </div>
                <label>Bien the
                    <select id="detailVariant">
                        ${variants.map((variant) => `
                            <option value="${escapeHtml(variant.variantId)}" ${variant.variantId === state.selectedVariantId ? 'selected' : ''}>
                                ${escapeHtml(variant.name)} - ${formatMoney(variant.effectivePrice)} - ton ${variant.quantity || 0}
                            </option>
                        `).join('')}
                    </select>
                </label>
                <label>So luong<input id="detailQuantity" type="number" min="1" value="1"></label>
                <div class="row-actions">
                    <button id="addToCartButton" class="primary-button" type="button">Them vao gio</button>
                    <button id="buyNowButton" class="ghost-button" type="button">Mua ngay</button>
                </div>
                <div class="variant-grid">
                    ${variants.map((variant) => `
                        <div class="variant-row">
                            <div>
                                <strong>${escapeHtml(variant.name)}</strong>
                                <p class="muted">${escapeHtml(variant.attribute || variant.sku || '')}</p>
                            </div>
                            <div>
                                <strong>${formatMoney(variant.effectivePrice)}</strong>
                                ${badge(variant.quantity > 0 ? 'Con hang' : 'Het hang', variant.quantity > 0 ? 'success' : 'danger')}
                            </div>
                        </div>
                    `).join('')}
                </div>
                <div>
                    <h3>Danh gia</h3>
                    <div class="mini-list">
                        ${(plant.reviews || []).map((review) => `
                            <div class="mini-row">
                                <div>
                                    <strong>${escapeHtml(review.userName || 'Khach hang')} - ${review.rating}/5</strong>
                                    <p>${escapeHtml(review.comment)}</p>
                                </div>
                                <span class="muted">${escapeHtml(formatDate(review.createdAt))}</span>
                            </div>
                        `).join('') || '<p class="state-line">Chua co danh gia.</p>'}
                    </div>
                </div>
            </div>
        </div>
    `;
    $('detailVariant').addEventListener('change', (event) => {
        state.selectedVariantId = event.target.value;
    });
    $('addToCartButton').addEventListener('click', () => addSelectedVariant(false));
    $('buyNowButton').addEventListener('click', () => addSelectedVariant(true));
}

async function addSelectedVariant(goCheckout) {
    if (!state.user) {
        showToast('Can dang nhap truoc khi them gio hang', 'error');
        return;
    }
    const quantity = Number($('detailQuantity').value || 1);
    try {
        await api('/api/user/cart', {
            method: 'POST',
            body: {variantId: state.selectedVariantId, quantity}
        });
        showToast('Da them vao gio hang');
        await loadCart();
        if (goCheckout) {
            await activateView('checkout');
        }
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadCart() {
    if (!state.user) {
        setStatus('cartStatus', 'Can dang nhap de xem gio hang.');
        return;
    }
    try {
        state.cart = await api('/api/user/cart');
        reconcileSelectedCartItems();
        updateCartCount(state.cart.totalItems || 0);
        renderCart();
        renderCheckoutSummary();
    } catch (error) {
        setStatus('cartStatus', error.message, 'error');
    }
}

function updateCartCount(count) {
    $('cartCount').textContent = count;
}

function renderCart() {
    const cart = state.cart;
    const items = cart ? cart.items : [];
    $('cartSubtotal').textContent = formatMoney(cart ? cart.subtotal : 0);
    $('cartTotal').textContent = formatMoney(cart ? cart.totalPrice : 0);
    if (!items.length) {
        setStatus('cartStatus', 'Gio hang dang rong.');
        $('cartList').innerHTML = '';
        return;
    }
    setStatus('cartStatus', `${items.length} san pham trong gio.`);
    $('cartList').innerHTML = items.map((item) => `
        <article class="cart-row">
            <label class="select-line" title="Chon san pham nay de dat hang">
                <input data-cart-select="${escapeHtml(item.cartItemId)}" type="checkbox" ${isCartItemSelected(item.cartItemId) ? 'checked' : ''}>
            </label>
            ${imageBlock(item.imageUrl, item.plantTitle, 'item-image')}
            <div class="item-main">
                <h3>${escapeHtml(item.plantTitle)} - ${escapeHtml(item.variantName)}</h3>
                <p>${formatMoney(item.unitPrice)} / ton ${item.stock}</p>
                ${item.inStock ? badge('Hop le', 'success') : badge('Can cap nhat ton kho', 'danger')}
            </div>
            <div class="row-actions">
                <input data-cart-qty="${escapeHtml(item.cartItemId)}" type="number" min="1" value="${item.quantity}">
                <button class="ghost-button" data-cart-update="${escapeHtml(item.cartItemId)}" type="button">Cap nhat</button>
                <button class="ghost-button danger" data-cart-remove="${escapeHtml(item.cartItemId)}" type="button">Xoa</button>
            </div>
        </article>
    `).join('');
    document.querySelectorAll('[data-cart-update]').forEach((button) => {
        button.addEventListener('click', () => updateCartItem(button.dataset.cartUpdate));
    });
    document.querySelectorAll('[data-cart-remove]').forEach((button) => {
        button.addEventListener('click', () => removeCartItem(button.dataset.cartRemove));
    });
    document.querySelectorAll('[data-cart-select]').forEach((input) => {
        input.addEventListener('change', () => toggleCartItemSelection(input.dataset.cartSelect, input.checked));
    });
}

function reconcileSelectedCartItems() {
    const items = state.cart && state.cart.items ? state.cart.items : [];
    const availableIds = items.map((item) => item.cartItemId);
    if (!availableIds.length) {
        state.selectedCartItemIds = [];
        return;
    }
    const stillSelected = state.selectedCartItemIds.filter((cartItemId) => availableIds.includes(cartItemId));
    state.selectedCartItemIds = stillSelected.length ? stillSelected : availableIds;
}

function isCartItemSelected(cartItemId) {
    return state.selectedCartItemIds.includes(cartItemId);
}

function toggleCartItemSelection(cartItemId, checked) {
    if (checked && !state.selectedCartItemIds.includes(cartItemId)) {
        state.selectedCartItemIds = [...state.selectedCartItemIds, cartItemId];
    }
    if (!checked) {
        state.selectedCartItemIds = state.selectedCartItemIds.filter((selectedId) => selectedId !== cartItemId);
    }
    state.couponDiscount = 0;
    renderCheckoutSummary();
}

function selectedCartItems() {
    const items = state.cart && state.cart.items ? state.cart.items : [];
    return items.filter((item) => isCartItemSelected(item.cartItemId));
}

function selectedCartSubtotal() {
    return selectedCartItems()
        .map((item) => Number(item.lineTotal || 0))
        .reduce((sum, value) => sum + value, 0);
}

async function updateCartItem(cartItemId) {
    const input = document.querySelector(`[data-cart-qty="${cartItemId}"]`);
    try {
        state.cart = await api(`/api/user/cart/${cartItemId}`, {
            method: 'PUT',
            body: {quantity: Number(input.value || 1)}
        });
        showToast('Da cap nhat gio hang');
        renderCart();
        renderCheckoutSummary();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function removeCartItem(cartItemId) {
    try {
        state.cart = await api(`/api/user/cart/${cartItemId}`, {method: 'DELETE'});
        reconcileSelectedCartItems();
        state.couponDiscount = 0;
        showToast('Da xoa san pham khoi gio');
        renderCart();
        renderCheckoutSummary();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadCheckoutData() {
    await Promise.allSettled([loadCart(), loadAddresses()]);
    renderCheckoutSummary();
}

function renderCheckoutSummary() {
    const cart = state.cart;
    const items = selectedCartItems();
    const subtotal = selectedCartSubtotal();
    const shipping = items.length ? Number(cart.shippingFee || 0) : 0;
    const discount = Math.min(Number(state.couponDiscount || 0), subtotal);
    const total = Math.max(subtotal - discount + shipping, 0);
    $('checkoutItems').innerHTML = items.map((item) => `
        <div class="mini-row">
            <div>
                <strong>${escapeHtml(item.plantTitle)} - ${escapeHtml(item.variantName)}</strong>
                <p>${item.quantity} x ${formatMoney(item.unitPrice)}</p>
            </div>
            <strong>${formatMoney(item.lineTotal)}</strong>
        </div>
    `).join('') || '<p class="state-line">Chua chon san pham nao de dat.</p>';
    $('checkoutSubtotal').textContent = formatMoney(subtotal);
    $('checkoutDiscount').textContent = formatMoney(discount);
    $('checkoutShipping').textContent = formatMoney(shipping);
    $('checkoutTotal').textContent = formatMoney(total);
}

async function loadAccountData() {
    await Promise.allSettled([loadProfile(), loadAddresses()]);
}

async function loadProfile() {
    try {
        const profile = await api('/api/user/profile');
        const form = $('profileForm');
        form.title.value = profile.title || '';
        form.email.value = profile.email || '';
        form.phone.value = profile.phone || '';
        form.avatar.value = profile.avatar || '';
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadAddresses() {
    if (!state.user) {
        return;
    }
    try {
        state.addresses = await api('/api/user/addresses');
        renderAddressOptions();
        renderAddressList();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function renderAddressOptions() {
    const options = ['<option value="">Them dia chi moi tu form ben duoi</option>'].concat(
        state.addresses.map((address) => `
            <option value="${escapeHtml(address.addressId)}" ${address.isDefault ? 'selected' : ''}>
                ${escapeHtml(address.receiverName)} - ${escapeHtml(address.fullAddress)}
            </option>
        `)
    );
    $('checkoutAddress').innerHTML = options.join('');
}

function renderAddressList() {
    $('addressList').innerHTML = state.addresses.map((address) => `
        <article class="notification-row">
            <div class="item-main">
                <h3>${escapeHtml(address.receiverName)} ${address.isDefault ? badge('Mac dinh', 'success') : ''}</h3>
                <p>${escapeHtml(address.receiverPhone)} - ${escapeHtml(address.fullAddress)}</p>
            </div>
            <div class="row-actions">
                <button class="ghost-button" data-address-edit="${escapeHtml(address.addressId)}" type="button">Sua</button>
                <button class="ghost-button" data-address-default="${escapeHtml(address.addressId)}" type="button">Mac dinh</button>
                <button class="ghost-button danger" data-address-delete="${escapeHtml(address.addressId)}" type="button">Xoa</button>
            </div>
        </article>
    `).join('') || '<p class="state-line">Chua co dia chi giao hang.</p>';
    document.querySelectorAll('[data-address-edit]').forEach((button) => button.addEventListener('click', () => editAddress(button.dataset.addressEdit)));
    document.querySelectorAll('[data-address-default]').forEach((button) => button.addEventListener('click', () => setDefaultAddress(button.dataset.addressDefault)));
    document.querySelectorAll('[data-address-delete]').forEach((button) => button.addEventListener('click', () => deleteAddress(button.dataset.addressDelete)));
}

function editAddress(addressId) {
    const address = state.addresses.find((item) => item.addressId === addressId);
    if (!address) {
        return;
    }
    const form = $('addressForm');
    form.addressId.value = address.addressId;
    form.receiverName.value = address.receiverName || '';
    form.receiverPhone.value = address.receiverPhone || '';
    form.addressDetail.value = address.addressDetail || '';
    form.wardName.value = address.wardName || '';
    form.districtName.value = address.districtName || '';
    form.provinceName.value = address.provinceName || '';
    form.isDefault.checked = Boolean(address.isDefault);
    activateView('account');
}

async function setDefaultAddress(addressId) {
    try {
        await api(`/api/user/addresses/${addressId}/default`, {method: 'PATCH'});
        showToast('Da dat dia chi mac dinh');
        await loadAddresses();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function deleteAddress(addressId) {
    try {
        await api(`/api/user/addresses/${addressId}`, {method: 'DELETE'});
        showToast('Da xoa dia chi');
        await loadAddresses();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadOrders() {
    try {
        state.orders = await api('/api/user/orders');
        renderOrders();
    } catch (error) {
        setStatus('ordersStatus', error.message, 'error');
    }
}

function renderOrders() {
    if (!state.orders.length) {
        setStatus('ordersStatus', 'Chua co don hang.');
        $('orderList').innerHTML = '';
        return;
    }
    setStatus('ordersStatus', `${state.orders.length} don hang`);
    $('orderList').innerHTML = state.orders.map((order) => `
        <article class="order-card">
            <div class="order-top">
                <div>
                    <h3>Don ${escapeHtml(shortId(order.orderId))}</h3>
                    <p>${escapeHtml(formatDate(order.createdAt))} - ${escapeHtml(order.shippingAddress || '')}</p>
                </div>
                <div class="row-actions">
                    ${badge(order.statusLabel, order.status === 4 ? 'success' : (order.status === 5 ? 'danger' : 'warning'))}
                    ${badge(order.paymentStatusLabel, order.paymentStatus === 1 ? 'success' : (order.paymentStatus === 2 ? 'danger' : 'warning'))}
                </div>
            </div>
            <div class="order-money">
                <strong>${formatMoney(order.totalPrice)}</strong>
                <div class="row-actions">
                    ${order.canCancel ? `<button class="ghost-button danger" data-order-cancel="${escapeHtml(order.orderId)}" type="button">Huy don</button>` : ''}
                    ${order.payment && order.payment.method && order.payment.method.startsWith('ONLINE') && order.payment.status === 0 ? `
                        <button class="ghost-button" data-order-pay-success="${escapeHtml(order.orderId)}" type="button">Mock paid</button>
                        <button class="ghost-button danger" data-order-pay-fail="${escapeHtml(order.orderId)}" type="button">Mock fail</button>
                    ` : ''}
                </div>
            </div>
            <div class="order-items">
                ${(order.items || []).map((item) => `
                    <div class="mini-row">
                        <div>
                            <strong>${escapeHtml(item.plantTitle)} - ${escapeHtml(item.variantName)}</strong>
                            <p>${item.quantity} x ${formatMoney(item.unitPrice)}</p>
                        </div>
                        <div class="row-actions">
                            <strong>${formatMoney(item.totalPrice)}</strong>
                            ${order.canReview ? `<button class="ghost-button" data-review-start="${escapeHtml(order.orderId)}" data-review-plant="${escapeHtml(item.plantId)}" type="button">Danh gia</button>` : ''}
                        </div>
                    </div>
                `).join('')}
            </div>
            <div class="timeline">
                ${(order.histories || []).map((history) => `
                    <div class="timeline-row">
                        <strong>${escapeHtml(formatDate(history.createdAt))}</strong>
                        <span>${escapeHtml(history.newStatusLabel)}${history.note ? ' - ' + escapeHtml(history.note) : ''}</span>
                    </div>
                `).join('') || '<p class="state-line">Chua co timeline.</p>'}
            </div>
        </article>
    `).join('');
    document.querySelectorAll('[data-order-cancel]').forEach((button) => button.addEventListener('click', () => cancelOrder(button.dataset.orderCancel)));
    document.querySelectorAll('[data-order-pay-success]').forEach((button) => button.addEventListener('click', () => mockPayment(button.dataset.orderPaySuccess, 'SUCCESS')));
    document.querySelectorAll('[data-order-pay-fail]').forEach((button) => button.addEventListener('click', () => mockPayment(button.dataset.orderPayFail, 'FAILED')));
    document.querySelectorAll('[data-review-start]').forEach((button) => {
        button.addEventListener('click', () => startReview(button.dataset.reviewStart, button.dataset.reviewPlant));
    });
}

async function cancelOrder(orderId) {
    try {
        await api(`/api/user/orders/${orderId}/cancel`, {method: 'POST'});
        showToast('Da huy don hang');
        await Promise.allSettled([loadOrders(), loadNotifications(), loadCart()]);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function mockPayment(orderId, result) {
    try {
        state.lastOrder = await api(`/api/user/orders/${orderId}/payment/mock`, {
            method: 'POST',
            body: {result}
        });
        showToast(result === 'SUCCESS' ? 'Thanh toan thanh cong' : 'Thanh toan that bai', result === 'SUCCESS' ? '' : 'error');
        $('mockPaymentBox').hidden = true;
        await Promise.allSettled([loadOrders(), loadNotifications()]);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadUnreadCount() {
    if (!state.user) {
        $('unreadCount').textContent = '0';
        return;
    }
    try {
        const response = await api('/api/user/notifications/unread-count');
        $('unreadCount').textContent = response.unreadCount || 0;
    } catch (error) {
        $('unreadCount').textContent = '0';
    }
}

async function loadNotifications() {
    try {
        state.notifications = await api('/api/user/notifications');
        renderNotifications();
        await loadUnreadCount();
    } catch (error) {
        setStatus('notificationStatus', error.message, 'error');
    }
}

function renderNotifications() {
    if (!state.notifications.length) {
        setStatus('notificationStatus', 'Chua co thong bao.');
        $('notificationList').innerHTML = '';
        return;
    }
    setStatus('notificationStatus', `${state.notifications.length} thong bao`);
    $('notificationList').innerHTML = state.notifications.map((notification) => `
        <article class="notification-row ${notification.isRead ? '' : 'unread'}">
            <div>
                <h3>${escapeHtml(notification.title)}</h3>
                <p>${escapeHtml(notification.messageText)}</p>
                <p class="muted">${escapeHtml(formatDate(notification.createdAt || notification.sendingTime))}</p>
            </div>
            <div class="row-actions">
                ${notification.isRead ? badge('Da doc') : badge('Chua doc', 'warning')}
                ${notification.isRead ? '' : `<button class="ghost-button" data-noti-read="${escapeHtml(notification.notificationUserId)}" type="button">Da doc</button>`}
            </div>
        </article>
    `).join('');
    document.querySelectorAll('[data-noti-read]').forEach((button) => button.addEventListener('click', () => markNotificationRead(button.dataset.notiRead)));
}

async function markNotificationRead(notificationUserId) {
    try {
        await api(`/api/user/notifications/${notificationUserId}/read`, {method: 'PATCH'});
        await loadNotifications();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function loadReviews() {
    try {
        state.reviews = await api('/api/user/reviews');
        renderReviews();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function renderReviews() {
    $('reviewList').innerHTML = state.reviews.map((review) => `
        <article class="review-row">
            <div>
                <h3>${escapeHtml(review.plantTitle || 'San pham')} - ${review.rating}/5</h3>
                <p>${escapeHtml(review.comment)}</p>
                <p class="muted">Don ${escapeHtml(shortId(review.orderId))} - ${escapeHtml(formatDate(review.createdAt))}</p>
            </div>
            <div class="row-actions">
                <button class="ghost-button" data-review-edit="${escapeHtml(review.reviewId)}" type="button">Sua</button>
                <button class="ghost-button danger" data-review-delete="${escapeHtml(review.reviewId)}" type="button">Xoa</button>
            </div>
        </article>
    `).join('') || '<p class="state-line">Chua co danh gia.</p>';
    document.querySelectorAll('[data-review-edit]').forEach((button) => button.addEventListener('click', () => editReview(button.dataset.reviewEdit)));
    document.querySelectorAll('[data-review-delete]').forEach((button) => button.addEventListener('click', () => deleteReview(button.dataset.reviewDelete)));
}

function bindAiChat() {
    if (!$('aiChatToggle')) {
        return;
    }
    $('aiChatToggle').addEventListener('click', () => setAiChatOpen(!state.aiChat.open));
    $('aiChatClose').addEventListener('click', () => setAiChatOpen(false));
    $('aiChatForm').addEventListener('submit', submitAiChat);
    $('aiChatClear').addEventListener('click', clearAiChat);
    renderAiChatMessages();
}

function setAiChatOpen(open) {
    state.aiChat.open = open;
    $('aiChatPanel').hidden = !open;
    $('aiChatToggle').hidden = open;
    if (open) {
        renderAiChatMessages();
        window.setTimeout(() => $('aiChatInput').focus(), 50);
    }
}

function resetAiChat() {
    state.aiChat.conversationId = null;
    state.aiChat.messages = [];
    state.aiChat.limits = null;
    state.aiChat.loading = false;
    setAiChatStatus('Can dang nhap');
    renderAiChatLimits();
    renderAiChatMessages();
}

async function loadAiHistory() {
    if (!state.user) {
        resetAiChat();
        return;
    }
    try {
        const histories = await api('/api/ai/chat/history');
        const latest = histories && histories.length ? histories[0] : null;
        state.aiChat.conversationId = latest ? latest.conversationId : null;
        state.aiChat.messages = latest && latest.messages ? latest.messages : [];
        setAiChatStatus('San sang');
        renderAiChatMessages();
    } catch (error) {
        setAiChatStatus(error.message);
    }
}

async function loadAiLimits() {
    if (!state.user) {
        state.aiChat.limits = null;
        renderAiChatLimits();
        return;
    }
    try {
        state.aiChat.limits = await api('/api/ai/chat/limits');
        renderAiChatLimits();
    } catch (error) {
        state.aiChat.limits = null;
        renderAiChatLimits(error.message);
    }
}

function renderAiChatLimits(errorMessage = '') {
    const box = $('aiChatLimits');
    if (!box) {
        return;
    }
    if (!state.user) {
        box.innerHTML = '<div class="ai-limit-note">Dang nhap de xem gioi han Gemini.</div>';
        return;
    }
    const limits = state.aiChat.limits;
    if (!limits) {
        box.innerHTML = `<div class="ai-limit-note">${escapeHtml(errorMessage || 'Dang tai gioi han AI...')}</div>`;
        return;
    }

    const requestUsed = Number(limits.usedRequestsToday || 0);
    const requestLimit = Number(limits.appDailyRequestLimit || 0);
    const tokenUsed = Number(limits.usedTokensToday || 0);
    const tokenLimit = Number(limits.appDailyTokenLimit || 0);
    const requestPct = percentUsed(requestUsed, requestLimit);
    const tokenPct = percentUsed(tokenUsed, tokenLimit);
    const riskClass = limitRiskClass(Math.max(requestPct, tokenPct));
    const paidState = limits.allowPaidModels ? 'Paid ON' : 'Paid OFF';
    const guardState = limits.billingGuardEnabled ? 'Guard ON' : 'Guard OFF';
    const freeTier = limits.providerFreeRpd
        ? `${formatNumber(limits.providerFreeRpm)} RPM / ${formatNumber(limits.providerFreeTpm)} TPM / ${formatNumber(limits.providerFreeRpd)} RPD`
        : 'N/A';
    const searchLine = limits.googleSearchGroundingEnabled && limits.googleSearchFreeRpd
        ? `Search grounding: ${formatNumber(limits.googleSearchFreeRpd)} RPD free`
        : 'Search grounding: off';

    box.innerHTML = `
        <div class="ai-limit-top">
            <strong>${escapeHtml((limits.provider || 'AI').toUpperCase())} - ${escapeHtml(limits.model || '')}</strong>
            <span class="ai-limit-pill ${limits.billingGuardEnabled ? '' : 'danger'}">${escapeHtml(guardState)}</span>
        </div>
        <div class="ai-limit-row">
            <span>App cap hom nay</span>
            <strong>${formatNumber(requestUsed)} / ${requestLimit > 0 ? formatNumber(requestLimit) : 'khong gioi han'} req</strong>
        </div>
        <div class="ai-limit-bar" aria-hidden="true"><span class="ai-limit-fill ${riskClass}" style="width:${Math.min(requestPct, 100)}%"></span></div>
        <div class="ai-limit-row">
            <span>Token hom nay</span>
            <strong>${formatNumber(tokenUsed)} / ${tokenLimit > 0 ? formatNumber(tokenLimit) : 'khong gioi han'}</strong>
        </div>
        <div class="ai-limit-bar" aria-hidden="true"><span class="ai-limit-fill ${limitRiskClass(tokenPct)}" style="width:${Math.min(tokenPct, 100)}%"></span></div>
        <div class="ai-limit-row">
            <span>Gemini free tier</span>
            <span>${escapeHtml(freeTier)}</span>
        </div>
        <div class="ai-limit-row">
            <span>${escapeHtml(searchLine)}</span>
            <span class="ai-limit-pill ${limits.allowPaidModels ? 'warn' : ''}">${escapeHtml(paidState)}</span>
        </div>
    `;
}

function percentUsed(used, limit) {
    if (!limit || limit <= 0) {
        return 0;
    }
    return Math.max(0, Math.min(100, (used / limit) * 100));
}

function limitRiskClass(percent) {
    if (percent >= 90) {
        return 'danger';
    }
    if (percent >= 75) {
        return 'warn';
    }
    return '';
}

function aiLimitReached() {
    const limits = state.aiChat.limits;
    if (!limits || !limits.billingGuardEnabled) {
        return false;
    }
    const requestLimitHit = Number(limits.appDailyRequestLimit || 0) > 0
        && Number(limits.remainingRequestsToday || 0) <= 0;
    const tokenLimitHit = Number(limits.appDailyTokenLimit || 0) > 0
        && Number(limits.remainingTokensToday || 0) <= 0;
    if (!requestLimitHit && !tokenLimitHit) {
        return false;
    }
    setAiChatStatus('Het quota hom nay');
    showToast('Da cham gioi han AI hom nay. Tang limit trong application.properties neu muon dung tiep.', 'error');
    return true;
}

function renderAiChatMessages() {
    const box = $('aiChatMessages');
    if (!box) {
        return;
    }
    if (!state.aiChat.messages.length) {
        box.innerHTML = '<p class="state-line">Chua co tin nhan.</p>';
        return;
    }
    box.innerHTML = state.aiChat.messages.map(aiMessageHtml).join('');
    scrollAiChatToBottom();
}

function aiMessageHtml(message) {
    const sender = message.senderType === 'USER' ? 'user' : 'assistant';
    const errorClass = message.error ? ' error' : '';
    const content = message.typing ? typingHtml() : escapeHtml(message.messageText || '');
    const label = sender === 'user' ? 'Ban' : 'Greeny AI';
    return `
        <article class="ai-message ${sender}${errorClass}">
            <div class="ai-message-bubble">${content}</div>
            <span class="ai-message-meta">${label}${message.createdAt ? ' - ' + escapeHtml(formatDate(message.createdAt)) : ''}</span>
        </article>
    `;
}

function typingHtml() {
    return '<span class="ai-typing"><span></span><span></span><span></span></span>';
}

function setAiChatStatus(text) {
    const status = $('aiChatStatus');
    if (status) {
        status.textContent = text || '';
    }
}

function scrollAiChatToBottom() {
    const box = $('aiChatMessages');
    if (box) {
        box.scrollTop = box.scrollHeight;
    }
}

async function submitAiChat(event) {
    event.preventDefault();
    if (state.aiChat.loading) {
        return;
    }
    if (!state.user) {
        showToast('Can dang nhap de chat voi AI', 'error');
        setAiChatStatus('Can dang nhap');
        return;
    }
    if (aiLimitReached()) {
        return;
    }
    const input = $('aiChatInput');
    const message = input.value.trim();
    if (!message) {
        return;
    }

    const assistantId = localAiId('ai');
    state.aiChat.messages.push({
        messageId: localAiId('user'),
        conversationId: state.aiChat.conversationId,
        senderType: 'USER',
        messageText: message,
        createdAt: new Date().toISOString()
    });
    state.aiChat.messages.push({
        messageId: assistantId,
        conversationId: state.aiChat.conversationId,
        senderType: 'AI',
        messageText: '',
        typing: true,
        createdAt: new Date().toISOString()
    });
    input.value = '';
    state.aiChat.loading = true;
    $('aiChatSend').disabled = true;
    setAiChatStatus('Dang tra loi...');
    renderAiChatMessages();

    try {
        await streamAiChat(message, assistantId);
        setAiChatStatus('San sang');
    } catch (error) {
        updateAiMessage(assistantId, {
            messageText: error.message || 'AI dang tam thoi khong phan hoi.',
            typing: false,
            error: true
        });
        setAiChatStatus('Co loi');
        showToast(error.message || 'AI dang tam thoi khong phan hoi', 'error');
    } finally {
        state.aiChat.loading = false;
        $('aiChatSend').disabled = false;
        renderAiChatMessages();
    }
}

async function streamAiChat(message, assistantId) {
    const response = await fetch('/api/ai/chat/stream', {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Accept': 'text/event-stream',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            message,
            conversationId: state.aiChat.conversationId
        })
    });
    if (!response.ok) {
        throw new Error(await responseErrorMessage(response));
    }
    if (!response.body) {
        throw new Error('Trinh duyet khong ho tro doc stream');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
        const {value, done} = await reader.read();
        if (done) {
            break;
        }
        buffer += decoder.decode(value, {stream: true});
        buffer = consumeSseBuffer(buffer, (event) => handleAiStreamEvent(event, assistantId));
    }
    if (buffer.trim()) {
        consumeSseBuffer(buffer + '\n\n', (event) => handleAiStreamEvent(event, assistantId));
    }
}

function handleAiStreamEvent(event, assistantId) {
    if (!event || !event.type) {
        return;
    }
    if (event.type === 'status' && event.conversationId) {
        state.aiChat.conversationId = event.conversationId;
    }
    if (event.type === 'chunk') {
        state.aiChat.conversationId = event.conversationId || state.aiChat.conversationId;
        const current = state.aiChat.messages.find((message) => message.messageId === assistantId);
        updateAiMessage(assistantId, {
            messageText: (current && current.messageText ? current.messageText : '') + (event.content || ''),
            typing: false
        });
    }
    if (event.type === 'done' && event.response) {
        state.aiChat.conversationId = event.response.conversationId;
        if (event.response.usageLimits) {
            state.aiChat.limits = event.response.usageLimits;
            renderAiChatLimits();
        }
        updateAiMessage(assistantId, {
            ...event.response.aiMessage,
            typing: false
        });
    }
    if (event.type === 'error') {
        throw new Error(event.error || 'AI dang tam thoi khong phan hoi');
    }
    renderAiChatMessages();
}

function consumeSseBuffer(buffer, onEvent) {
    let remaining = buffer.replace(/\r\n/g, '\n');
    let boundary = remaining.indexOf('\n\n');
    while (boundary >= 0) {
        const raw = remaining.slice(0, boundary);
        remaining = remaining.slice(boundary + 2);
        const event = parseSseEvent(raw);
        if (event) {
            onEvent(event);
        }
        boundary = remaining.indexOf('\n\n');
    }
    return remaining;
}

function parseSseEvent(raw) {
    const data = raw.split(/\r?\n/)
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trimStart())
        .join('\n');
    if (!data) {
        return null;
    }
    return JSON.parse(data);
}

async function responseErrorMessage(response) {
    const text = await response.text();
    if (!text) {
        return 'Xu ly chat AI that bai';
    }
    try {
        const data = JSON.parse(text);
        return data.message || 'Xu ly chat AI that bai';
    } catch (error) {
        return text;
    }
}

function updateAiMessage(messageId, patch) {
    state.aiChat.messages = state.aiChat.messages.map((message) => (
        message.messageId === messageId ? {...message, ...patch} : message
    ));
}

async function clearAiChat() {
    if (state.aiChat.loading) {
        return;
    }
    try {
        if (state.user && state.aiChat.conversationId) {
            await api(`/api/ai/chat/history/${state.aiChat.conversationId}`, {method: 'DELETE'});
        }
        state.aiChat.conversationId = null;
        state.aiChat.messages = [];
        setAiChatStatus(state.user ? 'San sang' : 'Can dang nhap');
        renderAiChatMessages();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function localAiId(prefix) {
    return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function startReview(orderId, plantId) {
    const form = $('reviewForm');
    form.reviewId.value = '';
    form.orderId.value = orderId;
    form.plantId.value = plantId;
    form.rating.value = 5;
    form.title.value = '';
    form.comment.value = '';
    form.images.value = '';
    activateView('reviews');
}

function editReview(reviewId) {
    const review = state.reviews.find((item) => item.reviewId === reviewId);
    if (!review) {
        return;
    }
    const form = $('reviewForm');
    form.reviewId.value = review.reviewId;
    form.orderId.value = review.orderId || '';
    form.plantId.value = review.plantId || '';
    form.rating.value = review.rating || 5;
    form.title.value = review.title || '';
    form.comment.value = review.comment || '';
    form.images.value = review.images || '';
}

async function deleteReview(reviewId) {
    try {
        await api(`/api/user/reviews/${reviewId}`, {method: 'DELETE'});
        showToast('Da xoa danh gia');
        await loadReviews();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function bindStaticForms() {
    $('catalogFilters').addEventListener('submit', async (event) => {
        event.preventDefault();
        state.page = 0;
        await loadProducts();
    });
    $('prevPage').addEventListener('click', async () => {
        state.page = Math.max(state.page - 1, 0);
        await loadProducts();
    });
    $('nextPage').addEventListener('click', async () => {
        state.page += 1;
        await loadProducts();
    });
    $('loginForm').addEventListener('submit', async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        $('loginMessage').textContent = 'Dang dang nhap...';
        try {
            await api('/api/auth/login', {
                method: 'POST',
                body: {email: form.email.value, password: form.password.value}
            });
            $('loginMessage').textContent = '';
            showToast('Dang nhap thanh cong');
            await loadSession();
        } catch (error) {
            $('loginMessage').textContent = error.message;
        }
    });
    $('logoutButton').addEventListener('click', async () => {
        await api('/api/auth/logout', {method: 'POST'}).catch(() => null);
        state.user = null;
        state.cart = null;
        showToast('Da dang xuat');
        await loadSession();
        activateView('catalog');
    });
    $('clearCartButton').addEventListener('click', async () => {
        try {
            await api('/api/user/cart', {method: 'DELETE'});
            state.selectedCartItemIds = [];
            state.couponDiscount = 0;
            await loadCart();
            showToast('Da xoa gio hang');
        } catch (error) {
            showToast(error.message, 'error');
        }
    });
    $('refreshCheckoutButton').addEventListener('click', loadCheckoutData);
    $('applyCouponButton').addEventListener('click', applyCoupon);
    $('checkoutForm').addEventListener('submit', submitCheckout);
    $('mockPaySuccess').addEventListener('click', () => state.lastOrder && mockPayment(state.lastOrder.orderId, 'SUCCESS'));
    $('mockPayFail').addEventListener('click', () => state.lastOrder && mockPayment(state.lastOrder.orderId, 'FAILED'));
    $('profileForm').addEventListener('submit', submitProfile);
    $('passwordForm').addEventListener('submit', submitPassword);
    $('addressForm').addEventListener('submit', submitAddress);
    $('resetAddressForm').addEventListener('click', () => $('addressForm').reset());
    $('refreshOrdersButton').addEventListener('click', loadOrders);
    $('markAllReadButton').addEventListener('click', async () => {
        await api('/api/user/notifications/read-all', {method: 'PATCH'});
        await loadNotifications();
    });
    $('refreshReviewsButton').addEventListener('click', loadReviews);
    $('resetReviewForm').addEventListener('click', () => $('reviewForm').reset());
    $('reviewForm').addEventListener('submit', submitReview);
}

async function applyCoupon() {
    if (!state.cart) {
        state.cart = await api('/api/user/cart');
        reconcileSelectedCartItems();
    }
    const subtotal = selectedCartSubtotal();
    if (subtotal <= 0) {
        setStatus('checkoutStatus', 'Can chon it nhat mot san pham de ap ma.', 'error');
        return;
    }
    try {
        const response = await api('/api/user/coupons/validate', {
            method: 'POST',
            body: {couponCode: $('couponCode').value, subtotal}
        });
        state.couponDiscount = Number(response.discountAmount || 0);
        setStatus('checkoutStatus', `${response.message}: -${formatMoney(response.discountAmount)}`, 'success');
        renderCheckoutSummary();
    } catch (error) {
        state.couponDiscount = 0;
        setStatus('checkoutStatus', error.message, 'error');
        renderCheckoutSummary();
    }
}

async function submitCheckout(event) {
    event.preventDefault();
    const form = event.currentTarget;
    if (!selectedCartItems().length) {
        setStatus('checkoutStatus', 'Can chon it nhat mot san pham trong gio hang.', 'error');
        return;
    }
    const address = form.addressId.value ? null : {
        receiverName: form.receiverName.value,
        receiverPhone: form.receiverPhone.value,
        addressDetail: form.addressDetail.value,
        wardName: form.wardName.value,
        districtName: form.districtName.value,
        provinceName: form.provinceName.value,
        isDefault: true
    };
    try {
        state.lastOrder = await api('/api/user/orders/checkout', {
            method: 'POST',
            body: {
                cartItemIds: state.selectedCartItemIds,
                addressId: form.addressId.value || null,
                address,
                couponCode: form.couponCode.value || null,
                paymentMethod: form.paymentMethod.value,
                notes: form.notes.value || null
            }
        });
        showToast('Dat hang thanh cong');
        setStatus('checkoutStatus', `Da tao don ${shortId(state.lastOrder.orderId)}`, 'success');
        $('mockPaymentBox').hidden = !(state.lastOrder.payment && state.lastOrder.payment.method && state.lastOrder.payment.method.startsWith('ONLINE'));
        state.couponDiscount = 0;
        await Promise.allSettled([loadCart(), loadOrders(), loadNotifications()]);
    } catch (error) {
        setStatus('checkoutStatus', error.message, 'error');
        showToast(error.message, 'error');
    }
}

async function submitProfile(event) {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        const profile = await api('/api/user/profile', {
            method: 'PUT',
            body: {
                title: form.title.value,
                email: form.email.value,
                phone: form.phone.value,
                avatar: form.avatar.value
            }
        });
        state.user = profile;
        $('sessionLabel').textContent = profile.title || profile.email;
        showToast('Da luu thong tin');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function submitPassword(event) {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        await api('/api/user/password', {
            method: 'PUT',
            body: {currentPassword: form.currentPassword.value, newPassword: form.newPassword.value}
        });
        form.reset();
        showToast('Da doi mat khau');
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function submitAddress(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const body = {
        receiverName: form.receiverName.value,
        receiverPhone: form.receiverPhone.value,
        addressDetail: form.addressDetail.value,
        wardName: form.wardName.value,
        districtName: form.districtName.value,
        provinceName: form.provinceName.value,
        isDefault: form.isDefault.checked
    };
    try {
        if (form.addressId.value) {
            await api(`/api/user/addresses/${form.addressId.value}`, {method: 'PUT', body});
        } else {
            await api('/api/user/addresses', {method: 'POST', body});
        }
        form.reset();
        showToast('Da luu dia chi');
        await loadAddresses();
    } catch (error) {
        showToast(error.message, 'error');
    }
}

async function submitReview(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const body = {
        orderId: form.orderId.value,
        plantId: form.plantId.value,
        rating: Number(form.rating.value),
        title: form.title.value,
        comment: form.comment.value,
        images: form.images.value
    };
    try {
        if (form.reviewId.value) {
            await api(`/api/user/reviews/${form.reviewId.value}`, {
                method: 'PUT',
                body: {rating: body.rating, title: body.title, comment: body.comment, images: body.images}
            });
        } else {
            await api('/api/user/reviews', {method: 'POST', body});
        }
        form.reset();
        showToast('Da luu danh gia');
        await Promise.allSettled([loadReviews(), state.selectedPlant ? openProduct(state.selectedPlant.plantId) : Promise.resolve()]);
    } catch (error) {
        showToast(error.message, 'error');
    }
}

function shortId(value) {
    if (!value) {
        return '';
    }
    return String(value).slice(0, 8);
}

function formatDate(value) {
    if (!value) {
        return '';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleString('vi-VN');
}

document.addEventListener('DOMContentLoaded', init);
