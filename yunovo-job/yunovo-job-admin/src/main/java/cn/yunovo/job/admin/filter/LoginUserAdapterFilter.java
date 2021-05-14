/*
 * COPYRIGHT. ShenZhen spt-tek, Ltd. 2015.
 * ALL RIGHTS RESERVED.
 *
 * No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
 * on any form or by any means, electronic, mechanical, photocopying, recording,
 * or otherwise, without the prior written permission of ShenZhen spt-tek, Ltd.
 *
 * Amendment History:
 *
 * Date By Description
 * ------------------- -----------
-------------------------------------------
 * 2015年11月16日 YangZhengsi Create the class
 */

package cn.yunovo.job.admin.filter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.sunshine.dcda.basecomponent.bean.BeanUtil;
import org.sunshine.dcda.basecomponent.casclient.common.SessionUtil;
import org.sunshine.dcda.basecomponent.casclient.common.SystemUserVo;
import org.sunshine.dcda.system.service.model.SystemResourceVo;
import org.sunshine.dcda.view.system.viewcomponent.ISystemResourceViewComponent;
import org.sunshine.dcda.view.system.viewcomponent.ISystemUserViewComponent;

import cn.yunovo.job.admin.config.SpringCasAutoconfig;
import cn.yunovo.job.admin.constants.JobConstants;
import cn.yunovo.job.admin.core.util.FtlUtil;
import cn.yunovo.job.admin.core.util.I18nUtil;

@WebFilter(urlPatterns = { "/secure/*" }, filterName = "LoginUserAdapterFilter")
public class LoginUserAdapterFilter implements javax.servlet.Filter {

	private static final Logger logger = LoggerFactory.getLogger(LoginUserAdapterFilter.class);
	private final static String LOGIN_NAME = "loginName";
	private final static String LOGIN_PASSWORD = "loginPassword";

	@Autowired
	SpringCasAutoconfig autoconfig;
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		try {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			HttpServletResponse httpResponse = (HttpServletResponse) response;
			// 获取Cas Session中的Assertion
			Assertion object = (Assertion) httpRequest.getSession().getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
			Map<String, Object> map = object.getPrincipal().getAttributes();
			// 获取cas服务端的登录名称
			String loginName = (String) map.get(LOGIN_NAME);
			// 获取cas服务端的登录密码
			String loginPassword = (String) map.get(LOGIN_PASSWORD);
			logger.info("当前登陆系统：用户中心。登录系统的用户名为：" + loginName);
			
			// 获取session的用户
			SystemUserVo systemUserVo = SessionUtil.getLoginUserInfo(httpRequest);
			if (systemUserVo == null) {
				// 获取spring的Bean
				WebApplicationContext wct = WebApplicationContextUtils.getWebApplicationContext(httpRequest.getSession()
						.getServletContext());
				
				ISystemUserViewComponent userViewComponent = (ISystemUserViewComponent) wct.getBean("systemUserViewComponent");
				
				org.sunshine.dcda.system.service.model.SystemUserVo systemUser = null;
				try {
					systemUser = userViewComponent.querySystemUser(loginName, loginPassword);
				} catch (Exception e) {  
					sendRedirect(httpResponse);
					return;
				}
				SessionUtil.setLoginUserInfo(httpRequest, BeanUtil.deepCopy(systemUser, SystemUserVo.class));
			}
			
			// 获取session的用户权限（button级）
			boolean ava = SessionUtil.sessionObjExist(httpRequest, "userButtons");
			if (!ava) {
				
				WebApplicationContext wct = WebApplicationContextUtils.getWebApplicationContext(httpRequest.getSession()
						.getServletContext());
				
				ISystemResourceViewComponent systemResourceViewComponent = (ISystemResourceViewComponent) wct.getBean("systemResourceViewComponent");
				List<SystemResourceVo> userButtons = null;
				try {
					userButtons = systemResourceViewComponent.queryButtonResources(SessionUtil.getLoginUserInfo(httpRequest).getId(), JobConstants.SITECODE);
				} catch (Exception e) {
					sendRedirect(httpResponse);
					return;
				}
				SessionUtil.setDataToSession(httpRequest, "userButtons", userButtons);
			}
			
			/*每次加载多语言*/
			request.setAttribute("I18nUtil",FtlUtil.generateStaticModel(I18nUtil.class.getName()));
			SystemUserVo userVo = SessionUtil.getLoginUserInfo(httpRequest);
			request.setAttribute("systemUser", userVo);
			chain.doFilter(request, response);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 错误跳转
	 * 
	 * @Description
	 * @param httpResponse
	 * @param url
	 *            跳转的URL
	 * @throws IOException
	 */
	private void sendRedirect(HttpServletResponse httpResponse) throws IOException {
		// 跳转到重新登录页面
		httpResponse.sendRedirect(autoconfig.getCasServerLogoutUrl());
	}
	
	@Override
	public void init(FilterConfig arg0) {
	}
}
