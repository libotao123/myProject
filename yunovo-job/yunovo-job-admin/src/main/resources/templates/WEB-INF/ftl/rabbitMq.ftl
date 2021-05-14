<!DOCTYPE html>
<html>
<head>
  	<#import "/common/common.macro.ftl" as netCommon>
	<@netCommon.commonStyle />
	<title>${I18n.admin_name}</title>
</head>
<body class="hold-transition skin-blue sidebar-mini <#if cookieMap?exists && "off" == cookieMap["xxljob_adminlte_settings"].value >sidebar-collapse</#if> ">
<link rel="stylesheet" href="${request.contextPath}/static/plugins/dataTables/css/jquery.dataTables.css">
<style type="text/css">
	.table th {
		text-align: center;
		vertical-align: middle!important;
		border: 1px solid #f4f4f4 !important;
	}
	
	.dataTables_wrapper.no-footer .dataTables_scrollBody {
	    border-bottom: 0px solid #111!important;
	}
	
</style>
<div class="wrapper">
	<!-- header -->
	<@netCommon.commonHeader />
	<!-- left -->
	<@netCommon.commonLeft "rabbitMq" />
	<div class="content-wrapper">
		<section class="content-header">
			<h1>Rabbit MQ 监控</h1>
		</section>
		
		<section class="content">
	      <div class="row">
	        <div class="col-xs-12">
	          <div class="nav-tabs-custom">
	            <ul class="nav nav-tabs">
	              <li class="active"><a href="#connections_tab" onclick="getConnections();" data-toggle="tab">socket连接</a></li>
	              <li><a href="#channels_tab" onclick="getChannels();" data-toggle="tab">通道</a></li>
	              <li><a href="#queues_tab" onclick="getQueues();" data-toggle="tab">队列</a></li>
	            </ul>
	            <div class="tab-content">
	 				<!--Connections-->
	 				<div class="tab-pane active" id="connections_tab">
	 					<div class="box-body pad">
	 						<div class="col-xs-1 pull-right">
				            	<button class="btn btn-block btn-info" onclick="getConnections();"><i class="fa fa-refresh"></i>&nbsp;&nbsp;&nbsp;刷新</button>
				            </div>
	                        <table class="table table-bordered col-md-offset-1" style="width: 70%;">
	                            <thead>
	                            	<tr class="success"><th  colspan="4">概观</th><th  colspan="3">细节</th><th  colspan="2">网络</th></tr>
	                                <tr class="info"><th>虚拟主机</th><th>名称</th><th>用户名</th><th>状态</th><th>SSL/TLS</th><th>协议</th><th>通道</th><th>来自客户端</th><th>发送客户端</th></tr>
	                            </thead>
	                            <tbody id="con_Tab">
	                            </tbody>
	                        </table>
	                    </div>
	 				</div>
					<!--/end Connections-->
	
	
					<!-- Channels -->
					<div class="tab-pane" id="channels_tab">
						<div class="box-body pad">
							<div class="col-xs-1 pull-right" style="margin-bottom:10px">
				            	<button class="btn btn-block btn-info" onclick="getChannels();"><i class="fa fa-refresh"></i>&nbsp;&nbsp;&nbsp;刷新</button>
				            </div>
							<table id="example"  class="table table-bordered " style="border: 1px solid #f4f4f4;">
	                            <thead>
	                                <tr style="background-color: #d9edf7;"><th>通道</th><th>虚拟主机</th><th>用户名</th><th>Mode&nbsp;&nbsp;<button type="button" onclick="modeDetails();" class="btn btn-info btn-xs">?</button></th><th>状态</th><th>未证实的</th><th>prefetch&nbsp;&nbsp;<button type="button" onclick="modePrefetch();" class="btn btn-info btn-xs">?</button></th><th>未受请求的</th><th>发布</th><th>确认</th><th>发送 / 获取</th><th>ack</th></tr>
	                            </thead>
	                            <tbody id="chan_Tab">
	                            </tbody>
	                        </table>
						
							<!--
	                        <table id="example" class="table table-bordered col-md-offset-1" style="width: 70%;">
	                            <thead>
	                            	<tr style="background-color: #f2dede;"><th colspan="5">概观</th><th colspan="3">细节</th><th colspan="4">消息率</th></tr>
	                                <tr class="warning"><th>通道</th><th>虚拟主机</th><th>用户名</th><th>mode?</th><th>状态</th><th>未证实的</th><th>Prefetch ?</th><th>未受请求的</th><th>发布</th><th>确认</th><th>发送 / 获取</th><th>ack</th></tr>
	                            </thead>
	                            <tbody id="chan_Tab">
	                            </tbody>
	                        </table> -->
	                    </div>
					</div>
					<!--/end Channels --> 
					
					<!-- Queues -->
					<div class="tab-pane" id="queues_tab">
						<div class="box-body pad">
							<div class="col-xs-1 pull-right">
				            	<button class="btn btn-block btn-info" onclick="getQueues();"><i class="fa fa-refresh"></i>&nbsp;&nbsp;&nbsp;刷新</button>
				            </div>
	                        <table class="table table-bordered col-md-offset-1" style="width: 70%;">
	                            <thead>
	                            	<tr class="success"><th colspan="4">概观</th><th colspan="3">消息</th><th colspan="3">消息处理率</th></tr>
	                                <tr class="info"><th>虚拟主机</th><th>队列名</th><th>特征</th><th>状态</th><th>未消费的</th><th>Unacked</th><th>总数</th><th>生产</th><th>发送 / 接收</th><th>ack</th></tr>
	                            </thead>
	                            <tbody id="que_Tab_tbody">
	                            </tbody>
	                        </table>
	                    </div>
					</div>
					<!--/end Queues --> 
	              </div>
	          </div>
	        </div>
	      </div>
	    </section>
	    
	    
		
		
	
	</div> 
	

	
	
	
	<!-- Content Wrapper. Contains page content -->
	<!-- <div class="content-wrapper">
		<iframe src="http://192.168.3.241:15672/" style="width:100%;height: calc(100vh - 55px);"></iframe>
	</div> -->
	<!-- /.content-wrapper -->
	
	<!-- footer -->
	<@netCommon.commonFooter />
