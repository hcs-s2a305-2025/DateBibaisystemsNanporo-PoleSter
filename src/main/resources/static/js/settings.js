document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('toggleSchedule');
    // 対象の入力要素・ボタンをまとめて取得
    const controls = document.querySelectorAll('.schedule-control');

    // 状態を更新する関数
    const updateState = () => {
        const isEnabled = toggle.checked;
        controls.forEach(control => {
        // スイッチがOFF(!isEnabled)なら disabled を true に設定
        control.disabled = !isEnabled;
        });
    };

    // 初期状態の設定（ページ読み込み時）
    updateState();

    // スイッチの切り替えイベントの検知
    toggle.addEventListener('change', updateState);
});