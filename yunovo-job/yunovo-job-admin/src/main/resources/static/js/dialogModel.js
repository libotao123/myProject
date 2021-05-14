/**
 * Created by yangzs on 2015/12/21. 弹出模式框，bootstrap自定义插件
 * ref:http://v3.bootcss.com/javascript/#modals
 * ref:http://www.runoob.com/bootstrap/bootstrap-modal-plugin.html
 */
dialogModel = {
	msgDefaults : {
		targetid : '',
		title : '',
		message : '',
		width : '',
		height : '',
		backdrop : 'static', //是否显示遮障
		keyboard : false, //是否开启esc键退出
		openEvent : null, //弹窗打开后回调函数
		closeEvent : function(_targetid) {
			$("#" + _targetid).remove();
		}, //弹窗关闭后回调函数
		closeText : "关闭"
	},
	confirmDefaults : {
		targetid : '',
		title : '',
		message : '',
		width : '',
		height : '',
		backdrop : 'static', //是否显示遮障
		keyboard : false, //是否开启esc键退出
		openEvent : null, //弹窗打开后回调函数
		closeEvent : function(_targetid) {
			$("#" + _targetid).remove();
		}, //弹窗关闭后回调函数
		okEvent : null, //单击确定按钮回调函数
		closeText : "关闭",
		okText : "确定"
	},
	defaults : {
		targetid : '',
		url : '',
		params : {},
		method : 'GET',
		width : '',
		height : '',
		backdrop : 'static', //是否显示遮障
		keyboard : false, //是否开启esc键退出
		openEvent : null, //弹窗打开后回调函数
		closeEvent : function(_targetid) {
			$("#" + _targetid).remove();
		} //弹窗关闭后回调函数
	},	
	htmlContent : {
		outHtml : "<div class='modal fade' id='#dataid'><div class='modal-dialog'><div class='modal-content'>#datacontent</div></div></div>",
		contentHtml : "<div class='modal-header'><button type='button' class='close' data-dismiss='modal' aria-label='Close'><span aria-hidden='true'>&times;</span></button><h4 class='modal-title'>#datatitle</h4></div><div class='modal-body'>#databody</div><div class='modal-footer'>#databutton</div>",
		closeButtonHtml : "<button type='button' class='btn btn_cancel' data-dismiss='modal'>#closetext</button>",
		submitButtonHtml : "<button type='button' data-type='comfirm' class='btn btn_save'>#oktext</button>"
	},
	dialogHtml : function(targetId) {
		return "<div class=\"modal fade\" id=\"" + targetId + "\"><div class=\"modal-dialog\"><div class=\"modal-content\"></div></div></div>"
	},
	basedialog : function(param_options) {
		var options = dialogModel.copy(dialogModel.defaults, param_options);
		var errorMsg = {
				targetid : "dialogMsgError",
				title : "温馨提示",
				message : "连接不存在或刷新页面后重新尝试！"
		};
		var html = this.dialogHtml(options.targetid);
		$("body").append(html);
		$.ajax({
			'type' : options.method,
			'url' : options.url,
			'data' : options.params,
			'async' : false,
			'success' : function(data, status) {
				if (status != 'success') {
					$('#' + options.targetid).modal('hide')
					$('#' + options.targetid).remove();
					$("body").removeAttr("style");
					dialogModel.msg(errorMsg);
					return;
				} else {
					$("#" + options.targetid).find(".modal-content").html(data);
					// 设置宽高
					if (options.width) {
						dialogModel.resetWidth(options.targetid, options.width);
					}
					if (options.height) {
						dialogModel.resetHeight(options.targetid, options.height);
					}
					$('#' + options.targetid).modal({
						keyboard : options.keyboard,
						backdrop : options.backdrop
					});
					$('#' + options.targetid).on('show.bs.modal', function () {
						if (options.openEvent) {
							options.openEvent();
						}
					});
				}
			},
			'error' : function() {
				$('#' + options.targetid).modal('hide')
				$('#' + options.targetid).remove();
				$("body").removeAttr("style")
				dialogModel.msg(errorMsg);
				return;
			}
		});
		$('#' + options.targetid).on('hide.bs.modal', function() {
			if (options.closeEvent) {
				options.closeEvent(options.targetid);
			}
		});
		$("body").removeAttr("style")
	},
	basecomfirm : function(param_options) {
		var options = dialogModel.copy(dialogModel.confirmDefaults, param_options);
		var contentHtml = this.htmlContent.contentHtml;
		var databutton = this.htmlContent.closeButtonHtml + this.htmlContent.submitButtonHtml;
		databutton = databutton.replace("#closetext", options.closeText);
		databutton = databutton.replace("#oktext", options.okText);
		var htmlText = this.htmlContent.outHtml.replace("#dataid", options.targetid);
		htmlText = htmlText.replace("#datacontent", contentHtml);
		htmlText = htmlText.replace("#datatitle", options.title);
		htmlText = htmlText.replace("#databody", options.message);
		htmlText = htmlText.replace("#databutton", databutton);
		$("body").append(htmlText);
		// 设置宽高
		if (options.width) {
			dialogModel.resetWidth(options.targetid, options.width);
		}
		if (options.height) {
			dialogModel.resetHeight(options.targetid, options.height);
		}
		$('#' + options.targetid).modal({
			keyboard : options.keyboard,
			backdrop : options.backdrop
		});
		$('#' + options.targetid).on('show.bs.modal', function () {
			if (options.openEvent) {
				options.openEvent();
			}
		});
		$('#' + options.targetid).on('hide.bs.modal', function() {
			if (options.closeEvent) {
				options.closeEvent(options.targetid);
			}
		});
		$('#' + options.targetid).find("button[data-type='comfirm']").click(function() {
			if (options.okEvent) {
				options.okEvent();
			}
		});
		$("body").removeAttr("style")
	},
	basemsg : function(param_options) {
		var options = dialogModel.copy(dialogModel.msgDefaults, param_options);
		var contentHtml = this.htmlContent.contentHtml;
		var databutton = this.htmlContent.closeButtonHtml;
		databutton = databutton.replace("#closetext", options.closeText);
		var htmlText = this.htmlContent.outHtml.replace("#dataid", options.targetid);
		htmlText = htmlText.replace("#datacontent", contentHtml);
		htmlText = htmlText.replace("#datatitle", options.title);
		htmlText = htmlText.replace("#databody", options.message);
		htmlText = htmlText.replace("#databutton", databutton);
		$("body").append(htmlText);
		// 设置宽高
		if (options.width) {
			dialogModel.resetWidth(options.targetid, options.width);
		}
		if (options.height) {
			dialogModel.resetHeight(options.targetid, options.height);
		}
		$('#' + options.targetid).modal({
			keyboard : options.keyboard,
			backdrop : options.backdrop
		});
		$('#' + options.targetid).on('show.bs.modal', function () {
			if (options.openEvent) {
				options.openEvent();
			}
		});
		$('#' + options.targetid).on('hide.bs.modal', function() {
			if (options.closeEvent) {
				options.closeEvent(options.targetid);
			}
		});
		$("body").removeAttr("style")
	},
	dialog : function(options) {
		if (window.top != window) {
			window.top.dialogModel.basedialog(options);
		} else {
			dialogModel.basedialog(options);
		}
	},
	comfirm : function(options) {
		window.top.dialogModel.basecomfirm(options);
	},
	msg : function(options) {
		window.top.dialogModel.basemsg(options);
	},
	resetWidth : function(targetid, width) {
		$("#" + targetid).find(".modal-body").css({
			"overflow-x" : "auto"
		});
		$("#" + targetid).find(".modal-dialog").css({"width" : width + "px"});
	},
	resetHeight : function(targetid, height) {
		$("#" + targetid).find(".modal-dialog").css({"height" : height});
		var modelHeaderHeight = $("#" + targetid).find(".modal-header").outerHeight();
		var modelFooterHeight = $("#" + targetid).find(".modal-footer").outerHeight();
		$("#" + targetid).find(".modal-body").css({
			"height" : (height - modelHeaderHeight - modelFooterHeight) + "px",
			"overflow-y" : "auto"
		});
	},
	copy : function(defaultOptions, clientOptions) {
		for(var p in defaultOptions) {  
			var name = p;//属性名称   
			var value = clientOptions[p];//属性对应的值
			if (value == null || typeof(value) == 'undefined' || value == '' || value == 'null') {
				clientOptions[name] = defaultOptions[name];
			} 
		}  
		return clientOptions;
	}
}