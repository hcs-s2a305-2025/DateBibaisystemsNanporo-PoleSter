document.addEventListener("DOMContentLoaded", function () {
    // -------------------------------------------------------------
    // 1. 受取日・受取時間のドロップダウン動的生成
    // -------------------------------------------------------------
    const pickupDateSelect = document.getElementById("pickupDate");
    const pickupTimeSelect = document.getElementById("pickupTime");

    if (pickupDateSelect && pickupTimeSelect) {
        // 設定値
        const START_HOUR = 11;    // 受取開始時間 (11:00)
        const END_HOUR = 15;      // 受取終了時間 (15:00)
        const INTERVAL_MIN = 10;  // 10分刻み
        const LEAD_TIME_MIN = 30; // 30分前予約制限
        const CLOSED_DAY = 0;     // 定休日: 0 = 日曜日

        const dayOfWeekNames = ["日", "月", "火", "水", "木", "金", "土"];
        const now = new Date();

        // YYYY-MM-DD 形式（フォーム送信値用）
        const formatDateValue = (d) => {
            const yyyy = d.getFullYear();
            const mm = String(d.getMonth() + 1).padStart(2, '0');
            const dd = String(d.getDate()).padStart(2, '0');
            return `${yyyy}-${mm}-${dd}`;
        };

        // 表示用ラベル（例: 9/30(水) 今日）
        const formatDateLabel = (d, dayLabel) => {
            const mm = d.getMonth() + 1;
            const dd = d.getDate();
            const dayOfWeek = dayOfWeekNames[d.getDay()];
            return `${mm}/${dd}(${dayOfWeek})${dayLabel ? ' ' + dayLabel : ''}`;
        };

        // --- 受取日（定休日を除く営業日3日間）の生成 ---
        pickupDateSelect.innerHTML = "";

        let addedCount = 0;
        let dayOffset = 0;

        // --- 受取日（定休日を除く、今日・明日・明後日のみ）の生成 ---

        // 今日(0), 明日(1), 明後日(2) の最大3日間のみチェック
        for (let dayOffset = 0; dayOffset < 3; dayOffset++) {
            const targetDate = new Date(now.getTime());
            targetDate.setDate(now.getDate() + dayOffset);

            // 日曜日（定休日）の場合は選択肢自体に追加しない
            if (targetDate.getDay() !== CLOSED_DAY) {
                let dayLabel = "";
                if (dayOffset === 0) dayLabel = "今日";
                else if (dayOffset === 1) dayLabel = "明日";
                else if (dayOffset === 2) dayLabel = "明後日";

                const option = document.createElement("option");
                option.value = formatDateValue(targetDate);
                option.textContent = formatDateLabel(targetDate, dayLabel);
                pickupDateSelect.appendChild(option);
            }
        }

        // --- 受取時間の更新関数 ---
        function updateTimeOptions() {
            const selectedDateStr = pickupDateSelect.value;
            pickupTimeSelect.innerHTML = '<option value="">選択してください</option>';

            if (!selectedDateStr) return;

            // 選択日付のパース
            const parts = selectedDateStr.split("-");
            const year = parseInt(parts[0], 10);
            const month = parseInt(parts[1], 10) - 1;
            const day = parseInt(parts[2], 10);

            const isToday = (
                now.getFullYear() === year &&
                now.getMonth() === month &&
                now.getDate() === day
            );

            // 今日選択時の最速受取可能時刻（現在時刻 + 30分）
            const minAllowedTime = new Date(now.getTime() + LEAD_TIME_MIN * 60 * 1000);

            let hasOption = false;

            // 11:00 から 15:00 まで 10分刻みで生成
            for (let h = START_HOUR; h <= END_HOUR; h++) {
                for (let m = 0; m < 60; m += INTERVAL_MIN) {
                    // 15:00を超えた時間（15:10など）は選択肢に含めない
                    if (h === END_HOUR && m > 0) break;

                    const slotTime = new Date(year, month, day, h, m, 0);

                    // 今日かつ「現在時刻 + 30分」より前の時間はスキップ
                    if (isToday && slotTime < minAllowedTime) {
                        continue;
                    }

                    const timeStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
                    const option = document.createElement("option");
                    option.value = timeStr;
                    option.textContent = timeStr;
                    pickupTimeSelect.appendChild(option);

                    hasOption = true;
                }
            }

            // 受取可能時間が存在しない場合（例: 本日の14:31以降にアクセスした場合）
            if (!hasOption) {
                pickupTimeSelect.innerHTML = '<option value="">本日の受付は終了しました</option>';
            }
        }

        // イベントリスナー設定
        pickupDateSelect.addEventListener("change", updateTimeOptions);

        // 初期描画実行
        updateTimeOptions();
    }

    // -------------------------------------------------------------
    // 2. メモ入力欄の残り文字数リアルタイムカウント
    // -------------------------------------------------------------
    const memoArea = document.getElementById("memo");
    const memoCountSpan = document.getElementById("memoCount");

    if (memoArea && memoCountSpan) {
        const MAX_LENGTH = 100;

        const updateMemoCount = () => {
            const remaining = MAX_LENGTH - memoArea.value.length;
            memoCountSpan.textContent = remaining;
        };

        memoArea.addEventListener("input", updateMemoCount);
        updateMemoCount(); // 初期表示
    }
});