</div>

<@netCommon.commonScript />
</body>
<script src="${request.contextPath}/static/plugins/dataTables/js/jquery.dataTables.js"></script>
<script type="text/javascript">
	$(function(){
		getConnections();
	});

	
	function getConnections() {
		$.ajax({
                url: base_url +"/secure/rabbitMq/getConnections",
                headers: { Accept: "application/json" },
                type: "POST",
                dataType: "json",
                data: {},              
                traditional:true,
                success: function(result) {
					if (result.code === 200) {
						var data = result.content;
						$('#con_Tab').html("");
						var html = "";
						if (data.length==0)　{
							html += '<tr><td colspan="12">没有数据</td></tr>';
						} else {
							for (var i=0; i<data.length; i++) {
								html += '<tr><td>'+data[i].Vhost+'</td>';
								html += '<td>'+data[i].name+'</br>'+data[i].user_provided_name+'</td>';
								html += '<td>'+data[i].user+'</td>';
								if (data[i].idle_since == undefined) {
									html += '<td style="color:#98f898"><i class="fa fa-square" ></i>&nbsp;'+data[i].state+'</td>';
								} else {
									html += '<td style="color:#ddd"><i class="fa fa-square" ></i>&nbsp;闲置</td>';
								}
								
								html += '<td>-</td>';
								html += '<td>'+data[i].protocol+'</td>';
								html += '<td>'+data[i].channels+'</td>';
								html += '<td>0B/s</td>';
								html += '<td>0B/s</td></tr>';
							}
						}
						$('#con_Tab').append(html);
					} else {
						alert("error");
					}
					
                }
  		});
	}
	
	function getChannels() {
		$.ajax({
                url: base_url +"/secure/rabbitMq/getChannels",
                headers: { Accept: "application/json" },
                type: "POST",
                dataType: "json",
                data: {},              
                traditional:true,
                success: function(result) {
					if (result.code === 200) {
						var data = eval('(' + result.content + ')');
						$('#chan_Tab').html("");
						var html = "";
						for (var i=0; i<data.length; i++) {
							html += '<tr><td>'+data[i].name+'</td>';
							html += '<td>'+data[i].vhost+'</td>';
							html += '<td>'+data[i].user+'</td>';
							html += '<td>'+(data[i].confirm==true?'<span class="label label-info">C</span>':'')+(data[i].transactional==true?'<span class="label label-info">T</span>':'')+'</td>'; 
							if (data[i].idle_since == undefined) {
								html += '<td style="color:#98f898"><i class="fa fa-square" ></i>&nbsp;'+data[i].state+'</td>';
							} else {
								html += '<td style="color:#ddd"><i class="fa fa-square" ></i>&nbsp;闲置</td>';
							}
							html += '<td>'+data[i].messages_unconfirmed+'</td>';
							html += '<td>'+data[i].prefetch_count+'</td>';
							html += '<td>'+data[i].messages_unacknowledged+'</td>';
							
							if (data[i].message_stats != undefined && data[i].message_stats.publish_details != undefined) {
								html += '<td>'+data[i].message_stats.publish_details.rate+'/s</td>';
							} else {
								html += '<td></td>';
							}
							if (data[i].message_stats != undefined && data[i].message_stats.confirm_details != undefined) {
								html += '<td>'+data[i].message_stats.confirm_details.rate+'/s</td>';
							} else {
								html += '<td></td>';
							}
							
							if (data[i].message_stats != undefined && data[i].message_stats.deliver_get_details != undefined) {
								html += '<td>'+data[i].message_stats.deliver_get_details.rate+'/s</td>';
							} else {
								html += '<td></td>';
							}
							if (data[i].message_stats != undefined && data[i].message_stats.ack_details != undefined) {
								html += '<td>'+data[i].message_stats.ack_details.rate+'/s</td></tr>';
							} else {
								html += '<td></td>';
							}
						}
						
						$('#chan_Tab').append(html);
						initTable();
					} else {
						alert("error");
					}
                }
  		});
	}
	
	function getQueues() {
		$.ajax({
                url: base_url +"/secure/rabbitMq/getQueues",
                headers: { Accept: "application/json" },
                type: "POST",
                dataType: "json",
                data: {},              
                traditional:true,
                success: function(result) {
                	if (result.code === 200) {
						var data = eval('(' + result.content + ')');
						$('#que_Tab_tbody').html("");
						var html = "";
						for (var i=0; i<data.length; i++) {
							html += '<tr><td>'+data[i].vhost+'</td>';
							html += '<td>'+data[i].name+'</td>';
							html += '<td>'+(data[i].durable==true?'<span class="label label-info">D</span>':'')+(data[i].internal==true?'<span class="label label-info">I</span>':'')+'</td>';
							if (data[i].idle_since == undefined) {
								html += '<td style="color:#98f898"><i class="fa fa-square" ></i>&nbsp;'+data[i].state+'</td>';
							} else {
								html += '<td style="color:#ddd"><i class="fa fa-square" ></i>&nbsp;闲置</td>';
							}
							html += '<td>'+data[i].messages_ready+'</td>';
							html += '<td>'+data[i].messages_unacknowledged+'</td>';
							html += '<td>'+data[i].messages+'</td>';
							if (data[i].message_stats != undefined && data[i].message_stats.publish_details != undefined) {
								html += '<td>'+data[i].message_stats.publish_details.rate+'/s</td>';
							} else {
								html += '<td></td>';
							}
							if (data[i].message_stats != undefined && data[i].message_stats.deliver_get_details != undefined) {
								html += '<td>'+data[i].message_stats.deliver_get_details.rate+'/s</td>';
							} else {
								html += '<td></td>';
							}
							if (data[i].message_stats != undefined && data[i].message_stats.ack_details != undefined) {
								html += '<td>'+data[i].message_stats.ack_details.rate+'/s</td></tr>';
							} else {
								html += '<td></td>';
							}
							
							
						}
						$('#que_Tab_tbody').append(html);
					} else {
						alert("error");
					}

                }
  		});
	}
	
	function initTable() {
		var oldTable = $('#example').dataTable();
		oldTable.fnDestroy(); //还原初始化了的dataTable
		$('#example').dataTable( {
			"info":false,//是否显示分页信息
			"ordering":true,//是否开启排序功能
			"paging": false,//开启分页
	         "scrollY": 400,
	         //"scrollX": false,
	         "searching": false,   //搜索框，不显示
	         "oLanguage" : { // 国际化配置
		           "sProcessing" : "正在获取数据，请稍后...",
		           "sLengthMenu" : "显示 _MENU_ 条",
		           "sZeroRecords" : "没有找到数据",
		           "sInfo" : "从 _START_ 到  _END_ 条记录 总记录数为 _TOTAL_ 条",
		           "sInfoEmpty" : "记录数为0",
		           "sInfoFiltered" : "(全部记录数 _MAX_ 条)",
		           "sInfoPostFix" : "",
		           "sSearch" : "查询",
		           "sUrl" : "",
		           "oPaginate" : {
		               "sFirst" : "第一页",
		               "sPrevious" : "上一页",
		               "sNext" : "下一页",
		               "sLast" : "最后一页"
		           }
		      }
	     });
		
	}
	
	function modeDetails() {
		layer.alert('Mode是渠道保证模式。可以是以下之一，也可以不是：</br><span class="label label-info">C</span> - 确认 </br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;频道将发送流式发布确认。</br><span class="label label-info">T</span> - 交易  </br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;渠道是交易性的。', {
		    skin: 'layui-layer-lan'
		    ,closeBtn: 0
		    ,anim: 4 //动画类型
		  });
	}
	
	function modePrefetch() {
		layer.alert('频道预取计数。</br></br>每个通道可以有两个预取计数：每个消费者计数，这将限制在通道上创建的每个新消费者，以及全局计数，该通道在通道上的所有消费者之间共享。</br></br>如果设置了此列，则此列显示一个，另一个或两个限制。', {
		    skin: 'layui-layer-lan'
		    ,closeBtn: 0
		    ,anim: 4 //动画类型
		  });
	}
	
	
       
</script>

</html>
