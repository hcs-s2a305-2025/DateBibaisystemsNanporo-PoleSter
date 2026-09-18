//************************************
// デジタル時計のコード
//***********************************

function displayTime() {
    const padZero = value => value.toString().padStart(2, '0');

    const now = new Date();
    const hour = padZero(now.getHours());
    const minute = padZero(now.getMinutes());
    const second = padZero(now.getSeconds());

    const currentTime = `${hour}:${minute}:${second}`;
    document.querySelector('.clock').textContent = currentTime; 
}

window.addEventListener("load", ()=>{
            // 曜日を漢字で表示するため
            let week = ["日", "月", "火", "水", "木", "金", "土"];

            // 現在日時を取得
            let today = new Date();

            // 年月日を取得
            let year = today.getFullYear();

            let month = today.getMonth() + 1;        // 1月~12月 -> 0~11

            let date = today.getDate();

            // 曜日を取得
            let day = today.getDay();                // 日~土 -> 0~6

            // 現在時刻を表示する
            document.getElementById("date").innerHTML =
                            year + "/" + month + "/" + date +             // 年月日
                            "(" + week[day] + ")" ;    // 曜日
        });


displayTime();
setInterval(displayTime, 1000);