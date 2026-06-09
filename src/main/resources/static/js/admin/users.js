(function (window, document) {
    'use strict';

    const admin = window.GreenyAdmin = window.GreenyAdmin || {};
    let userState = null;

    function normalizeText(value) {
        return String(value || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim();
    }

    function getFormValue(form, name, defaultValue = '') {
        const field = form.elements[name];

        if (!field) {
            return defaultValue;
        }

        return field.value || defaultValue;
    }

    function dateMatches(cardDate, selectedDate) {
        if (!selectedDate) {
            return true;
        }

        return Boolean(cardDate) && cardDate === selectedDate;
    }

	function isAdminUser(card) {
	    return normalizeUserRole(card.dataset.role) === '0';
	}

    function isArchivedUser(card) {
        const status = String(card.dataset.status || 'none');

        return status === '2' || status === '-1';
    }

    function getRoleKey(card) {
        return isAdminUser(card) ? 'admin' : 'customer';
    }

    function getBucketKey(card) {
        return isArchivedUser(card) ? 'archived' : 'active';
    }

    function getUserSortValue(card) {
        return normalizeText(`${card.dataset.name || ''} ${card.dataset.email || ''}`);
    }

    function sortCards(cards) {
        return cards.sort((left, right) => {
            const leftArchived = isArchivedUser(left) ? 1 : 0;
            const rightArchived = isArchivedUser(right) ? 1 : 0;

            if (leftArchived !== rightArchived) {
                return leftArchived - rightArchived;
            }

            return getUserSortValue(left).localeCompare(getUserSortValue(right), 'vi');
        });
    }

    function createRoleGroup(roleKey, title, subtitle, iconClass) {
        const group = document.createElement('section');
        group.className = `user-role-group user-role-${roleKey}`;
        group.dataset.userRoleGroup = roleKey;

        group.innerHTML = `
            <header class="user-group-head">
                <div class="user-group-title">
                    <span class="user-group-icon">
                        <span class="mini-icon ${iconClass}" aria-hidden="true"></span>
                    </span>
                    <div>
                        <p class="eyebrow">${roleKey === 'admin' ? 'Quản trị' : 'Người dùng'}</p>
                        <h3>${title}</h3>
                        <p>${subtitle}</p>
                    </div>
                </div>

                <div class="user-group-counts">
                    <span><strong data-user-visible-count>0</strong> đang hiển thị</span>
                    <span><strong data-user-total-count>0</strong> tổng</span>
                </div>
            </header>

            <div class="user-bucket user-active-bucket">
                <div class="user-bucket-title">
                    <strong>Tài khoản đang quản lý</strong>
                    <span data-user-active-count>0 tài khoản</span>
                </div>
                <div class="user-card-bucket" data-user-bucket="${roleKey}-active"></div>
            </div>

            <div class="user-bucket user-archived-zone">
                <div class="user-bucket-title danger">
                    <strong>Đã khóa / đã xóa</strong>
                    <span data-user-archived-count>0 tài khoản</span>
                </div>
                <div class="user-card-bucket archived" data-user-bucket="${roleKey}-archived"></div>
            </div>
        `;

        return group;
    }

    function clearNode(node) {
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
    }

    function buildUserWorkspace() {
        const section = document.getElementById('users');

        if (!section) {
            return null;
        }

        const list = section.querySelector('.user-list');

        if (!list) {
            return null;
        }

        if (userState && userState.list === list) {
            return userState;
        }

        const cards = Array.from(list.querySelectorAll('.user-card'));

        if (cards.length === 0) {
            return null;
        }

        const preservedNodes = Array.from(list.children).filter((node) => {
            return !node.classList.contains('user-card')
                && !node.classList.contains('user-group-layout');
        });

        const layout = document.createElement('div');
        layout.className = 'user-group-layout';

        const adminGroup = createRoleGroup(
            'admin',
            'Tài khoản Admin',
            'Nhóm có quyền quản trị hệ thống. Nên kiểm soát kỹ, vì một admin nghịch dại đủ biến dashboard thành bãi chiến trường.',
            'icon-shield'
        );

        const customerGroup = createRoleGroup(
            'customer',
            'Tài khoản User',
            'Nhóm người dùng / khách hàng thông thường. Đây là phần cần quản lý nhiều nhất trong thực tế.',
            'icon-users'
        );

        layout.appendChild(adminGroup);
        layout.appendChild(customerGroup);

        clearNode(list);
        list.appendChild(layout);

        preservedNodes.forEach((node) => list.appendChild(node));

        userState = {
            section,
            list,
            layout,
            cards: sortCards(cards),
            groups: {
                admin: adminGroup,
                customer: customerGroup
            },
            buckets: {
                adminActive: layout.querySelector('[data-user-bucket="admin-active"]'),
                adminArchived: layout.querySelector('[data-user-bucket="admin-archived"]'),
                customerActive: layout.querySelector('[data-user-bucket="customer-active"]'),
                customerArchived: layout.querySelector('[data-user-bucket="customer-archived"]')
            }
        };

        return userState;
    }

    function getBucket(state, card) {
        const roleKey = getRoleKey(card);
        const bucketKey = getBucketKey(card);

        if (roleKey === 'admin' && bucketKey === 'active') {
            return state.buckets.adminActive;
        }

        if (roleKey === 'admin' && bucketKey === 'archived') {
            return state.buckets.adminArchived;
        }

        if (roleKey === 'customer' && bucketKey === 'active') {
            return state.buckets.customerActive;
        }

        return state.buckets.customerArchived;
    }

    function getUserFilters(form) {
        return {
            name: normalizeText(getFormValue(form, 'name')),
            email: normalizeText(getFormValue(form, 'email')),
            phone: normalizeText(getFormValue(form, 'phone')),
            role: getFormValue(form, 'role', 'all'),
            status: getFormValue(form, 'status', 'all'),
            created: getFormValue(form, 'created')
        };
    }

	function normalizeUserRole(role) {
	    const value = String(role || 'none');

	    /*
	     * Backend/template đang có một số tài khoản role null -> data-role="none".
	     * Trong admin thực tế, tài khoản không phải Admin thì gom về User.
	     */
	    return value === '0' ? '0' : '1';
	}

	function userCardMatches(card, filters) {
	    const cardName = normalizeText(card.dataset.name);
	    const cardEmail = normalizeText(card.dataset.email);
	    const cardPhone = normalizeText(card.dataset.phone);
	    const cardRole = normalizeUserRole(card.dataset.role);
	    const cardStatus = String(card.dataset.status || 'none');
	    const cardCreated = card.dataset.created || '';

	    return (!filters.name || cardName.includes(filters.name))
	        && (!filters.email || cardEmail.includes(filters.email))
	        && (!filters.phone || cardPhone.includes(filters.phone))
	        && (filters.role === 'all' || cardRole === filters.role)
	        && (filters.status === 'all' || cardStatus === filters.status)
	        && dateMatches(cardCreated, filters.created);
	}

    function updateGroupSummary(state, stats) {
        ['admin', 'customer'].forEach((roleKey) => {
            const group = state.groups[roleKey];

            if (!group) {
                return;
            }

            const visibleCount = group.querySelector('[data-user-visible-count]');
            const totalCount = group.querySelector('[data-user-total-count]');
            const activeCount = group.querySelector('[data-user-active-count]');
            const archivedCount = group.querySelector('[data-user-archived-count]');
            const activeBucket = group.querySelector('.user-active-bucket');
            const archivedBucket = group.querySelector('.user-archived-zone');

            if (visibleCount) {
                visibleCount.textContent = stats[roleKey].visible;
            }

            if (totalCount) {
                totalCount.textContent = stats[roleKey].total;
            }

            if (activeCount) {
                activeCount.textContent = `${stats[roleKey].active} tài khoản`;
            }

            if (archivedCount) {
                archivedCount.textContent = `${stats[roleKey].archived} tài khoản`;
            }

            if (activeBucket) {
                activeBucket.hidden = stats[roleKey].active === 0;
            }

            if (archivedBucket) {
                archivedBucket.hidden = stats[roleKey].archived === 0;
            }

            group.hidden = stats[roleKey].visible === 0;
        });
    }

    function renderUsers(filters) {
        const state = buildUserWorkspace();

        if (!state) {
            return 0;
        }

        Object.values(state.buckets).forEach(clearNode);

        const stats = {
            admin: {
                total: 0,
                visible: 0,
                active: 0,
                archived: 0
            },
            customer: {
                total: 0,
                visible: 0,
                active: 0,
                archived: 0
            }
        };

        let visibleTotal = 0;

        state.cards.forEach((card) => {
            const roleKey = getRoleKey(card);
            const archived = isArchivedUser(card);

            stats[roleKey].total += 1;

            const matched = userCardMatches(card, filters);

            card.hidden = !matched;
            card.classList.toggle('is-filter-hidden', !matched);

            if (!matched) {
                card.style.setProperty('display', 'none', 'important');
                return;
            }

            card.style.removeProperty('display');

            const bucket = getBucket(state, card);

            if (bucket) {
                bucket.appendChild(card);
            }

            visibleTotal += 1;
            stats[roleKey].visible += 1;

            if (archived) {
                stats[roleKey].archived += 1;
            } else {
                stats[roleKey].active += 1;
            }
        });

        updateGroupSummary(state, stats);

        return visibleTotal;
    }

    function applyUserFilters() {
        const section = document.getElementById('users');

        if (!section) {
            return;
        }

        const form = section.querySelector('[data-user-filter-form]');
        const emptyState = section.querySelector('.user-filter-empty');

        if (!form) {
            return;
        }

        const filters = getUserFilters(form);
        const visibleCount = renderUsers(filters);
        const totalCards = userState ? userState.cards.length : 0;

        if (emptyState) {
            emptyState.hidden = !(totalCards > 0 && visibleCount === 0);
        }
    }

    function resetUserFilters() {
        const section = document.getElementById('users');

        if (!section) {
            return;
        }

        const form = section.querySelector('[data-user-filter-form]');
        const emptyState = section.querySelector('.user-filter-empty');

        if (form) {
            form.reset();
        }

        renderUsers({
            name: '',
            email: '',
            phone: '',
            role: 'all',
            status: 'all',
            created: ''
        });

        if (emptyState) {
            emptyState.hidden = true;
        }
    }

    function bindUserFilterEvents() {
        const section = document.getElementById('users');

        if (!section) {
            return false;
        }

        const form = section.querySelector('[data-user-filter-form]');
        const applyButton = section.querySelector('[data-apply-user-filters]');
        const resetButton = section.querySelector('[data-reset-user-filters]');

        if (!form) {
            return false;
        }

        if (form.dataset.userFilterBound === '1') {
            return true;
        }

        form.dataset.userFilterBound = '1';

        if (applyButton) {
            applyButton.addEventListener('click', function (event) {
                event.preventDefault();
                applyUserFilters();
            });
        }

        if (resetButton) {
            resetButton.addEventListener('click', function (event) {
                event.preventDefault();
                resetUserFilters();
            });
        }

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            applyUserFilters();
        });

        form.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                applyUserFilters();
            }
        });

        form.addEventListener('change', function () {
            applyUserFilters();
        });

        return true;
    }

    function bindJumpToUserOrders() {
        document.querySelectorAll('[data-jump-user-orders]').forEach((button) => {
            if (button.dataset.jumpOrdersBound === '1') {
                return;
            }

            button.dataset.jumpOrdersBound = '1';

            button.addEventListener('click', function () {
                const email = button.dataset.jumpUserOrders || '';

                if (admin.activateSection) {
                    admin.activateSection('orders');
                }

                if (!admin.orders || !admin.orders.filterForm || !email) {
                    return;
                }

                admin.orders.filterForm.reset();

                const emailInput = admin.orders.filterForm.querySelector('[name="email"]');

                if (emailInput) {
                    emailInput.value = email;
                }

                if (typeof admin.orders.applyFieldFilters === 'function') {
                    admin.orders.applyFieldFilters();
                }
            });
        });
    }

    function initUsersAdmin() {
        buildUserWorkspace();
        bindUserFilterEvents();
        renderUsers({
            name: '',
            email: '',
            phone: '',
            role: 'all',
            status: 'all',
            created: ''
        });
        bindJumpToUserOrders();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUsersAdmin);
    } else {
        initUsersAdmin();
    }

    admin.applyUserFilters = applyUserFilters;
    admin.resetUserFilters = resetUserFilters;
    admin.initUsersAdmin = initUsersAdmin;

})(window, document);