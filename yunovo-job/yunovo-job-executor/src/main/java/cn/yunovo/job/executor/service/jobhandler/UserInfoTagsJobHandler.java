package cn.yunovo.job.executor.service.jobhandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="user_tags_job_lbt")
@Component

/*
 * 同步用户标签
 * 
 * 
 */
public class UserInfoTagsJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UserInfoTagsJobHandler.class);
    
    //cdp
    private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
//    private final String THRID_API_URL = "http://192.168.3.240:7000";
    private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
    private String appid = "wxfcd75e38666c34eb";
	private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
    private String domain = "wx.yunovo.cn";
//    private String appid = "wxf120e49da368b3fd";
//	private String appsecret = "e6e78cd8798604f4129f3f7c5f9e22b5";
//	private String domain = "t.wx.yunovo.cn";
	
	private final String BATCH_TAGGING = "/rest/api/duck/batchtagging?accessToken=%s";
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			batchget();
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[UserInfoJobHandler.batchget][更新unionID异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[UserInfoJobHandler.batchget][更新unionID异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

	//同步用户标签
	public void batchget() throws Exception {
		String result = null;
		Integer count = 0;
		List<String> list = new ArrayList<>();
		String s = "oXEDlt1cCn3eSZFnTQxTwQiJttnk,oXEDlt_90VklHwLym1J84ihiB_Og,oXEDltzOqtd0iOq90JQHOqCpeZlM,oXEDltzzwwu2_QRYrejv9K2fnfkM";
		list = Arrays.asList(s.split(","));
/*		//1、同步威仕特用户标签，机构号：OG-000149，tagid：100
		//1.1、查询威仕特用户列表
		list = getOpenids(domain,"OG-000149");
		//1.2、为威仕特用户打标签
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,100);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000149",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000149",result);
		}
		//2、同步永盛杰用户标签，机构号：OG-000152，tagid：101
		list = getOpenids(domain,"OG-000152");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,101);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000152",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000152",result);
		}
		//3、同步易图用户标签，机构号：OG-000158，tagid：103
		list = getOpenids(domain,"OG-000158");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,103);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000158",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000158",result);
		}
		//4、同步艾酷用户标签，机构号：OG-000159，tagid：104
		list = getOpenids(domain,"OG-000159");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,104);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000159",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000159",result);
		}
		//5、同步美伴天机用户标签，机构号：OG-000103，tagid：105
		list = getOpenids(domain,"OG-000103");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,105);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000103",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000103",result);
		}
		//6、同步美伴天机用户标签，机构号：OG-000166，tagid：105
		list = getOpenids(domain,"OG-000166");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,105);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000166",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000166",result);
		}
		//7、同步奇橙天下用户标签，机构号：OG-000125，tagid：106
		list = getOpenids(domain,"OG-000125");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,106);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000125",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000125",result);
		}
		//8、同步奇橙天下用户标签，机构号：OG-000170，tagid：106
		list = getOpenids(domain,"OG-000170");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging(appid, appsecret,list,106);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000170",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1}","OG-000170",result);
		}*/
		//9、同步车萝卜用户标签，机构号：OG-000164，tagid：107
//		list = getOpenids(domain,"OG-000164","A12","A11");
//		list = getOpenid(domain,"OG-000164","A11");
		if(list != null && list.size() > 0) {
			count = 0;
			count = batchtagging2(appid, appsecret,list,103);
			logger.info("[UserInfoJobHandler.batchget],organCode={},count={}","OG-000164",count);
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],organCode={0},result={1},listCount={}","OG-000164",count,list.size());
		}
	}
	
	//根据机构号查询openid列表
	public List<String> getOpenids(String domain,String organCode,String proName1,String proName2){
		String sql = "SELECT DISTINCT wx.open_id FROM cc_device_product_model m " + 
				"INNER JOIN cc_device_last_location l ON l.device_sn = m.device_sn " + 
				"INNER JOIN cc_device d ON d.device_sn = m.device_sn " + 
				"INNER JOIN cc_device_bind b on b.device_id = d.device_id AND b.status = 1 AND b.wx_id is not null " + 
				"INNER JOIN cc_customer_wx wx on wx.wx_id = b.wx_id " + 
				"WHERE substr(d.device_sn,-1) != '_' AND m.pro_name != '' " + 
				"AND l.city in('深圳市','上海市') " + 
				"AND wx.wx_domain = ? " + 
				"AND d.organ_code = ? " +
				"AND (m.pro_name = ? or m.pro_name = ?)";
		return jdbcTemplate.queryForList(sql,String.class,domain,organCode,proName1,proName2);
	}
	
	//根据机构号查询openid列表
	public List<String> getOpenid(String domain,String organCode,String proName1){
		String sql = "SELECT DISTINCT wx.open_id FROM cc_device_product_model m " + 
				"INNER JOIN cc_device_last_location l ON l.device_sn = m.device_sn " + 
				"INNER JOIN cc_device d ON d.device_sn = m.device_sn " + 
				"INNER JOIN cc_device_bind b on b.device_id = d.device_id AND b.status = 1 AND b.wx_id is not null " + 
				"INNER JOIN cc_customer_wx wx on wx.wx_id = b.wx_id " + 
				"WHERE substr(d.device_sn,-1) != '_' AND m.pro_name != '' " + 
				"AND l.city in('深圳市','上海市') " + 
				"AND wx.wx_domain = ? " + 
				"AND d.organ_code = ? " +
				"AND m.pro_name = ? ";
		return jdbcTemplate.queryForList(sql,String.class,domain,organCode,proName1);
	}

	//批量为用户打标签
	public int batchtagging2(String appid, String appsecret,List<String> list,int tagid) {
		if(null != list && list.size() > 0) {
			Integer count = 0;
			String accessToken =  this.getAccessToken(appid, appsecret, "0");
			String tags = null;
			String openid = null;
			for(int i=0;i<list.size();i++) {
				openid = list.get(i);
				tags = "{\"openid_list\":\"" + openid + "\",\"tagid\":"+ tagid +"}";
				
				String url = THRID_API_URL+String.format(BATCH_TAGGING,accessToken);
				String result = httpPost(url, tags);
				JSONObject jsonObject  = JSONObject.parseObject(result);
				
				if(jsonObject != null && StringUtils.equals(jsonObject.getString("errcode"),"40001")) {
					accessToken =  this.getAccessToken(appid, appsecret, "1");
					url = THRID_API_URL+String.format(BATCH_TAGGING,accessToken);
					result = httpPost(url, tags);
//					jsonObject = JSONObject.parseObject(result);
				}
				
				if(jsonObject.getInteger("errcode") == 0) {
					count++;
				}
			}
			return count;
		}
		return 0;
	}
	//批量为用户打标签
	public int batchtagging(String appid, String appsecret,List<String> list,int tagid) {
		if(null != list && list.size() > 0) {
			Integer count = 0;
			String accessToken =  this.getAccessToken(appid, appsecret, "0");
			String tags = null;
			tags = "{\"openid_list\":" + JSON.toJSONString(list) + ",\"tagid\":"+ tagid +"}";
			
			String url = THRID_API_URL+String.format(BATCH_TAGGING,accessToken);
			XxlJobLogger.log("[UserInfoJobHandler.batchget],url={0},tags={1}",url,tags);
			String result = httpPost(url, tags);
			JSONObject jsonObject  = JSONObject.parseObject(result);
			
			if(jsonObject != null && StringUtils.equals(jsonObject.getString("errcode"),"40001")) {
				accessToken =  this.getAccessToken(appid, appsecret, "1");
				url = THRID_API_URL+String.format(BATCH_TAGGING,accessToken);
				result = httpPost(url, tags);
//				jsonObject = JSONObject.parseObject(result);
			}
			
			if(jsonObject.getInteger("errcode") == 0) {
				count++;
			}
			return count;
		}
		return 0;
	}
	
	/**
	 * 获取微信访问token
	 * @return 
	 */
	public String getAccessToken(String appid, String appsecret, String cleanCache) {
		Map<String,String> params = new HashMap<>();
		params.put("appid", appid);
		params.put("secret", appsecret);
		params.put("cleanCache", cleanCache);
		String  result = httpGet(THRID_API_URL + ACCESS_TOKEN_URL,params);
		JSONObject json = JSONObject.parseObject(result);
		if(!json.containsKey("access_token") || "".equals(json.getString("access_token"))) {
			logger.info("[UserInfoJobHandler.getAccessToken][ERROR]从网络获取access_token失败,result={},params={}",result,JSONObject.toJSONString(params));
		}else {
			logger.info("[UserInfoJobHandler.getAccessToken]从网络获取到access_token成功，access_token={},params={}",json.getString("access_token"),JSONObject.toJSONString(params));
		}
		return json.getString("access_token");
	}
	
	public String httpGet(String url,Map<String,String> param){
		return restTemplate.getForObject(url, String.class, param);
	}
	public String httpPost(String url,String params){
		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());

		HttpEntity<String> formEntity = new HttpEntity<String>(params,headers);
		return restTemplate.postForObject(url, formEntity, String.class);
	}
	
}
