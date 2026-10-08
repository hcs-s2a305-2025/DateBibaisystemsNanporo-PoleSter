// =========================================================
// PoleStar POS - 機能設定
// =========================================================
// このブロックだけを変更すれば、現場の運用に合わせて
// 上限・接続先・キーボード設定を変更できるようにしています。

const POS_CONFIG = {
    // ------------------------------
    // 各種数量上限
    // ------------------------------
    // 商品ごとの「カート内合計数量」の上限。デフォルトは9個。
    DEFAULT_PRODUCT_MAX: 9,
    PRODUCT_MAX: {
        // 例: P001: 5,
        // 個別設定を追加したい場合はここへ記述。
    },

    // トッピング1種類あたりの、1商品内での数量上限。
    // 「無限押下」を防ぐため、初期値は3個。
    DEFAULT_TOPPING_MAX: 3,
    TOPPING_MAX: {
        // ごはん量は排他グループなので1つだけ。
        T001: 1,
        T002: 1,
        T003: 1,
        T004: 1,
        // ソース系は3個まで。
        T005: 3,
        T006: 3,
        T007: 3,
        T008: 3,
        // 追加ザンギは複数可能とする例。
        T009: 15,
        // セット商品は1つだけ。
        T010: 1,
        T011: 1,
        T012: 1,
        T013: 1,
    },

    // ------------------------------
    // 排他設定
    // ------------------------------
    // 同じグループは同時に1種類だけ選択可能。
    TOPPING_EXCLUSIVE_GROUPS: {
        RICE_SIZE: ['T001', 'T002', 'T003', 'T004'],
        SET_GOODS: ['T010', 'T011', 'T012', 'T013'],
    },

    // ------------------------------
    // 電卓入力
    // ------------------------------
    MAX_CALCULATOR_DIGITS: 9,
    MAX_MOBILE_ORDER_DIGITS: 4,
    MOBILE_ORDER_PREFIX: 'M',

    // ------------------------------
    // 金額上限
    // ------------------------------
    MAX_MANUAL_AMOUNT: 999999999,
    MAX_DISCOUNT_AMOUNT: 999999999,
    MAX_RETURN_AMOUNT: 999999999,

    // ------------------------------
    // APIエンドポイント
    // ------------------------------
    // Java側のControllerへ接続する際に必要に応じて変更してください。
    PAYMENT_ENDPOINT: '/w/pos/payment',
    MOBILE_ORDER_ENDPOINT: '/w/pos/mobile-order',
    // QRは「IDを取得するだけ」の構造にしています。
    // ポイント付与そのものはJava側で実装してください。
    QR_POINT_ENDPOINT: '/w/pos/qr/point',

    // ------------------------------
    // QRコード
    // ------------------------------
    QR_CAMERA_FPS: 10,
    QR_BOX_SIZE: 250,

    // ------------------------------
    // キーコンフィグ
    // ------------------------------
    // 凡例:
    //   NUM_0～NUM_9   : 数字入力
    //   CLEAR          : C
    //   BACKSPACE      : ×
    //   TEN_THOUSAND   : 万券
    //   TOGGLE_ACTION  : アクション1⇔2
    //   MOBILE_ORDER   : M（モバイルオーダー入力開始）
    //   CONFIRM        : 決定 / 値引 / 返品確認 / 会計確定
    //   SUBTOTAL       : 小計
    //   PAYMENT        : 預/現計
    //
    // PCキーボードのキー名をそのまま指定できます。
    // 例: 'F1', 'Enter', 'Escape', 'Backspace', 'm', 'a' など。
    KEY_CONFIG: {
        NUM_0: '0',
        NUM_1: '1',
        NUM_2: '2',
        NUM_3: '3',
        NUM_4: '4',
        NUM_5: '5',
        NUM_6: '6',
        NUM_7: '7',
        NUM_8: '8',
        NUM_9: '9',
        CLEAR: 'Escape',
        BACKSPACE: 'Backspace',
        TEN_THOUSAND: 'F1',
        TOGGLE_ACTION: 'F2',
        MOBILE_ORDER: 'm',
        CONFIRM: 'Enter',
        SUBTOTAL: 'F3',
        PAYMENT: 'F4',
    },
};

// =========================================================
// 状態
// =========================================================

let calculatorValue = '';
let calculatorMode = 'NORMAL'; // NORMAL / MOBILE_ORDER
let mobileOrderNumber = '';

let subtotalAmount = 0;
let discountAmount = 0;
let returnAmount = 0;
let totalAmount = 0;
// 選択中の割引（クーポン）名を管理する配列（追加）
let appliedCoupons = [];

// 商品カート
let cartItems = [];

// 現在モーダルで編集中の商品
let currentProduct = null;

// QRスキャナ
let qrScanner = null;
let qrScanCompleted = false;
let qrMemberId = '';

// アクション1 / 2
let currentAction12 = 1;

// モバイルオーダー読み込み中フラグ（二重リクエスト防止）
let isLoadingMobileOrder = false;

// =========================================================
// 共通関数
// =========================================================

function getCsrfHeaders() {
    const headers = {
        'Content-Type': 'application/json'
    };

    const tokenMeta = document.querySelector('meta[name="_csrf"]');
    const headerMeta = document.querySelector('meta[name="_csrf_header"]');

    const token = tokenMeta ? tokenMeta.getAttribute('content') : '';
    const headerName = headerMeta ? headerMeta.getAttribute('content') : '';

    if (token && token !== 'null' && headerName && headerName !== 'null') {
        headers[headerName] = token;
    }

    return headers;
}

function formatYen(value) {
    return Number(value || 0).toLocaleString('ja-JP');
}

function clampInteger(value, min, max) {
    const number = Number(value);
    if (!Number.isFinite(number)) {
        return min;
    }
    return Math.min(max, Math.max(min, Math.trunc(number)));
}

