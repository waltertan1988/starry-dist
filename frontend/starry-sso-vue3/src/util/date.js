
function dateToString(dateTimeStamp){
    let date = new Date(dateTimeStamp)
    date.setTime(date.getTime() + date.getTimezoneOffset() * 60000) //使用默认时区展示时间戳
    return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2,'0')}-${date.getDate().toString().padStart(2,'0')} ${date.getHours().toString().padStart(2,'0')}:${date.getMinutes().toString().padStart(2,'0')}:${date.getSeconds().toString().padStart(2,'0')}`
}

const DateUtil = {
    dateToString
}


export default DateUtil

