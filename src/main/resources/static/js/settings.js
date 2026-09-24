document.addEventListener('DOMContentLoaded', () => {
    // アラート表示
    const urlParams = new URLSearchParams(window.location.search);

    if (urlParams.has('success')) {
        alert('変更を保存しました。');
        history.replaceState(null, '', window.location.pathname);
    } else if (urlParams.has('error')) {
        alert('変更できませんでした。もう一度お試しください。');
        history.replaceState(null, '', window.location.pathname);
    }
    
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