function updateClock() {
    const now = new Date();
    const dateElem = document.getElementById('date');
    const clockElem = document.querySelector('.clock');

    if (!dateElem || !clockElem) {
        return;
    }

    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    dateElem.textContent = `${year}/${month}/${day}`;
    clockElem.textContent = `${hours}:${minutes}:${seconds}`;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// =========================================================
// 上限・排他設定
// =========================================================

function getProductLimit(productId) {
    return POS_CONFIG.PRODUCT_MAX[productId] ?? POS_CONFIG.DEFAULT_PRODUCT_MAX;
}

function getToppingLimit(toppingId) {
    return POS_CONFIG.TOPPING_MAX[toppingId] ?? POS_CONFIG.DEFAULT_TOPPING_MAX;
}

function getExclusiveGroup(toppingId) {
    const entry = Object.entries(POS_CONFIG.TOPPING_EXCLUSIVE_GROUPS)
        .find(([, ids]) => ids.includes(toppingId));
    return entry ? entry[0] : null;
}

function getExclusiveIds(groupName) {
    return POS_CONFIG.TOPPING_EXCLUSIVE_GROUPS[groupName] || [];
}

function getCartProductQuantity(productId) {
    return cartItems
        .filter(item => item.productId === productId)
        .reduce((sum, item) => sum + Number(item.quantity || 0), 0);
}

// =========================================================
// 商品・トッピング表示
// =========================================================

function updateProductBadges() {
    document.querySelectorAll('[data-product-count]').forEach(badge => {
        const id = badge.dataset.productCount;
        const count = getCartProductQuantity(id);
        const limit = getProductLimit(id);

        badge.textContent = count;
        badge.classList.toggle('d-none', count <= 0);

        const button = badge.closest('.product-button');
        if (button) {
            button.classList.toggle('product-selected', count > 0);
            button.classList.toggle('limit-reached', count >= limit);
            button.title = count >= limit
                ? `登録上限 ${limit}個に達しています`
                : `登録数 ${count}/${limit}`;
            button.disabled = count >= limit;
        }
    });
}

function resetToppingBadges() {
    document.querySelectorAll('[data-topping-count]').forEach(badge => {
        badge.textContent = '0';
        badge.classList.add('d-none');

        const button = badge.closest('.topping-button');
        if (button) {
            button.classList.remove('topping-selected', 'exclusive-selected', 'limit-reached');
        }
    });
}

function updateToppingBadges() {
    resetToppingBadges();

    if (!currentProduct || !currentProduct.toppings) {
        updateToppingControls();
        return;
    }

    Object.entries(currentProduct.toppings).forEach(([toppingId, toppingData]) => {
        const count = Number(toppingData.quantity || 0);
        const badge = document.querySelector(
            `[data-topping-count="${CSS.escape(toppingId)}"]`
        );

        if (badge) {
            badge.textContent = count;
            badge.classList.toggle('d-none', count <= 0);

            const button = badge.closest('.topping-button');
            if (button) {
                const limit = getToppingLimit(toppingId);
                const groupName = getExclusiveGroup(toppingId);
                button.classList.toggle('topping-selected', count > 0);
                button.classList.toggle('limit-reached', count >= limit);
                button.classList.toggle(
                    'exclusive-selected',
                    Boolean(groupName && count > 0)
                );
            }
        }
    });

    updateToppingControls();
}

function ensureToppingControls() {
    document.querySelectorAll('.topping-button').forEach(button => {
        if (button.querySelector('.topping-adjust-wrap')) {
            return;
        }

        const toppingId = button.dataset.toppingId || '';
        const controls = document.createElement('div');
        controls.className = 'topping-adjust-wrap';
        controls.innerHTML = `
            <span class="topping-adjust-btn" role="button" tabindex="0"
                    data-topping-action="decrease" data-topping-id="${escapeHtml(toppingId)}"
                    aria-label="${escapeHtml(button.dataset.toppingName || 'トッピング')}を減らす">−</span>
            <span class="topping-adjust-count" data-topping-adjust-count="${escapeHtml(toppingId)}">0</span>
            <span class="topping-adjust-btn" role="button" tabindex="0"
                    data-topping-action="increase" data-topping-id="${escapeHtml(toppingId)}"
                    aria-label="${escapeHtml(button.dataset.toppingName || 'トッピング')}を増やす">＋</span>
        `;
        button.appendChild(controls);
    });
}

function updateToppingControls() {
    document.querySelectorAll('[data-topping-adjust-count]').forEach(counter => {
        const id = counter.dataset.toppingAdjustCount;
        const count = Number(currentProduct?.toppings?.[id]?.quantity || 0);
        counter.textContent = count;

        const wrapper = counter.closest('.topping-adjust-wrap');
        if (wrapper) {
            const decrease = wrapper.querySelector('[data-topping-action="decrease"]');
            const increase = wrapper.querySelector('[data-topping-action="increase"]');
            const limit = getToppingLimit(id);

            if (decrease) {
                decrease.disabled = count <= 0;
            }
            if (increase) {
                increase.disabled = count >= limit;
            }
        }
    });
}

function getCurrentProductTotal() {
    if (!currentProduct) {
        return 0;
    }

    const toppingTotal = Object.values(currentProduct.toppings || {})
        .reduce((sum, topping) => {
            return sum + Number(topping.price || 0) * Number(topping.quantity || 0);
        }, 0);

    return Number(currentProduct.unitPrice || 0) + toppingTotal;
}

function updateModalProductDisplay() {
    applyToppingAvailability();
    const nameElem = document.getElementById('modal-product-name');
    const descriptionElem = document.getElementById('modal-product-description');
    const totalElem = document.getElementById('modal-item-total');
    const stopSaleBtn = document.getElementById('stop-sale-btn');
    const addProductBtn = document.getElementById('add-product-btn');

    if (!currentProduct) {
        if (nameElem) nameElem.textContent = '商品を選択してください';
        if (descriptionElem) descriptionElem.textContent = '商品内容';
        if (totalElem) totalElem.textContent = '0';
        updateToppingControls();
        return;
    }

    if (nameElem) {
        nameElem.textContent = currentProduct.name;
    }

    if (descriptionElem) {
        descriptionElem.textContent = `本体 ${formatYen(currentProduct.unitPrice)}円`;
    }

    if (totalElem) {
        totalElem.textContent = formatYen(getCurrentProductTotal());
    }
    // --- 販売停止 / 販売再開ボタンの見た目と挙動の切替 ---
    if (stopSaleBtn) {
        const btnText = stopSaleBtn.querySelector('h3') || stopSaleBtn;
        if (currentProduct.isSoldOut) {
            btnText.textContent = '販売再開';
            stopSaleBtn.classList.remove('modal-warning');
            stopSaleBtn.classList.add('modal-success'); // 販売再開用スタイルクラス（またはbtn-success等）
        } else {
            btnText.textContent = '販売停止';
            stopSaleBtn.classList.remove('modal-success');
            stopSaleBtn.classList.add('modal-warning');
        }
    }

    // 販売停止中の商品はカートに追加できないように制御
    if (addProductBtn) {
        addProductBtn.disabled = currentProduct.isSoldOut;
    }

    updateToppingControls();
}

function openProductModal(button) {
    const productId = button.dataset.productId || '';
    const productName = button.dataset.productName || '商品';
    const productPrice = Number(button.dataset.productPrice || 0);
    // 販売停止フラグを取得 (文字列 'true' かどうか)
    const isSoldOut = button.dataset.soldOut === 'true';
    const currentCount = getCartProductQuantity(productId);
    const limit = getProductLimit(productId);

    if (!isSoldOut && currentCount >= limit) {
        alert(`${productName}は登録上限 ${limit}個に達しています。`);
        return;
    }

    currentProduct = {
        productId,
        name: productName,
        unitPrice: productPrice,
        isSoldOut: isSoldOut,
        toppings: {}
    };

    resetToppingBadges();
    // -----------------------------------------------------
    // 特定商品の場合「ごはん普通」をデフォルト選択
    // -----------------------------------------------------
    const defaultRiceKeywords = ['弁当', '特上'];
    const hasDefaultRice = defaultRiceKeywords.some(keyword => productName.includes(keyword));

    if (hasDefaultRice) {
        // 'T001' が「ごはん普通」のIDと仮定しています
        setToppingQuantity('T002', 1);
    } else {
        updateModalProductDisplay();
    }
}

// 商品名から選択可能なトッピングIDを判定する。
// ザンギ弁当: すべて / 弁当・特上: ご飯＋定番コンビセット / 単品ザンギ: ソースのみ / その他: なし
function getAllowedToppingIds(productName) {
    const name = productName || '';
    const rice = ['T001', 'T002', 'T003', 'T004'];
    const sauce = ['T005', 'T006', 'T007', 'T008'];

    if (name.includes('ザンギ弁当')) {
        return new Set([...rice, ...sauce, 'T009', 'T010', 'T011', 'T012', 'T013']);
    }
    if (name.includes('弁当') || name.includes('特上')) {
        return new Set([...rice, 'T013']);
    }
    if (name.includes('単品ザンギ')) {
        return new Set(sauce);
    }
    return new Set();
}

// 選択できないトッピングは非表示にせず、レイアウトを保ったまま非活性（グレーアウト）にする。
function applyToppingAvailability() {
    const allowed = currentProduct ? getAllowedToppingIds(currentProduct.name) : null;

    document.querySelectorAll('.topping-button').forEach(button => {
        const isAllowed = !allowed || allowed.has(button.dataset.toppingId);
        button.disabled = !isAllowed;
        button.classList.toggle('topping-unavailable', !isAllowed);
    });
}

function setToppingQuantity(toppingId, delta) {
    if (!currentProduct) {
        alert('先に商品を選択してください。');
        return;
    }

    if (delta > 0 && !getAllowedToppingIds(currentProduct.name).has(toppingId)) {
        return;
    }

    const button = document.querySelector(
        `.topping-button[data-topping-id="${CSS.escape(toppingId)}"]`
    );
    if (!button) {
        return;
    }

    const toppingName = button.dataset.toppingName || '';
    const toppingPrice = Number(button.dataset.toppingPrice || 0);
    const limit = getToppingLimit(toppingId);
    const currentQuantity = Number(currentProduct.toppings?.[toppingId]?.quantity || 0);
    let nextQuantity = currentQuantity + delta;

    nextQuantity = clampInteger(nextQuantity, 0, limit);

    if (delta > 0 && currentQuantity >= limit) {
        alert(`${toppingName}は1商品につき${limit}個までです。`);
        return;
    }

    // 排他グループに新しい項目を選択した場合、同じグループの他項目を解除。
    const groupName = getExclusiveGroup(toppingId);
    if (delta > 0 && groupName && currentQuantity === 0 && nextQuantity > 0) {
        getExclusiveIds(groupName).forEach(otherId => {
            if (otherId !== toppingId && currentProduct.toppings[otherId]) {
                currentProduct.toppings[otherId].quantity = 0;
            }
        });
    }

    // -----------------------------------------------------
    // ソースの数量に応じた「ソースだく」「だくだく」の名称設定
    // -----------------------------------------------------
    const isSauce = ['T005', 'T006', 'T007', 'T008'].includes(toppingId) || toppingName.includes('ソース');
    let displayName = toppingName;
    let sauceLevel = '';

    if (isSauce) {
        if (nextQuantity === 2) {
            displayName = `${toppingName}（ソースだく）`;
            sauceLevel = 'ソースだく';
        } else if (nextQuantity === 3) {
            displayName = `${toppingName}（だくだく）`;
            sauceLevel = 'だくだく';
        }
    }

    if (!currentProduct.toppings[toppingId]) {
        currentProduct.toppings[toppingId] = {
            id: toppingId,
            rawName: toppingName,
            name: displayName,
            price: toppingPrice,
            quantity: 0,
            sauceLevel: sauceLevel
        };
    }

    currentProduct.toppings[toppingId].quantity = nextQuantity;
    currentProduct.toppings[toppingId].name = displayName;
    currentProduct.toppings[toppingId].sauceLevel = sauceLevel;

    if (nextQuantity <= 0) {
        delete currentProduct.toppings[toppingId];
    }

    updateToppingBadges();
    updateModalProductDisplay();
}

function selectTopping(button) {
    const toppingId = button.dataset.toppingId || '';
    const currentQuantity = Number(currentProduct?.toppings?.[toppingId]?.quantity || 0);
    const groupName = getExclusiveGroup(toppingId);

    // 排他グループは「同じボタンをもう一度押す」と選択解除できる。
    if (groupName && currentQuantity > 0) {
        setToppingQuantity(toppingId, -currentQuantity);
        return;
    }

    setToppingQuantity(toppingId, 1);
}

function addCurrentProductToCart() {
    if (!currentProduct) {
        alert('商品が選択されていません。');
        return;
    }

    const currentCount = getCartProductQuantity(currentProduct.productId);
    const limit = getProductLimit(currentProduct.productId);

    if (currentCount >= limit) {
        alert(`${currentProduct.name}は登録上限 ${limit}個に達しています。`);
        return;
    }

    cartItems.push({
        productId: currentProduct.productId,
        name: currentProduct.name,
        unitPrice: currentProduct.unitPrice,
        quantity: 1,
        toppings: JSON.parse(JSON.stringify(currentProduct.toppings || {})),
        total: getCurrentProductTotal()
    });

    calculateSubtotal();
    updateProductBadges();
    updateCartDetailTable();

    const modalElem = document.getElementById('posModal1');
    if (modalElem && window.bootstrap) {
        bootstrap.Modal.getOrCreateInstance(modalElem).hide();
    }

    currentProduct = null;
    resetToppingBadges();
    updateModalProductDisplay();
}

// =========================================================
// カート・数量調整
// =========================================================

function updateCartItemQuantity(index, delta) {
    const item = cartItems[index];
    if (!item) {
        return;
    }

    const limit = getProductLimit(item.productId);
    const nextQuantity = clampInteger(item.quantity + delta, 0, limit);

    if (delta > 0 && item.quantity >= limit) {
        alert(`${item.name}は登録上限 ${limit}個に達しています。`);
        return;
    }

    if (nextQuantity <= 0) {
        cartItems.splice(index, 1);
    } else {
        item.quantity = nextQuantity;
    }

    calculateSubtotal();
    updateProductBadges();
    updateCartDetailTable();
}

function calculateSubtotal() {
    subtotalAmount = cartItems.reduce(
        (sum, item) => sum + Number(item.total || 0) * Number(item.quantity || 0),
        0
    );

    recalculateTotal();
}

function removeLastCartItem() {
    // 予約注文が読み込まれている場合は予約注文の取り消しを行う
    if (mobileOrderNumber) {
        cancelMobileOrder();
        return;
    }
    if (cartItems.length === 0) {
        alert('登録されている商品がありません。');
        return;
    }

    cartItems.pop();
    calculateSubtotal();
    updateProductBadges();
    updateCartDetailTable();
}

function clearAllCartItems() {
    if (cartItems.length === 0 && subtotalAmount === 0 && discountAmount === 0 && returnAmount === 0) {
        return;
    }

    if (!confirm('登録した商品・値引・返品調整をすべて取り消しますか？')) {
        return;
    }

    cartItems = [];
    subtotalAmount = 0;
    discountAmount = 0;
    returnAmount = 0;
    appliedCoupons = [];
    mobileOrderNumber = '';
    calculatorValue = '';
    calculatorMode = 'NORMAL';
    qrMemberId = '';

    document.querySelectorAll('.discount-button').forEach(button => {
        button.dataset.applied = 'false';
        button.setAttribute('aria-pressed', 'false');
        button.classList.remove('discount-selected', 'active');
    });

    updateQrMemberDisplay();
    updateMobileOrderDisplay();
    recalculateTotal();
    updateProductBadges();
    updateCartDetailTable();
    updateCalculatorDisplay();
}

function updateCartDetailTable() {
    const tbody = document.querySelector('#posModal3 tbody');
    const totalElems = document.querySelectorAll('#posModal3 .pay-amount h1');

    if (!tbody) {
        return;
    }

    tbody.innerHTML = '';

    if (cartItems.length === 0) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td colspan="7" class="cart-empty">商品が登録されていません</td>';
        tbody.appendChild(tr);
    } else {
        cartItems.forEach((item, index) => {
            const tr = document.createElement('tr');
            // トッピング・セット商品のテキスト生成
            const toppingSummary = Object.values(item.toppings || {})
                .filter(t => Number(t.quantity || 0) > 0)
                .map(t => `${escapeHtml(t.name)} ×${t.quantity}`)
                .join('<br>');

            const toppingText = toppingSummary
                ? `<br><small class="text-muted">${toppingSummary}</small>`
                : '';

            // 商品名の中にある改行コード（\n）を <br> に変換しつつエスケープ
            const safeName = escapeHtml(item.name).replace(/\n/g, '<br>');

            tr.innerHTML = `
                <td>${index + 1}</td>
                <td>${safeName}${toppingText}</td>
                <td>
                    <div class="cart-quantity-wrap">
                        <button type="button" class="cart-adjust-btn" data-cart-action="decrease" data-cart-index="${index}">−</button>
                        <span class="cart-quantity-value">${item.quantity}</span>
                        <button type="button" class="cart-adjust-btn" data-cart-action="increase" data-cart-index="${index}">＋</button>
                    </div>
                </td>
                <td>${formatYen(item.total)}</td>
                <td>0</td>
                <td>${formatYen(item.total * item.quantity)}</td>
                <td class="text-end">
                    <button type="button" class="btn-delete" data-cart-action="delete" data-cart-index="${index}">削除</button>
                </td>
            `;

            tbody.appendChild(tr);
        });
    }

    totalElems.forEach((elem, index) => {
        if (index === totalElems.length - 1 || totalElems.length === 1) {
            elem.textContent = formatYen(totalAmount);
        }
    });
}

