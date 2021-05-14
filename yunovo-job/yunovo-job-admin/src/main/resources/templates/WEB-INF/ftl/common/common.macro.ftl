<#macro commonStyle>

	<#-- favicon -->
	<link rel="icon" href="favicon.ico" />

	<meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <!-- Tell the browser to be responsive to screen width -->
    <meta content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" name="viewport">
    <!-- Bootstrap 3.3.5 -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/bootstrap/css/bootstrap.min.css">
    <!-- Font Awesome -->
    <!-- <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.5.0/css/font-awesome.min.css"> -->
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/font-awesome-4.5.0/css/font-awesome.min.css">
    <!-- Ionicons -->
    <!-- <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/ionicons/2.0.1/css/ionicons.min.css"> -->
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/ionicons-2.0.1/css/ionicons.min.css">
    <!-- Theme style -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/AdminLTE-local.min.css">
    <!-- AdminLTE Skins. Choose a skin from the css/skins folder instead of downloading all of them to reduce the load. -->
    <link rel="stylesheet" href="${request.contextPath}/static/adminlte/dist/css/skins/_all-skins.min.css">
      
	<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
    <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
    <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

	<!-- pace -->
	<link rel="stylesheet" href="${request.contextPath}/static/plugins/pace/themes/pace-theme-flash.css">

	<#-- i18n -->
	<#global I18n = I18nUtil.getMultString()?eval />
	<style type="text/css">
		body {
			color:#73879C;
		}
		.navbar {
		    background: #EDEDED !important;
		    border-bottom: 1px solid #D9DEE4 !important;
		}
		.content-wrapper {
			background: #F7F7F7 !important;
		}
		.main-sidebar {
			background: #2A3F54 !important;
		}
		.logo {
			height:70px !important;
			background: #2A3F54 !important;
			
		}
		.logo img {
			margin-top:10px !important;
		}
		.header {
			background: #2A3F54 !important;
		}
		.sidebar-toggle:hover{ background:#2A3F54 !important;}
		.skin-blue .sidebar-menu>li:hover>a, .skin-blue .sidebar-menu>li.active>a {
			border-right-color : #1ABB9C !important;
			border-style:none solid none none; 
			border-width:5px !important;
		    text-shadow: rgba(0,0,0,.25) 0 -1px 0 !important;
		    background: linear-gradient(#334556,#2C4257),#2A3F54 !important;
		    box-shadow: rgba(0,0,0,.25) 0 1px 0, inset rgba(255,255,255,.16) 0 1px 0 !important;
		}
		.dropdown-menu{ 
			width:220px;
			box-shadow: 0 6px 12px rgba(0,0,0,.175);
		}
	</style>

</#macro>

<#macro commonScript>
	<!-- jQuery 2.1.4 -->
	<script src="${request.contextPath}/static/adminlte/plugins/jQuery/jquery-2.2.3.min.js"></script>
	<!-- Bootstrap 3.3.5 -->
	<script src="${request.contextPath}/static/adminlte/bootstrap/js/bootstrap.min.js"></script>
	<!-- FastClick -->
	<script src="${request.contextPath}/static/adminlte/plugins/fastclick/fastclick.min.js"></script>
	<!-- AdminLTE App -->
	<script src="${request.contextPath}/static/adminlte/dist/js/app.min.js"></script>
	<#-- jquery.slimscroll -->
	<script src="${request.contextPath}/static/adminlte/plugins/slimScroll/jquery.slimscroll.min.js"></script>

    <!-- pace -->
    <script src="${request.contextPath}/static/plugins/pace/pace.min.js"></script>
    <#-- jquery cookie -->
	<script src="${request.contextPath}/static/plugins/jquery/jquery.cookie.js"></script>

	<#-- layer -->
	<script src="${request.contextPath}/static/plugins/layer/layer.js"></script>

	<#-- common -->
    <script src="${request.contextPath}/static/js/common.1.js"></script>
    <#-- 注销确认弹框 -->
    <script src="${request.contextPath}/static/js/dialogModel.js"></script>
 
    <script>
		var base_url = '${request.contextPath}';
        var I18n = ${I18nUtil.getMultString()};
	</script>
	
	<script type="text/javascript">
		function toLogOut(){
			var obj = {
		        targetid : "exit_project",
		        title : "温馨提示",
		        message : "确定要退出系统吗?",
		        okEvent : function(){
		        	$('#exit_project').modal("hide");
		        	window.location.href =  base_url+"/index/logout";
					return true;
		        }
		    }
			dialogModel.comfirm(obj);    
		}
		
		function toUserCenter() {
			window.location.href =  base_url+"/index/toUserCenter";
		}

</script>

</#macro>

<#macro commonHeader>
	<header class="main-header">
		<div>
		<a href="${request.contextPath}/secure/" class="logo">
			<span class="logo-mini"><img src="${request.contextPath}/static/img/logo_icon.png"  alt="" style="margin-top: -0px;width:40px;"/></span>
			<span class="logo-lg"><img src="${request.contextPath}/static/img/logo.png"  alt="" style="margin-top: -18px;"/></span><!--<b>${I18n.admin_name}</b>    <img src="${request.contextPath}/static/img/logo.png"  alt="" />-->
		</a>
		<div>
		<nav class="navbar navbar-static-top" role="navigation" style="height:50px">
			<a href="#" class="sidebar-toggle" data-toggle="offcanvas" role="button" style="color: #5A738E;"><span class="sr-only">切换导航</span></a>
			<!-- <a id="menu_toggle"  data-toggle="offcanvas" role="button" style="width:50px;height:50px;color: #5A738E;"><i class="fa fa-bars" style="width:50px;height:50px"></i></a> -->
			
          	<div class="navbar-custom-menu">
				<ul class="nav navbar-nav">
					<li class="dropdown">
						<a href="#" class="dropdown-toggle" data-toggle="dropdown" style="color:#5A738E;">
							<img src="${request.contextPath}/static/img/logo_icon.png" alt="" style="height:16px;margin-right:10px">
							<span class="hidden-xs">${systemUser.loginName}</span>
							<b class="caret"></b>
						</a>
						<ul class="dropdown-menu">
							<li><a href="javascript:;" onclick="toUserCenter();" style="height:40px;line-height:40px"><i class="glyphicon glyphicon-home pull-right" style="margin-top:10px"></i>门户</a></li>
            				<li><a href="javascript:;" onclick="toLogOut();"  style="height:40px;line-height:40px"><i class="fa fa-sign-out pull-right" style="margin-top:10px"></i> 退出</a></li>
						</ul>
					</li>
				</ul>
			</div>
		</nav>
	</header>
</#macro>

<#macro commonLeft pageName >
	<!-- Left side column. contains the logo and sidebar -->
	<aside class="main-sidebar">
		<!-- sidebar: style can be found in sidebar.less -->
		<section class="sidebar">
			<!-- sidebar menu: : style can be found in sidebar.less -->
			<ul class="sidebar-menu" style="margin-top:50px">
				<!-- <li class="header" style="font-size:20px;margin-left:10%;color:#FFF;">任务调度中心</li> -->
				<li class="nav-click <#if pageName == "jobinfo">active</#if>" ><a href="${request.contextPath}/secure/jobinfo"><i class="fa fa-circle-o text-aqua"></i><span>${I18n.jobinfo_name}</span></a></li>
				<li class="nav-click <#if pageName == "joblog">active</#if>" ><a href="${request.contextPath}/secure/joblog"><i class="fa fa-circle-o text-yellow"></i><span>${I18n.joblog_name}</span></a></li>
                <li class="nav-click <#if pageName == "jobgroup">active</#if>" ><a href="${request.contextPath}/secure/jobgroup"><i class="fa fa-circle-o text-green"></i><span>${I18n.jobgroup_name}</span></a></li>
				<li class="nav-click <#if pageName == "rabbitMq">active</#if>" ><a href="${request.contextPath}/secure/rabbitMq/link"><i class="fa fa-circle-o text-gray"></i><span>RabbitMQ</span></a></li>
				<li class="nav-click <#if pageName == "help">active</#if>" ><a href="${request.contextPath}/secure/help"><i class="fa fa-circle-o text-aqua"></i><span>${I18n.job_help}</span></a></li>
			</ul>
		</section>
		<!-- /.sidebar -->
	</aside>
</#macro>

<#macro commonControl >
	<!-- Control Sidebar -->
	<aside class="control-sidebar control-sidebar-dark">
		<!-- Create the tabs -->
		<ul class="nav nav-tabs nav-justified control-sidebar-tabs">
			<li class="active"><a href="#control-sidebar-home-tab" data-toggle="tab"><i class="fa fa-home"></i></a></li>
			<li><a href="#control-sidebar-settings-tab" data-toggle="tab"><i class="fa fa-gears"></i></a></li>
		</ul>
		<!-- Tab panes -->
		<div class="tab-content">
			<!-- Home tab content -->
			<div class="tab-pane active" id="control-sidebar-home-tab">
				<h3 class="control-sidebar-heading">近期活动</h3>
				<ul class="control-sidebar-menu">
					<li>
						<a href="javascript::;">
							<i class="menu-icon fa fa-birthday-cake bg-red"></i>
							<div class="menu-info">
								<h4 class="control-sidebar-subheading">张三今天过生日</h4>
								<p>2015-09-10</p>
							</div>
						</a>
					</li>
					<li>
						<a href="javascript::;"> 
							<i class="menu-icon fa fa-user bg-yellow"></i>
							<div class="menu-info">
								<h4 class="control-sidebar-subheading">Frodo 更新了资料</h4>
								<p>更新手机号码 +1(800)555-1234</p>
							</div>
						</a>
					</li>
					<li>
						<a href="javascript::;"> 
							<i class="menu-icon fa fa-envelope-o bg-light-blue"></i>
							<div class="menu-info">
								<h4 class="control-sidebar-subheading">Nora 加入邮件列表</h4>
								<p>nora@example.com</p>
							</div>
						</a>
					</li>
					<li>
						<a href="javascript::;">
						<i class="menu-icon fa fa-file-code-o bg-green"></i>
						<div class="menu-info">
							<h4 class="control-sidebar-subheading">001号定时作业调度</h4>
							<p>5秒前执行</p>
						</div>
						</a>
					</li>
				</ul>
				<!-- /.control-sidebar-menu -->
			</div>
			<!-- /.tab-pane -->

			<!-- Settings tab content -->
			<div class="tab-pane" id="control-sidebar-settings-tab">
				<form method="post">
					<h3 class="control-sidebar-heading">个人设置</h3>
					<div class="form-group">
						<label class="control-sidebar-subheading"> 左侧菜单自适应
							<input type="checkbox" class="pull-right" checked>
						</label>
						<p>左侧菜单栏样式自适应</p>
					</div>
					<!-- /.form-group -->

				</form>
			</div>
			<!-- /.tab-pane -->
		</div>
	</aside>
	<!-- /.control-sidebar -->
	<!-- Add the sidebar's background. This div must be placed immediately after the control sidebar -->
	<div class="control-sidebar-bg"></div>
</#macro>

<#macro commonFooter >
	<!-- <footer class="main-footer">
        <b></b> &nbsp;  
		<div class="pull-right hidden-xs">
            <strong>Copyright &copy; 2014-${.now?string('yyyy')} &nbsp;
                <a href="http://http://www.yunovo.cn//" target="_blank" >深圳市云智易联科技有限公司</a>
				&nbsp;版权所有
            </strong>
		</div>
	</footer> -->
</#macro>