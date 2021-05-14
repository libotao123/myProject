package cn.yunovo.job.executor.service.jobhandler;

import java.util.ArrayList;
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
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="tags_batch_job_lbt")
@Component

/*
 * 批量为用户打标签
 * 
 * 
 */
public class TagsBatchJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(TagsBatchJobHandler.class);
    
    //cdp
    private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
//    private final String THRID_API_URL = "http://192.168.3.240:7000";
    
    private int tagid = 109;
    private String domain = "wx.yunovo.cn";
    private String appid = "wxfcd75e38666c34eb";
	private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
	
//    private int tagid = 2;
//	private String domain = "t.wx.yunovo.cn";
//    private String appid = "wxf120e49da368b3fd";
//	private String appsecret = "e6e78cd8798604f4129f3f7c5f9e22b5";
	
	private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
	private final String BATCH_TAGGING = "/rest/api/duck/batchtagging?accessToken=%s";
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public RestTemplate restTemplate;
    
 // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
    private int query_id1 = 540;
    
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
            logger.error("[UserInfoJobHandler.batchget][批量为用户打标签异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[UserInfoJobHandler.batchget][批量为用户打标签异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

	//同步用户标签
	public void batchget() throws Exception {
		Integer errcode = 0;
//		List<String> list = getOpenids(domain);
		JSONArray list = null;
    	list = findYundashData(query_id1,yundash_api_key);
		int total = list.size();
		logger.info("[UserInfoJobHandler.batchget],total={}",total);
    	XxlJobLogger.log("[UserInfoJobHandler.batchget],total={0}",total);
		if(total < 1) {
			return;
		}
		
		int count = 0;
		int page = 3;
		List<String> dateList;
		for(int i = 0;i < total;i += page) {
			if(i+page < total) {
				dateList = new ArrayList<>();
				for(int j=i;j<i+page;j++) {
//					dateList.add(list.get(j));
					dateList.add(list.getJSONObject(j).getString("open_id"));
				}
			}else {
				dateList = new ArrayList<>();
				for(int j=i;j<total;j++) {
//					dateList.add(list.get(j));
					dateList.add(list.getJSONObject(j).getString("open_id"));
				}
			}
			
			//批量为用户打标签
			errcode = batchtagging(appid, appsecret,dateList,tagid);
			logger.info("[UserInfoJobHandler.batchget],errcode={},dateListSize={}",errcode,dateList.size());
	    	XxlJobLogger.log("[UserInfoJobHandler.batchget],errcode={0},dateListSize={1}",errcode,dateList.size());
	    	
	    	if(++count >= 2) {
	    		break;
	    	}
		}
		
	}
	
	//查询openid列表
	public List<String> getOpenids(String domain){
		String sql = "SELECT DISTINCT wx.open_id FROM clw.cc_customer_wx wx " + 
				"INNER JOIN clw.cc_device_bind b ON b.wx_id = wx.wx_id AND b.`status` = 1 " + 
				"INNER JOIN clw.cc_device d ON d.device_id = b.device_id " + 
				"INNER JOIN gprs.cc_gprs_card c ON c.card_iccid = d.device_iccid " + 
				"WHERE wx.wx_domain = ? ";
		return jdbcTemplate.queryForList(sql,String.class,domain);
	}
	
	//批量为用户打标签
	public int batchtagging(String appid, String appsecret,List<String> list,int tagid) {
		if(null != list && list.size() > 0) {
			Integer errcode = 0;
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
				jsonObject = JSONObject.parseObject(result);
			}
			
			errcode = jsonObject.getInteger("errcode");
			return errcode;
		}
		return -2;
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
	
	public JSONArray findYundashData(int query_id,String yundash_api_key) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id,yundash_api_key);
		logger.info("[UserInfoUnionIDJobHandler][开始调yundash查询用户]  url={}",yds_query_url);
		XxlJobLogger.log("[UserInfoUnionIDJobHandler][开始调yundash查询用户]  url={0}",yds_query_url);
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
		return yds_result;
	}
	
}