function deleteCartItem(index) {
    if (!Number.isInteger(index) || !cartItems[index]) {
        return;
    }

    cartItems.splice(index, 1);
    calculateSubtotal();
    updateProductBadges();
    updateCartDetailTable();
}

// =========================================================
// 金額表示・割引・返品
// =========================================================

function updateQrMemberDisplay() {
    const elem = document.getElementById('qr-member-id');
    if (elem) {
        elem.textContent = qrMemberId || '未読み取り';
    }
}

function updateMobileOrderDisplay() {
    const main = document.getElementById('mobile-order-display');
    const action4 = document.getElementById('mobile-order-display-action4');

    const text = mobileOrderNumber
        ? `モバイルオーダー：${mobileOrderNumber}`
        : '通常会計';

    if (main) main.textContent = text;
    if (action4) action4.textContent = mobileOrderNumber ? text : '';
}

function updateReturnStatus() {
    const elem = document.getElementById('return-status');
    if (!elem) {
        return;
    }

    elem.textContent = returnAmount > 0
        ? `返品調整：-${formatYen(returnAmount)}円`
        : '';
}

function updatePaymentDisplay() {
    const totalDisplay = document.getElementById('total-display');
    const subtotalDisplay = document.getElementById('subtotal-display');
    const receivedDisplay = document.getElementById('received-display');

    if (totalDisplay) {
        totalDisplay.textContent = formatYen(totalAmount);
    }

    if (subtotalDisplay) {
        subtotalDisplay.textContent = formatYen(Math.max(0, subtotalAmount + discountAmount));
    }

    if (receivedDisplay) {
        if (calculatorMode === 'MOBILE_ORDER') {
            receivedDisplay.textContent = `${POS_CONFIG.MOBILE_ORDER_PREFIX}${calculatorValue}`;
        } else {
            receivedDisplay.textContent = formatYen(Number(calculatorValue || 0));
        }
    }

    updateReturnStatus();
}

