/////////////////////////////////////////////////////////////
/// 科大讯飞语音识别插件
/// create by wilhan.tian
/////////////////////////////////////////////////////////////
var exec = require('cordova/exec');

/// 初始化Xfyun
/// appid: 应用ID 可在官网查询
exports.init = function(appid, success, error) {
    exec(success, error, "Xfyun", "init", [appid]);
};


/// 开始监听识别
/// language:语言
/// success(res)
///     res: {
///         action: string,//(onVolumeChanged | onResult | onBeginOfSpeech | onEndOfSpeech)
///         data: any,//({volume:number,data:byte[]} | {result:{},isLast:boolean} | void | void)
///     }
/// error(errCode)
///     errCode: 错误码（除官方错误码外，还包含本插件错误码。-1:未成功构建命令, -2:用户未赋予相关权限）
exports.startListening = function(language, success, error) {
    exec(success, error, "Xfyun", "startListeningGrammar", [language]);
};

/// 停止命令识别(只是停止录制，停止后立即将语音进行识别，回调会继续执行。可通过startListeningGrammar继续监听)
exports.stopListening = function(success, error) {
    exec(success, error, "Xfyun", "stopListeningGrammar", []);
};

/// 取消命令识别(完全释放语音识别功能，相关回调不在执行，需要重新构建命令[buildGrammar])
exports.clear = function(success, error) {
    exec(success, error, "Xfyun", "cancelGrammar", []);
};