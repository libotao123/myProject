package cn.yunovo.job.executor.service.jobhandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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

@JobHandler(value="user_info_job_lbt")
@Component

/*
 * 同步unionID
 * 
 * 
 */
public class UserInfoUnionIDJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UserInfoUnionIDJobHandler.class);
	
    // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
    private int query_id1 = 540;
    
    //cdp
    private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
//    private final String THRID_API_URL = "http://192.168.3.240:7000";
    private final String ACCESS_TOKEN_URL = "/rest/api/duck/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
    private String appid = "wxfcd75e38666c34eb";
	private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
//    private String appid = "wxf120e49da368b3fd";
//	private String appsecret = "e6e78cd8798604f4129f3f7c5f9e22b5";
	
	private final String BATCH_GET_USER = "/rest/api/duck/batchget?accessToken=%s";
    
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
            logger.error("[UserInfoUnionIDJobHandler.batchget][更新unionID异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[UserInfoUnionIDJobHandler.batchget][更新unionID异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

	//同步unionID
	public void batchget() throws Exception {
		//1、查询openid列表
		JSONArray yds_result = null;
    	yds_result = findYundashData(query_id1,yundash_api_key);
		/*List<String> list = getOpenids();
		List<Map<String,String>> dataList2 = new ArrayList<>();
		Map<String,String> map = null;
		for(int i=0;i<list.size();i++) {
			map = new HashMap<>();
			map.put("open_id", list.get(i));
			dataList2.add(map);
		}
		yds_result = JSONArray.parseArray(JSON.toJSONString(dataList2));*/
    	logger.info("[UserInfoUnionIDJobHandler.batchget],sum={}",yds_result.size());
    	XxlJobLogger.log("[UserInfoUnionIDJobHandler.batchget],sum={0}",yds_result.size());
    	
		//2、组装数据查询微信方unionID
    	int page = 100;
		int[] ints = null;
		String result = null;
		String user_list = null;
		List<Map<String,String>> dataList = null;
		Map<String,String> tempMap = null;
		for (int i=0; i<yds_result.size(); i+=page) {
			try {
				if(i+page < yds_result.size()) {
					dataList = new ArrayList<>();
					for(int j = i;j < i + page;j++) {
						tempMap = new HashMap<>();
						tempMap.put("openid", yds_result.getJSONObject(j).getString("open_id"));
						tempMap.put("lang", "zh_CN");
						dataList.add(tempMap);
					}
					
				}else {
					dataList = new ArrayList<>();
					for(int j = i;j < yds_result.size();j++) {
						tempMap = new HashMap<>();
						tempMap.put("openid", yds_result.getJSONObject(j).getString("open_id"));
						tempMap.put("lang", "zh_CN");
						dataList.add(tempMap);
					}
				}
				
				user_list = "{\"user_list\":" + JSONObject.toJSONString(dataList) + "}";
				
				//调微信接口批量获取用户信息
				result = getUsers(appid, appsecret, user_list);
				
				//3、更新数据库unionID
				ints = updateUnionID(result);
				
			} catch (Exception e) {
				logger.error("[UserInfoUnionIDJobHandler.batchget][],user_list={},result={},ints={},e={}",user_list,result,ints.length,ExceptionUtils.getStackTrace(e));
				XxlJobLogger.log("[UserInfoUnionIDJobHandler.batchget][],user_list={0},result={1},ints={2},e={3}",user_list,result,ints.length,ExceptionUtils.getStackTrace(e));
			}
		}
		logger.info("[UserInfoUnionIDJobHandler.batchget][],user_list={},result={},ints={}",user_list,result,ints);
		XxlJobLogger.log("[UserInfoUnionIDJobHandler.batchget][],user_list={0},result={1},ints={2}",user_list,result,ints);
    	
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
	
	//更新数据库unionID
	public int[] updateUnionID(String result) throws Exception {
		JSONObject jsonObject = JSONObject.parseObject(result);
		String user_info_list = jsonObject.getString("user_info_list");
		if(StringUtils.isEmpty(user_info_list)) {
			return null;
		}
		
		List<JSONObject> datalist = new ArrayList<>();
		String unionid = null;
		JSONArray jsonArray = JSONArray.parseArray(user_info_list);
		if(jsonArray != null && jsonArray.size() > 0) {
			for(int i = 0;i<jsonArray.size();i++) {
				unionid = jsonArray.getJSONObject(i).getString("unionid");
				if(StringUtils.isNotEmpty(unionid)) {
					datalist.add(jsonArray.getJSONObject(i));
				}
			}
		}
		
		String sql = "UPDATE cc_customer_wx SET union_id = ? WHERE open_id = ?";
		try {
			return jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

				@Override
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					ps.setString(1, datalist.get(i).getString("unionid"));
					ps.setString(2, datalist.get(i).getString("openid"));
				}

				@Override
				public int getBatchSize() {
					return datalist.size();
				}
				
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * 批量获取用户基本信息
	 * @param message
	 * @return
	 */
	public String getUsers(String appid, String appsecret,String user_list) {
		
		String accessToken =  this.getAccessToken(appid, appsecret, "0");
		
		String url = THRID_API_URL+String.format(BATCH_GET_USER,accessToken);
		String result = httpPost(url, user_list);
		JSONObject jsonObject  = JSONObject.parseObject(result);
		
		if(jsonObject != null && StringUtils.equals(jsonObject.getString("errcode"),"40001")) {
			accessToken =  this.getAccessToken(appid, appsecret, "1");
			url = THRID_API_URL+String.format(BATCH_GET_USER,accessToken);
			result = httpPost(url, user_list);
//			jsonObject = JSONObject.parseObject(result);
		}
		return result;
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
			logger.info("[UserInfoUnionIDJobHandler.getAccessToken][ERROR]从网络获取access_token失败,result={},params={}",result,JSONObject.toJSONString(params));
		}else {
			logger.info("[UserInfoUnionIDJobHandler.getAccessToken]从网络获取到access_token成功，access_token={},params={}",json.getString("access_token"),JSONObject.toJSONString(params));
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
	
	public List<String> getOpenids(){
//		String sql = "SELECT DISTINCT open_id FROM cc_customer_wx ";
		String sql = "SELECT DISTINCT open_id FROM cc_customer_wx WHERE open_id != '' OR open_id != 'null' ";
		return jdbcTemplate.queryForList(sql,String.class);
	}
}