// 販売停止 / 販売再開の切り替え
async function toggleProductSaleStatus() {
    if (!currentProduct) return;

    const nextStatus = !currentProduct.isSoldOut;
    const actionText = nextStatus ? '販売停止' : '販売再開';

    if (!confirm(`「${currentProduct.name}」を${actionText}に変更しますか？`)) {
        return;
    }

    try {
        const response = await fetch('/w/pos/goods/toggle-sold-out', {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify({
                goodsId: currentProduct.productId,
                soldOut: nextStatus
            })
        });

        const result = await response.json();

        if (!response.ok || (result && result.success === false)) {
            throw new Error(result?.message || '状態変更に失敗しました。');
        }

        // 1. カレント商品の状態更新
        currentProduct.isSoldOut = nextStatus;

        // 2. メイン画面側の商品ボタンの data-sold-out 属性と見た目を更新
        const mainProductBtn = document.querySelector(`.product-button[data-product-id="${CSS.escape(currentProduct.productId)}"]`);
        if (mainProductBtn) {
            mainProductBtn.dataset.soldOut = String(nextStatus);
            mainProductBtn.classList.toggle('sold-out', nextStatus);
        }

        // 3. モーダル内表示の更新
        updateModalProductDisplay();

        alert(`「${currentProduct.name}」を${actionText}に切り替えました。`);
    } catch (error) {
        console.error('販売状態更新エラー:', error);
        alert(error.message || '通信エラーが発生しました。');
    }
}

function recalculateTotal() {
    // 表示上0円未満にならないようにする。
    // 返品額・値引額そのものは別状態として保持し、Java側へ渡せるようにする。
    totalAmount = Math.max(0, subtotalAmount + discountAmount - returnAmount);
    updatePaymentDisplay();
    updateCartDetailTable();
}

// 割引ボタンの名称を取得するヘルパー関数
function getDiscountName(button) {
    // data-discount-name 属性があれば優先し、なければボタンの表示テキストを使用
    return button.dataset.discountName || button.textContent.trim();
}

// 適用中の割引名をカンマ区切り文字列にして返す関数
function getAppliedCouponString() {
    if (appliedCoupons.length > 0) {
        return appliedCoupons.join(',');
    }
    // ボタンの割引はなく、電卓での手入力値引（discountAmount < 0）のみ適用されている場合
    if (discountAmount < 0) {
        return '値引';
    }
    return null;
}

function toggleDiscount(button) {
    const discount = Math.abs(Number(button.dataset.discount || 0));
    const alreadyApplied = button.dataset.applied === 'true';
    const couponName = getDiscountName(button);

    if (!alreadyApplied && couponName === 'スタンプカード割引' &&     !qrMemberId && !mobileOrderNumber) {
            alert('店頭注文でスタンプカード割引を使うには、先に会員QRを読み取ってください。');
        return;
    }
    if (alreadyApplied) {
        discountAmount += discount;
        button.dataset.applied = 'false';
        button.setAttribute('aria-pressed', 'false');
        button.classList.remove('discount-selected', 'active');
        // 配列から解除された割引名を削除
        appliedCoupons = appliedCoupons.filter(name => name !== couponName);
    } else {
        discountAmount -= discount;
        button.dataset.applied = 'true';
        button.setAttribute('aria-pressed', 'true');
        button.classList.add('discount-selected', 'active');
        // 配列に割引名を追加（重複防止）
        if (!appliedCoupons.includes(couponName)) {
            appliedCoupons.push(couponName);
        }
    }

    recalculateTotal();
}

function applyManualDiscount() {
    if (calculatorMode === 'MOBILE_ORDER') {
        alert('モバイルオーダー番号入力中です。Cでクリアしてから値引額を入力してください。');
        return;
    }

    const amount = Number(calculatorValue || 0);

    if (!Number.isInteger(amount) || amount <= 0) {
        alert('値引ボタンを押す前に、電卓で1円以上の値引額を入力してください。');
        return;
    }

    if (amount > POS_CONFIG.MAX_DISCOUNT_AMOUNT) {
        alert('設定された値引上限を超えています。');
        return;
    }

    if (amount > totalAmount) {
        alert('値引額が現在の合計金額を超えています。');
        return;
    }

    discountAmount -= amount;
    calculatorValue = '';
    recalculateTotal();
    updateCalculatorDisplay();
    alert(`${formatYen(amount)}円を値引しました。`);
}

function applyReturnAmount() {
    if (calculatorMode === 'MOBILE_ORDER') {
        alert('モバイルオーダー番号入力中です。Cでクリアしてから返品金額を入力してください。');
        return;
    }

    const amount = Number(calculatorValue || 0);

    if (!Number.isInteger(amount) || amount <= 0) {
        alert('返品ボタンを押す前に、電卓で1円以上の返品金額を入力してください。');
        return;
    }

    if (amount > POS_CONFIG.MAX_RETURN_AMOUNT) {
        alert('設定された返品上限を超えています。');
        return;
    }

    if (amount > subtotalAmount + discountAmount - returnAmount) {
        alert('返品額が現在の会計金額を超えています。');
        return;
    }

    returnAmount += amount;
    calculatorValue = '';
    recalculateTotal();
    updateCalculatorDisplay();
    alert(`${formatYen(amount)}円の返品調整を登録しました。小計へ進んで返金処理を完了してください。`);
}

// =========================================================
// 電卓 / モバイルオーダー番号
// =========================================================

function updateCalculatorDisplay() {
    const inputDisplay = document.getElementById('calculator-input-display');
    const receivedDisplay = document.getElementById('received-display');

    let displayText = '0';
    if (calculatorMode === 'MOBILE_ORDER') {
        displayText = `${POS_CONFIG.MOBILE_ORDER_PREFIX}${calculatorValue}`;
    } else if (calculatorValue !== '') {
        displayText = formatYen(Number(calculatorValue));
    }

    if (inputDisplay) {
        inputDisplay.textContent = displayText;
    }

    if (receivedDisplay) {
        receivedDisplay.textContent = displayText;
    }
}

