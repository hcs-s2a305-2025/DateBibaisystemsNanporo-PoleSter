document.addEventListener('DOMContentLoaded', function() {
    const basePriceInput = document.getElementById('basePrice');
    const riceSelect = document.getElementById('riceSelect');
    const sourceSelect = document.getElementById('sourceSelect');
    const totalPriceDisplay = document.getElementById('totalPriceDisplay');
    const goodsIdInput = document.getElementById('goodsId');

    if (!basePriceInput || !totalPriceDisplay) return;

    const basePrice = parseInt(basePriceInput.value) || 0;

    function checkSideMenu() {
        const goodsIdVal = goodsIdInput ? String(goodsIdInput.value).trim() : '';
        // 商品IDの先頭が 'S' または 's' の場合
        const isSideMenu = goodsIdVal.toUpperCase().startsWith('S');

        if (riceSelect) {
            const riceContainer = riceSelect.closest('.add-selector') || riceSelect.parentElement;

            if (isSideMenu) {
                // --- サイドメニューの場合 ---
                // 既に "0" の option が無ければ動的に追加する
                let noneOption = riceSelect.querySelector('option[value="0"]');
                if (!noneOption) {
                    noneOption = new Option('なし', '0');
                    noneOption.dataset.price = "0";
                    riceSelect.add(noneOption, 0); // 先頭に追加
                }

                // 強制的に value を "0" (なし) にセット
                riceSelect.value = "0";

                // 「ご飯の量：」の選択エリアごと非表示にする
                if (riceContainer) {
                    riceContainer.style.setProperty('display', 'none', 'important');
                }
            } else {
                // --- 弁当（通常商品）の場合 ---
                // もし "0" (なし) の option が存在していたら削除する
                const noneOption = riceSelect.querySelector('option[value="0"]');
                if (noneOption) {
                    noneOption.remove();
                }

                // デフォルトを「普通(20)」にする
                if (!riceSelect.value || riceSelect.value === "0") {
                    riceSelect.value = "20";
                }

                // 表示する
                if (riceContainer) {
                    riceContainer.style.display = '';
                }
            }
        }

        if (sourceSelect) {
    const sourceContainer = sourceSelect.closest('.add-selector') || sourceSelect.parentElement;

    // 「なし」オプションが存在しない場合は作成して先頭に追加する関数
    let noneOption = sourceSelect.querySelector('option[value="0"]');
    if (!noneOption) {
        noneOption = new Option('なし', '0');
        noneOption.dataset.price = "0";
        sourceSelect.add(noneOption, 0); // 先頭に追加
    }

    if (isSideMenu && goodsIdVal !== "S001" && goodsIdVal !== "S002") {
        // --- サイドメニュー（S001/S002以外）の場合：ソース選択不可 ---
        
        // 値を「なし(0)」に強制変更
        sourceSelect.value = "0";

        // 非表示にして選択できないようにする
        if (sourceContainer) {
            sourceContainer.style.setProperty('display', 'none', 'important');
        }
    } else {
        // --- 弁当類、またはサイドメニューのS001 / S002の場合：ソース選択可能 ---

        // 初期選択値がない、または値が存在しない場合のデフォルト設定
        // （サーバー側で th:selected が指定されていない場合は先頭の「なし」が選ばれます）
        if (!sourceSelect.value) {
            sourceSelect.value = "0";
        }

        // 表示する
        if (sourceContainer) {
            sourceContainer.style.display = '';
        }
    }
}
    }

    function updatePrice() {
        let ricePrice = 0;
        let sourcePrice = 0;

        if (riceSelect && riceSelect.selectedIndex >= 0) {
            const selectedRiceOption = riceSelect.options[riceSelect.selectedIndex];
            ricePrice = parseInt(selectedRiceOption.dataset.price || 0);
        }

        if (sourceSelect && sourceSelect.selectedIndex >= 0) {
            const selectedSourceOption = sourceSelect.options[sourceSelect.selectedIndex];
            sourcePrice = parseInt(selectedSourceOption.dataset.price || 0);
        }

        const total = basePrice + ricePrice + sourcePrice;
        totalPriceDisplay.textContent = total.toLocaleString();
    }

    if (riceSelect) riceSelect.addEventListener('change', updatePrice);
    if (sourceSelect) sourceSelect.addEventListener('change', updatePrice);

    // 画面読み込み時に判定・計算を実行
    checkSideMenu();
    updatePrice();
});