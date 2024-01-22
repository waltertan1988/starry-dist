
const KEYS = {
    PRINCIPAL: {
        NICKNAME: "principal.nickname"
    }
}

function set(key, val, config){
    let array = [];

    array.push(key + "=" + val)

    if(config){
        if(config.expireInMs){
            var d = new Date();
            d.setTime(d.getTime() + config.expireInMs);
            array.push("expires=" + d.toGMTString())
        }
        if(config.path){
            array.push("path=" + config.path)
        }
        if(config.domain){
            array.push("domain=" + config.domain)
        }
    }

    document.cookie = array.join(";")
}

function get(key){
    let name = key + "=";
    let ca = document.cookie.split(';');
    for(let i=0; i<ca.length; i++) {
        let c = ca[i].trim();
        if (c.indexOf(name)===0) {
            return c.substring(name.length, c.length)
        }
    }
    return null;
}

function del(key) {
    const date = new Date()
    date.setTime(date.getTime() - 1)
    document.cookie = `${key}=; expires=${date.toUTCString()}; path=/;`
}

const Cookie = {
    KEYS,
    get,
    set,
    del
}

export default Cookie