function inputCalculatorNumber(number) {
    if (calculatorMode === 'MOBILE_ORDER') {
        if (calculatorValue.length >= POS_CONFIG.MAX_MOBILE_ORDER_DIGITS) {
            return;
        }

        // M直後の0もそのまま保持して「M072」のように表示する。
        calculatorValue += String(number).replace(/^0{2}$/, '00');
        calculatorValue = calculatorValue.slice(0, POS_CONFIG.MAX_MOBILE_ORDER_DIGITS);
        updateCalculatorDisplay();

        // 規定桁数（4桁）に達したら自動で予約注文を検索・登録
        if (calculatorValue.length === POS_CONFIG.MAX_MOBILE_ORDER_DIGITS) {
            loadMobileOrder();
        }
        return;
    }

    if (number === '00' && calculatorValue === '') {
        calculatorValue = '0';
        updateCalculatorDisplay();
        return;
    }

    if (calculatorValue === '0' && number !== '00') {
        calculatorValue = number;
    } else {
        calculatorValue += number;
    }

    if (calculatorValue.length > POS_CONFIG.MAX_CALCULATOR_DIGITS) {
        calculatorValue = calculatorValue.substring(0, POS_CONFIG.MAX_CALCULATOR_DIGITS);
    }

    updateCalculatorDisplay();
}

function startMobileOrderInput() {
    if (calculatorMode === 'MOBILE_ORDER') {
        return;
    }

    calculatorMode = 'MOBILE_ORDER';
    // calculatorValue = calculatorValue.replace(/\D/g, '').slice(0, POS_CONFIG.MAX_MOBILE_ORDER_DIGITS);
    calculatorValue = '';    
    mobileOrderNumber = '';
    updateCalculatorDisplay();
}

function clearCalculator() {
    calculatorValue = '';
    calculatorMode = 'NORMAL';
    updateCalculatorDisplay();
}

function deleteLastCalculatorDigit() {
    if (calculatorValue.length === 0) {
        return;
    }

    calculatorValue = calculatorValue.slice(0, -1);
    updateCalculatorDisplay();
}

function setTenThousandYen() {
    calculatorMode = 'NORMAL';
    calculatorValue = '10000';
    updateCalculatorDisplay();
}

async function confirmManualAmount() {
    // M + 数字 + 決定 = モバイルオーダー取得
    if (calculatorMode === 'MOBILE_ORDER') {
        await loadMobileOrder();
        return;
    }

    const amount = Number(calculatorValue || 0);

    if (amount <= 0) {
        alert('金額を入力してください。');
        return;
    }

    if (amount > POS_CONFIG.MAX_MANUAL_AMOUNT) {
        alert('設定された手入力金額の上限を超えています。');
        return;
    }

    cartItems.push({
        productId: 'MANUAL',
        name: '手入力',
        unitPrice: amount,
        quantity: 1,
        toppings: {},
        total: amount
    });

    calculatorValue = '';
    calculateSubtotal();
    updateProductBadges();
    updateCartDetailTable();
    updateCalculatorDisplay();
}

// =========================================================
// アクション切替
// =========================================================

function setAction12(action) {
    currentAction12 = action === 2 ? 2 : 1;

    const group1 = document.querySelectorAll('.action-group-1');
    const group2 = document.querySelectorAll('.action-group-2');

    group1.forEach(element => {
        element.classList.toggle('d-none', currentAction12 !== 1);
    });

    group2.forEach(element => {
        element.classList.toggle('d-none', currentAction12 !== 2);
    });
}

function toggleAction12() {
    setAction12(currentAction12 === 1 ? 2 : 1);
}

function showAction3() {
    const group3 = document.querySelectorAll('.action-group-3');
    const group4 = document.querySelectorAll('.action-group-4');

    group3.forEach(element => element.classList.remove('d-none'));
    group4.forEach(element => element.classList.add('d-none'));

    updatePaymentDisplay();
    updateCalculatorDisplay();
}

function showAction4() {
    const group3 = document.querySelectorAll('.action-group-3');
    const group4 = document.querySelectorAll('.action-group-4');

    group3.forEach(element => element.classList.add('d-none'));
    group4.forEach(element => element.classList.remove('d-none'));

    calculatorValue = '';
    calculatorMode = 'NORMAL';
    updatePaymentDisplay();
    updateCalculatorDisplay();
}

// =========================================================
// モバイルオーダー
// =========================================================

// 画面上のボタン（.topping-button）からセット商品・トッピング情報を探す関数
function findToppingInfoFromDOM(id, fallbackName) {
    if (!id && !fallbackName) return null;

    // IDの表記表記表記ゆれに対応 (例: "10" や 10 が来たら "T010" に変換して探す)
    let searchIds = [String(id)];
    if (/^\d+$/.test(String(id))) {
        const padded = String(id).padStart(3, '0');
        searchIds.push(`T${padded}`); // 例: "10" -> "T010"
        searchIds.push(`T${id}`);     // 例: "10" -> "T10"
    } else if (String(id).startsWith('T')) {
        searchIds.push(String(id).substring(1)); // 例: "T010" -> "010"
    }

    // 1. 画面上の .topping-button から ID が一致するものを探す（手動追加と同じ取得先）
    for (const searchId of searchIds) {
        const elem = document.querySelector(
            `[data-topping-id="${CSS.escape(searchId)}"], ` +
            `[data-set-id="${CSS.escape(searchId)}"], ` +
            `[data-id="${CSS.escape(searchId)}"]`
        );
        if (elem) {
            return {
                id: searchId,
                name: elem.dataset.toppingName || elem.dataset.setName || elem.dataset.name || fallbackName,
                price: Number(elem.dataset.toppingPrice || elem.dataset.setPrice || elem.dataset.price || 0)
            };
        }
    }

    // 2. IDでで見つからなくても、名前でボタンを探す
    if (fallbackName) {
        const elemByName = document.querySelector(
            `[data-topping-name="${CSS.escape(fallbackName)}"], ` +
            `[data-set-name="${CSS.escape(fallbackName)}"], ` +
            `[data-name="${CSS.escape(fallbackName)}"]`
        );
        if (elemByName) {
            return {
                id: elemByName.dataset.toppingId || elemByName.dataset.setId || String(id),
                name: fallbackName,
                price: Number(elemByName.dataset.toppingPrice || elemByName.dataset.setPrice || 0)
            };
        }
    }

    return null;
}

function normalizeMobileOrderToppings(item) {
    const toppings = {};
    // toppings（ソース類）と setGoods/options（セット商品類）を結合して処理
    const rawList = [];

    // --- A. 配列形式の抽出 (toppings, setGoods, options, subItems 等) ---
    const possibleArrays = [
        item.toppings, item.toppingList,
        item.setGoods, item.setGoodsList, item.sets, item.setList,
        item.options, item.optionList, item.subItems
    ];

    possibleArrays.forEach(target => {
        if (!target) return;
        if (Array.isArray(target)) {
            rawList.push(...target);
        } else if (typeof target === 'object') {
            // 単一オブジェクトで届いた場合もリストに追加
            rawList.push(target);
        }
    });

    // --- B. item直下に単体プロパティとして入っているセット商品の抽出 ---
    const directSetGoodsName = item.setGoodsName || item.setName || item.setGoods || item.optionName;
    const directSetGoodsId = item.setGoodsId || item.setId || item.setCode;
    if (directSetGoodsName || directSetGoodsId) {
        rawList.push({
            id: directSetGoodsId,
            name: directSetGoodsName,
            price: item.setGoodsPrice || item.setPrice || 0,
            quantity: item.setGoodsQuantity || item.setQuantity || 1
        });
    }

    // --- C. 各項目の整形とDOMからの名前補完 ---
    rawList.forEach((t, idx) => {
        if (!t) return;

        // 文字列だけで入っている場合の対応
        if (typeof t === 'string') {
            t = { name: t };
        }

        const rawId = t.id || t.toppingId || t.setId || t.setGoodsId || t.code || `opt_${idx}`;
        const rawName = String(
            t.name || t.toppingName || t.setName || t.setGoodsName || t.goodsName || t.optionName || ''
        ).trim();

        // DOM（画面上の全ボタン）から名前と価格を補助取得
        const domInfo = findToppingInfoFromDOM(rawId, rawName);

        const finalName = domInfo?.name || rawName;
        if (!finalName) return; // 名前が取れなかったものはスキップ

        const finalId = domInfo?.id || String(rawId);
        const price = Number(t.price ?? t.setPrice ?? t.addPrice ?? domInfo?.price ?? 0);
        const quantity = Number(t.quantity ?? t.count ?? 1);

        if (toppings[finalId]) {
            toppings[finalId].quantity += quantity;
        } else {
            toppings[finalId] = {
                id: finalId,
                name: finalName,
                price: price,
                quantity: quantity
            };
        }
    });

    return toppings;
}

