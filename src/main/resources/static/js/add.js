document.addEventListener('DOMContentLoaded', function() {
    const basePriceInput = document.getElementById('basePrice');
    const riceSelect = document.getElementById('riceSelect');
    const sourceSelect = document.getElementById('sourceSelect');
    const zangiSelect = document.getElementById('zangiSelect');
    const setSelect = document.getElementById('setSelect');
    const totalPriceDisplay = document.getElementById('totalPriceDisplay');
    const goodsIdInput = document.getElementById('goodsId');
    // 商品名を表示している h1 要素を取得
    const goodsNameElement = document.querySelector('.add-select h1');

    if (!basePriceInput || !totalPriceDisplay) return;

    const basePrice = parseInt(basePriceInput.value) || 0;

    function checkMenuOptions() {
        const goodsIdVal = goodsIdInput ? String(goodsIdInput.value).trim().toUpperCase() : '';
        const goodsName = goodsNameElement ? goodsNameElement.textContent.trim() : '';

        // 1. お弁当類（B: お弁当, U: 裏商品）かどうかの判定
        const isBento = goodsIdVal.startsWith('B') || goodsIdVal.startsWith('U');
        
        // 2. 商品名に「ザンギ」が含まれるかどうかの判定
        const isZangi = goodsName.includes('ザンギ');

        // --- ご飯の量の表示制御 ---
        if (riceSelect) {
            const riceContainer = riceSelect.closest('.add-selector') || riceSelect.parentElement;

            if (isBento) {
                // お弁当・裏商品の場合：ご飯の量を表示
                const noneOption = riceSelect.querySelector('option[value="0"]');
                if (noneOption) {
                    noneOption.remove();
                }

                // デフォルト値を「普通 (20)」に設定
                if (!riceSelect.value || riceSelect.value === "0") {
                    riceSelect.value = "20";
                }

                if (riceContainer) {
                    riceContainer.style.display = '';
                }
            } else {
                // 単品（サイドメニュー等）の場合：ご飯の量を非表示＆値を0に固定
                let noneOption = riceSelect.querySelector('option[value="0"]');
                if (!noneOption) {
                    noneOption = new Option('なし', '0');
                    noneOption.dataset.price = "0";
                    riceSelect.add(noneOption, 0);
                }

                riceSelect.value = "0";

                if (riceContainer) {
                    riceContainer.style.setProperty('display', 'none', 'important');
                }
            }
        }

        // --- ソースの表示制御 ---
        if (sourceSelect) {
            const sourceContainer = sourceSelect.closest('.add-selector') || sourceSelect.parentElement;

            // 「なし (0)」オプションの存在チェック・追加
            let noneOption = sourceSelect.querySelector('option[value="0"]');
            if (!noneOption) {
                noneOption = new Option('なし', '0');
                noneOption.dataset.price = "0";
                sourceSelect.add(noneOption, 0);
            }

            if (isZangi) {
                // 「ザンギ」が含まれる商品の場合：ソース選択を表示
                if (!sourceSelect.value) {
                    sourceSelect.value = "0";
                }

                if (sourceContainer) {
                    sourceContainer.style.display = '';
                }
            } else {
                // 「ザンギ」が含まれない商品の場合：ソース選択を非表示＆値を「なし (0)」に設定
                sourceSelect.value = "0";

                if (sourceContainer) {
                    sourceContainer.style.setProperty('display', 'none', 'important');
                }
            }
        }
    }

    // 金額の計算・更新処理
    function updatePrice() {
        let ricePrice = 0;
        let sourcePrice = 0;
        let zangiPrice = 0;
        let setPrice = 0;

        if (riceSelect && riceSelect.selectedIndex >= 0) {
            const selectedRiceOption = riceSelect.options[riceSelect.selectedIndex];
            ricePrice = parseInt(selectedRiceOption.dataset.price || 0);
        }

        if (sourceSelect && sourceSelect.selectedIndex >= 0) {
            const selectedSourceOption = sourceSelect.options[sourceSelect.selectedIndex];
            sourcePrice = parseInt(selectedSourceOption.dataset.price || 0);
        }

        if (zangiSelect) {
            zangiPrice = parseInt(zangiSelect.value || 0, 10) * 100;
        }

        if (setSelect && setSelect.selectedIndex >= 0) {
            const selectedSetOption = setSelect.options[setSelect.selectedIndex];
            setPrice = parseInt(selectedSetOption.dataset.price || 0, 10);
        }

        const total = basePrice + ricePrice + sourcePrice + zangiPrice + setPrice;
        totalPriceDisplay.textContent = total.toLocaleString();
    }

    if (riceSelect) riceSelect.addEventListener('change', updatePrice);
    if (sourceSelect) sourceSelect.addEventListener('change', updatePrice);
    if (zangiSelect) zangiSelect.addEventListener('change', updatePrice);
    if (setSelect) setSelect.addEventListener('change', updatePrice);

    // 画面読み込み時に表示判定および金額計算を実行
    checkMenuOptions();
    updatePrice();
});