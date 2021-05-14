package cn.yunovo.job.admin.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.sunshine.dcda.basecomponent.casclient.common.SessionUtil;
import org.sunshine.dcda.basecomponent.casclient.common.SystemUserVo;
import org.sunshine.dcda.system.service.model.SystemResourceVo;
import org.sunshine.dcda.view.system.viewcomponent.ISystemResourceViewComponent;

import cn.yunovo.job.admin.config.SpringCasAutoconfig;
import cn.yunovo.job.admin.constants.JobConstants;

@Controller
@RequestMapping("/index")
public class IndexControll {
	
	@Autowired
	SpringCasAutoconfig autoconfig;
	private static final Logger logger = LoggerFactory.getLogger(IndexControll.class);
	
	@Resource
	private ISystemResourceViewComponent systemResourceViewComponent;
	
	/*@RequestMapping("/homepage")
	public ModelAndView index(HttpServletRequest request, ModelMap model) throws Exception{
		SystemUserVo userVo = SessionUtil.getLoginUserInfo(request);
		logger.info(userVo.toString() + ": 登录平台");
		
		List<SystemResourceVo> userResources = systemResourceViewComponent.queryMenuResources(userVo.getId(), JobConstants.SITECODE);
		model.put("userResources", userResources);
		model.put("systemUser", userVo);
		return new ModelAndView("index");
	}
	
	@RequestMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Exception {
		SystemUserVo userVo = SessionUtil.getLoginUserInfo(request);
		logger.info(userVo.toString() + ": 登出平台");
		try {
			SessionUtil.removeSessionLoginUser(request);
			SessionUtil.removeSessionAttribut(request, "userButtons");
			request.getSession().invalidate();
			response.sendRedirect(CASConstants.getParamValue(CASConstants.CAS_SERVER_LOGOUT_URL_KEY));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@RequestMapping("/toUserCenter")
	public String toUserCenter(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Exception {
		try {
			response.sendRedirect(CASConstants.getParamValue(CASConstants.CAS_SERVER_LOGIN_URL_KEY));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}*/
	
	@RequestMapping("/homepage")
	public ModelAndView index(HttpServletRequest request, ModelMap model) throws Exception{
		SystemUserVo userVo = SessionUtil.getLoginUserInfo(request);
		logger.info(userVo.toString() + ": 登录平台");
		
		List<SystemResourceVo> userResources = systemResourceViewComponent.queryMenuResources(userVo.getId(), JobConstants.SITECODE);
		model.put("userResources", userResources);
		model.put("systemUser", userVo);
		return new ModelAndView("index");
	}
	
	@RequestMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Exception {
		SystemUserVo userVo = SessionUtil.getLoginUserInfo(request);
		logger.info(userVo.toString() + ": 登出平台");
		try {
			SessionUtil.removeSessionLoginUser(request);
			SessionUtil.removeSessionAttribut(request, "userButtons");
			request.getSession().invalidate();
			response.sendRedirect(autoconfig.getCasServerLogoutUrl());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@RequestMapping("/toUserCenter")
	public String toUserCenter(HttpServletRequest request, HttpServletResponse response, ModelMap model) throws Exception {
		try {
			response.sendRedirect(autoconfig.getCasServerLoginUrl());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	@RequestMapping("/welcome")
	public ModelAndView welcome(HttpServletRequest request, ModelMap model) throws Exception{
		SystemUserVo userVo = SessionUtil.getLoginUserInfo(request);
		model.put("systemUser", userVo);
		return new ModelAndView("module/welcome");
	}
}