function normalizeMobileOrderItems(result) {
    // デバッグ用：Javaから届いた生のJSON構造をブラウザのコンソールに出力
    console.log('[POS Debug] 受信データ全展開:\n' + JSON.stringify(result, null, 2));
    const rawItems = Array.isArray(result)
        ? result
        : (result?.items || result?.orderItems || result?.details || []);

    return rawItems.map(item => {
        const unitPrice = Number(item.unitPrice ?? item.price ?? item.amount ?? 0);
        const quantity = Math.max(1, Number(item.quantity || 1));
        // toppings だけでなく setGoods / options / sets などの配列もまとめて抽出
        // const rawToppings = item.toppings || item.toppingList;
        // const rawSetGoods = item.setGoods || item.sets || item.options || item.setGoodsList;
        const toppings = normalizeMobileOrderToppings(item);
        // トッピング小計（1個当たり）
        const toppingTotal = Object.values(toppings)
            .reduce((sum, topping) => sum + Number(topping.price || 0) * Number(topping.quantity || 0), 0);

        // 1個当たりの合計金額
        const singleItemTotal = unitPrice;

        return {
            productId: String(item.productId ?? item.id ?? item.productCode ?? 'MOBILE_ITEM'),
            name: String(item.name ?? item.productName ?? 'モバイルオーダー商品'),
            unitPrice,
            quantity,
            toppings,
            total: singleItemTotal
        };
    });
}

async function loadMobileOrder() {
    // 処理中なら即時リターン
    if (isLoadingMobileOrder) return;

    const digits = String(calculatorValue || '').trim();

    if (!/^\d+$/.test(digits)) {
        alert('Mの後にモバイルオーダー番号を入力してください。');
        return;
    }

    if (digits.length === 0 || digits.length > POS_CONFIG.MAX_MOBILE_ORDER_DIGITS) {
        alert(`モバイルオーダー番号は${POS_CONFIG.MAX_MOBILE_ORDER_DIGITS}桁以内で入力してください。`);
        return;
    }

    const orderNo = `${POS_CONFIG.MOBILE_ORDER_PREFIX}${digits.padStart(POS_CONFIG.MAX_MOBILE_ORDER_DIGITS, '0')}`;

    isLoadingMobileOrder = true; // ロック開始
    try {
        const response = await fetch(POS_CONFIG.MOBILE_ORDER_ENDPOINT, {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify({
                orderNo,
                mobileOrderNo: orderNo
            })
        });

        const contentType = response.headers.get('content-type') || '';
        let result;

        if (contentType.includes('application/json')) {
            result = await response.json();
        } else {
            const text = await response.text();
            result = { success: response.ok, message: text };
        }

        if (!response.ok) {
            throw new Error(result?.message || `モバイルオーダー取得エラー: ${response.status}`);
        }

        if (result && result.success === false) {
            throw new Error(result.message || 'モバイルオーダーの取得に失敗しました。');
        }

        const items = normalizeMobileOrderItems(result);
        if (items.length === 0) {
            throw new Error('指定されたモバイルオーダーの商品内訳が取得できませんでした。');
        }

        cartItems = items;
        mobileOrderNumber = orderNo;
        calculatorValue = '';
        calculatorMode = 'NORMAL';

        // モバイルオーダー取得時は、前の値引・返品調整を混ぜない。
        subtotalAmount = 0;
        discountAmount = 0;
        returnAmount = 0;
        appliedCoupons = [];

        document.querySelectorAll('.discount-button').forEach(button => {
            button.dataset.applied = 'false';
            button.setAttribute('aria-pressed', 'false');
            button.classList.remove('discount-selected', 'active');
        });

        calculateSubtotal();
        updateProductBadges();
        updateCartDetailTable();
        updateMobileOrderDisplay();
        updateCalculatorDisplay();
        showAction4();

        alert(`${orderNo} の注文内容を読み込みました。小計画面へ移動します。`);
    } catch (error) {
        console.error('モバイルオーダー取得エラー:', error);
        alert(error.message || 'モバイルオーダーの取得に失敗しました。');
    } finally {
        isLoadingMobileOrder = false; // 成功・失敗にかかわらずロック解除
    }
}

// 予約注文を取り消す関数を追加
function cancelMobileOrder() {
    if (!mobileOrderNumber) {
        return false;
    }

    if (confirm(`予約注文（${mobileOrderNumber}）を取り消して通常会計に戻しますか？`)) {
        mobileOrderNumber = '';
        cartItems = [];
        subtotalAmount = 0;
        discountAmount = 0;
        returnAmount = 0;
        calculatorValue = '';
        calculatorMode = 'NORMAL';

        // 割引ボタン選択解除
        document.querySelectorAll('.discount-button').forEach(button => {
            button.dataset.applied = 'false';
            button.setAttribute('aria-pressed', 'false');
            button.classList.remove('discount-selected', 'active');
        });

        updateProductBadges();
        updateCartDetailTable();
        updateMobileOrderDisplay();
        recalculateTotal();
        updateCalculatorDisplay();
        showAction3(); // 通常会計画面へ戻す
        alert('予約注文を取り消しました。');
        return true;
    }
    return true; // キャンセル時も処理を中断させるためtrue
}

// removeLastCartItem() の先頭に予約注文の判定を追加
function removeLastCartItem() {
    // 予約注文が読み込まれている場合は予約注文の取り消しを行う
    if (mobileOrderNumber) {
        cancelMobileOrder();
        return;
    }

    if (cartItems.length === 0) {
        alert('登録されている商品がありません。');
        return;
    }

    cartItems.pop();
    calculateSubtotal();
    updateProductBadges();
    updateCartDetailTable();
}

// =========================================================
// 会計確定 / 通常会計
// =========================================================

async function postPaymentResult() {
    const received = Number(calculatorValue || 0);
    const total = Number(totalAmount || 0);

    if(cartItems.length === 0) {
        alert('商品が登録されていません。');
        return;
    }

    if (total > 0 && received < total) {
    alert('預かり金額が合計金額より少なくなっています。');
    return;
    }   

    const change = Math.max(0, received - total);

    const paymentData = {
        transactionType: 'SALE',
        qrId: qrMemberId || null,
        mobileOrderNo: mobileOrderNumber || null,
        subtotal: subtotalAmount,
        discount: discountAmount,
        useCoupon: getAppliedCouponString(),
        returnAmount,
        total,
        received,
        change,
        paymentMethod: 'CASH',
        items: cartItems.map(item => ({
            productId: item.productId,
            name: item.name,
            unitPrice: item.unitPrice,
            quantity: item.quantity,
            total: item.total,
            toppings: Object.entries(item.toppings || {}).map(([id, t]) => {
                const numericId = parseInt(String(id).replace(/\D/g, ''), 10) || null;
                return {
                    id: id,
                    name: t.name,
                    price: t.price,
                    quantity: t.quantity,
                    plusZangiCount: id === 'T009' ? Number(t.quantity || 0) : null,
                    customId: numericId,
                    setGoodsId: numericId,
                    sauceLevel: t.sauceLevel || null,
                    customValue: t.sauceLevel || null
                };
            })
        }))
    };

    const completeButton = document.getElementById('complete-payment-btn');
    if (completeButton) {
        completeButton.disabled = true;
    }

    try {
        const response = await fetch(POS_CONFIG.PAYMENT_ENDPOINT, {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify(paymentData)
        });

        const contentType = response.headers.get('content-type') || '';
        let result = null;

        if (contentType.includes('application/json')) {
            result = await response.json();
        } else {
            const text = await response.text();
            result = { success: response.ok, message: text };
        }

        if (!response.ok) {
            throw new Error(result?.message || `サーバーエラー: ${response.status}`);
        }

        if (result && result.success === false) {
            throw new Error(result.message || '会計処理に失敗しました。');
        }

        alert(
            '会計処理が完了しました。\n' +
            '合計：' + formatYen(total) + '円\n' +
            '預かり：' + formatYen(received) + '円\n' +
            'お釣り：' + formatYen(change) + '円'
        );

        resetTransactionState();
    } catch (error) {
        console.error('会計POSTエラー:', error);
        alert(error.message || 'サーバーとの通信に失敗しました。');
    } finally {
        if (completeButton) {
            completeButton.disabled = false;
        }
    }
}

// =========================================================
// 返品確定
// =========================================================

async function completeReturnPayment() {
    const refundCash = Number(calculatorValue || 0);

    if (returnAmount <= 0) {
        alert('返品処理が登録されていません。先に返品金額を入力して返品ボタンを押してください。');
        return;
    }

    if (refundCash !== returnAmount) {
        alert(`返金額と入力金額が一致していません。返金額：${formatYen(returnAmount)}円`);
        return;
    }

    const paymentData = {
        transactionType: 'RETURN',
        qrId: qrMemberId || null,
        mobileOrderNo: mobileOrderNumber || null,
        subtotal: subtotalAmount,
        discount: discountAmount,
        useCoupon: getAppliedCouponString(),
        returnAmount,
        total: totalAmount,
        received: refundCash,
        change: 0,
        refundAmount: refundCash,
        paymentMethod: 'CASH',
        items: cartItems.map(item => ({
            productId: item.productId,
            name: item.name,
            unitPrice: item.unitPrice,
            quantity: item.quantity,
            total: item.total,
            toppings: Object.values(item.toppings || {}).map(t => ({
                name: t.name,
                price: t.price,
                quantity: t.quantity
            }))
        }))
    };

    const completeButton = document.getElementById('complete-payment-btn');
    if (completeButton) {
        completeButton.disabled = true;
    }

    try {
        const response = await fetch(POS_CONFIG.PAYMENT_ENDPOINT, {
            method: 'POST',
            headers: getCsrfHeaders(),
            body: JSON.stringify(paymentData)
        });

        const contentType = response.headers.get('content-type') || '';
        let result = null;

        if (contentType.includes('application/json')) {
            result = await response.json();
        } else {
            result = { success: response.ok, message: await response.text() };
        }

        if (!response.ok) {
            throw new Error(result?.message || `返金サーバーエラー: ${response.status}`);
        }

        if (result && result.success === false) {
            throw new Error(result.message || '返金処理に失敗しました。');
        }

        alert(`${formatYen(refundCash)}円の返金処理が完了しました。`);
        resetTransactionState();
    } catch (error) {
        console.error('返金POSTエラー:', error);
        alert(error.message || '返金処理に失敗しました。');
    } finally {
        if (completeButton) {
            completeButton.disabled = false;
        }
    }
}

function resetTransactionState() {
    showAction3();

    cartItems = [];
    subtotalAmount = 0;
    discountAmount = 0;
    returnAmount = 0;
    appliedCoupons = [];
    calculatorValue = '';
    calculatorMode = 'NORMAL';
    mobileOrderNumber = '';
    qrMemberId = '';

    document.querySelectorAll('.discount-button').forEach(button => {
        button.dataset.applied = 'false';
        button.setAttribute('aria-pressed', 'false');
        button.classList.remove('discount-selected', 'active');
    });

    updateProductBadges();
    updateCartDetailTable();
    updateMobileOrderDisplay();
    updateQrMemberDisplay();
    recalculateTotal();
    updateCalculatorDisplay();
}

// =========================================================
// QRコード
// =========================================================

function setQrStatus(message) {
    const status = document.getElementById('qr-status');
    if (status) {
        status.textContent = message;
    }
}

async function stopQrScanner() {
    if (!qrScanner) {
        return;
    }

    // try {
    //     await qrScanner.stop();
    // } catch (error) {
    //     console.warn('QRスキャナ停止エラー:', error);
    // }

    try {
        await qrScanner.clear();
    } catch (error) {
        console.warn('QRスキャナ画面クリアエラー:', error);
    }

    qrScanner = null;
}

async function handleQrScan(decodedText) {
    if (qrScanCompleted) {
        return;
    }

    let id = String(decodedText || '').trim();
    if (!id) {
        return;
    }
    // JSON形式（{"mail":"..."}など）の場合はJavaScript側でメールアドレスのみを抽出
    if (id.startsWith('{') && id.includes('mail')) {
        try {
            const parsed = JSON.parse(id);
            if (parsed && parsed.mail) {
                id = parsed.mail;
            }
        } catch (e) {
            // JSONパース失敗時は正規表現で抽出
            const match = id.match(/"mail"\s*:\s*"([^"]+)"/);
            if (match && match[1]) {
                id = match[1];
            }
        }
    }

    qrScanCompleted = true;
    qrMemberId = id;

    updateQrMemberDisplay();
    setQrStatus(`ID「${id}」を取得しました。`);
    await stopQrScanner();

    alert(`QR ID「${id}」を取得しました。\n会計時にJava側へ送信します。`);

    const modalElem = document.getElementById('qrModal');
    if (modalElem && window.bootstrap) {
        bootstrap.Modal.getOrCreateInstance(modalElem).hide();
    }
}

async function startQrScanner() {
    const reader = document.getElementById('qrReader');

    if (!reader) {
        return;
    }

    await stopQrScanner();
    reader.innerHTML = '';
    qrScanCompleted = false;

    if (typeof Html5QrcodeScanner === 'undefined') {
        setQrStatus('QR読み取りライブラリを読み込めませんでした。');
        return;
    }

    setQrStatus('カメラを起動しています…');

    qrScanner = new Html5QrcodeScanner(
        'qrReader',
        {
            fps: POS_CONFIG.QR_CAMERA_FPS,
            qrbox: {
                width: POS_CONFIG.QR_BOX_SIZE,
                height: POS_CONFIG.QR_BOX_SIZE
            },
            aspectRatio: 1.0,
            rememberLastUsedCamera: true,
            experimentalFeatures: {
                useBarCodeDetectorIfSupported: true
            },
            showTorchButtonIfSupported: true
        },
        false
    );

    qrScanner.render(
        decodedText => handleQrScan(decodedText),
        errorMessage => {
            // フレームごとの読み取り失敗は通常状態なので画面には表示しない。
            console.debug('QR scan:', errorMessage);
        }
    );
}

// =========================================================
// キーボード
// =========================================================

function getConfiguredKey(name) {
    return String(POS_CONFIG.KEY_CONFIG[name] ?? '').toLowerCase();
}

function isConfiguredKey(event, name) {
    return event.key.toLowerCase() === getConfiguredKey(name);
}

function handleConfiguredKeyboard(event) {
    // モーダル等で別の入力欄を増やした場合に邪魔しないための保険。
    const target = event.target;
    if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) {
        return;
    }

    for (let i = 0; i <= 9; i += 1) {
        if (isConfiguredKey(event, `NUM_${i}`)) {
            event.preventDefault();
            inputCalculatorNumber(String(i));
            return;
        }
    }

    if (event.key === '+') {
        // テンキーの「+」は数量調整用として将来利用可能。
        return;
    }

    if (isConfiguredKey(event, 'CLEAR')) {
        event.preventDefault();
        clearCalculator();
        return;
    }

    if (isConfiguredKey(event, 'BACKSPACE')) {
        event.preventDefault();
        deleteLastCalculatorDigit();
        return;
    }

    if (isConfiguredKey(event, 'TEN_THOUSAND')) {
        event.preventDefault();
        document.getElementById('ten-thousand-btn')?.click();
        return;
    }

    if (isConfiguredKey(event, 'TOGGLE_ACTION')) {
        event.preventDefault();
        document.getElementById('toggle-action-btn')?.click();
        return;
    }

    if (isConfiguredKey(event, 'MOBILE_ORDER')) {
        event.preventDefault();
        document.getElementById('calculator-memory')?.click();
        return;
    }

    if (isConfiguredKey(event, 'SUBTOTAL')) {
        event.preventDefault();
        document.getElementById('toggle-action-pay')?.click();
        return;
    }

    if (isConfiguredKey(event, 'PAYMENT')) {
        event.preventDefault();
        document.getElementById('complete-payment-btn')?.click();
        return;
    }

    if (isConfiguredKey(event, 'CONFIRM')) {
        event.preventDefault();

        if (currentAction12 === 1) {
            document.getElementById('confirm-manual-btn')?.click();
            return;
        }

        if (currentAction12 === 2) {
            // 値引/返品はマウス・タッチボタンを主操作とし、Enterは返品を優先しない。
            return;
        }
    }
}

// =========================================================
// DOM読み込み完了
// =========================================================

document.addEventListener('DOMContentLoaded', function () {
    // 時計
    updateClock();
    setInterval(updateClock, 1000);

    // 初期UI
    ensureToppingControls();
    setAction12(1);
    calculateSubtotal();
    updateProductBadges();
    updateToppingBadges();
    updateCartDetailTable();
    updatePaymentDisplay();
    updateCalculatorDisplay();
    updateQrMemberDisplay();
    updateMobileOrderDisplay();

    // -----------------------------------------------------
    // 商品ボタン
    // -----------------------------------------------------
    document.querySelectorAll('.product-button').forEach(button => {
        button.addEventListener('click', function () {
            openProductModal(this);
        });
    });

    // -----------------------------------------------------
    // トッピングボタン
    // -----------------------------------------------------
    document.querySelectorAll('.topping-button').forEach(button => {
        button.addEventListener('click', function (event) {
            // ＋/−調整ボタンが押された場合は親ボタンのクリック処理（＋1）を行わない
            if (event.target.closest('[data-topping-action]')) {
                return;
            }
            selectTopping(this);
        });
    });

    // -----------------------------------------------------
    // トッピング +/-
    // -----------------------------------------------------
    document.addEventListener('click', function (event) {
        const adjustButton = event.target.closest('[data-topping-action]');
        if (!adjustButton) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const toppingId = adjustButton.dataset.toppingId;
        const action = adjustButton.dataset.toppingAction;
        setToppingQuantity(toppingId, action === 'increase' ? 1 : -1);
    });

    document.addEventListener('keydown', function (event) {
        const adjustButton = event.target.closest?.('[data-topping-action]');
        if (!adjustButton || !['Enter', ' '].includes(event.key)) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        const toppingId = adjustButton.dataset.toppingId;
        const action = adjustButton.dataset.toppingAction;
        setToppingQuantity(toppingId, action === 'increase' ? 1 : -1);
    });

    // -----------------------------------------------------
    // 商品追加
    // -----------------------------------------------------
    document.getElementById('add-product-btn')
        ?.addEventListener('click', addCurrentProductToCart);

    // -----------------------------------------------------
    // 商品モーダル初期化
    // -----------------------------------------------------
    document.getElementById('posModal1')
        ?.addEventListener('hidden.bs.modal', function () {
            currentProduct = null;
            resetToppingBadges();
            updateModalProductDisplay();
        });

    // -----------------------------------------------------
    // 販売停止
    // -----------------------------------------------------
    document.getElementById('stop-sale-btn')
        ?.addEventListener('click', toggleProductSaleStatus);

    // -----------------------------------------------------
    // 商品詳細の + / - / 削除
    // -----------------------------------------------------
    document.querySelector('#posModal3')
        ?.addEventListener('click', function (event) {
            const button = event.target.closest('[data-cart-action][data-cart-index]');
            if (!button) {
                return;
            }

            const index = Number(button.dataset.cartIndex);
            const action = button.dataset.cartAction;

            if (action === 'increase') {
                updateCartItemQuantity(index, 1);
            } else if (action === 'decrease') {
                updateCartItemQuantity(index, -1);
            } else if (action === 'delete') {
                deleteCartItem(index);
            }
        });

    // -----------------------------------------------------
    // アクション1 ⇔ アクション2
    // -----------------------------------------------------
    document.getElementById('toggle-action-btn')
        ?.addEventListener('click', toggleAction12);

    // 取消（最後の商品を削除）
    document.getElementById('cancel-last-item-btn')
        ?.addEventListener('click', removeLastCartItem);

    // 万券
    document.getElementById('ten-thousand-btn')
        ?.addEventListener('click', setTenThousandYen);

    // M → モバイルオーダー入力開始
    document.getElementById('calculator-memory')
        ?.addEventListener('click', startMobileOrderInput);

    // -----------------------------------------------------
    // アクション2
    // -----------------------------------------------------
    document.getElementById('return-last-item-btn')
        ?.addEventListener('click', applyReturnAmount);

    document.getElementById('manual-discount-btn')
        ?.addEventListener('click', applyManualDiscount);

    document.getElementById('cancel-all-btn')
        ?.addEventListener('click', clearAllCartItems);

    document.getElementById('exchange-btn')
        ?.addEventListener('click', function () {
            const amount = Number(prompt('両替する金額を入力してください（円）', '1000') || 0);
            if (amount > 0) {
                alert(`${formatYen(amount)}円の両替処理を開始します。`);
            }
        });

    document.getElementById('receipt-btn')
        ?.addEventListener('click', function () {
            if (cartItems.length === 0) {
                alert('領収書を発行する商品がありません。');
                return;
            }
            window.print();
        });

    // 決定（手入力 / Mオーダー取得）
    document.getElementById('confirm-manual-btn')
        ?.addEventListener('click', confirmManualAmount);

    // -----------------------------------------------------
    // アクション3 → アクション4
    // -----------------------------------------------------
    document.getElementById('toggle-action-pay')
        ?.addEventListener('click', showAction4);

    // -----------------------------------------------------
    // アクション4 → アクション3
    // -----------------------------------------------------
    document.getElementById('back-to-subtotal')
        ?.addEventListener('click', showAction3);

    // -----------------------------------------------------
    // 数字ボタン（0 / 00 / 1～9）
    // -----------------------------------------------------
    document.querySelectorAll('[data-calc-num]').forEach(button => {
        button.addEventListener('click', function () {
            const numValue = this.dataset.calcNum;
            if (numValue) {
                inputCalculatorNumber(numValue);
            }
        });
    });

    // -----------------------------------------------------
    // C
    // -----------------------------------------------------
    document.getElementById('calculator-clear')
        ?.addEventListener('click', clearCalculator);

    // -----------------------------------------------------
    // ×
    // -----------------------------------------------------
    document.getElementById('calculator-backspace')
        ?.addEventListener('click', deleteLastCalculatorDigit);

    // -----------------------------------------------------
    // 割引ボタン
    // -----------------------------------------------------
    document.querySelectorAll('.discount-button').forEach(button => {
        button.dataset.applied = 'false';
        button.setAttribute('aria-pressed', 'false');
        button.addEventListener('click', function () {
        toggleDiscount(this);
        });
    });

    // -----------------------------------------------------
    // 預/現計 / 返金完了
    // -----------------------------------------------------
    document.getElementById('complete-payment-btn')
        ?.addEventListener('click', function () {
            if (returnAmount > 0) {
                completeReturnPayment();
            } else {
                postPaymentResult();
            }
        });

    // -----------------------------------------------------
    // QR
    // -----------------------------------------------------
    const qrModal = document.getElementById('qrModal');

    qrModal?.addEventListener('shown.bs.modal', function () {
        startQrScanner();
    });

    qrModal?.addEventListener('hidden.bs.modal', function () {
        stopQrScanner();
        qrScanCompleted = false;
        setQrStatus('カメラを起動しています…');
    });

    // -----------------------------------------------------
    // キーボード
    // -----------------------------------------------------
    document.addEventListener('keydown', handleConfiguredKeyboard);
});