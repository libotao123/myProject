package cn.yunovo.job.executor.service.jobhandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

/*
 * iccid激活时间更新
 * 
 * 
 */
@JobHandler(value="update_iccid_activatedTime_job_lbt")
@Component

public class IccidActiveTimeJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(IccidActiveTimeJobHandler.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    
    private int query_id1 = 477;
    
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
	
//  	private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
  	private final String THRID_API_URL = "http://192.168.3.240:7000";
  	//ICCID查询接口 api url
  	private final String SEND_TEMPLATE_MESSAGE = "/rest/api/sy/iccid/info/queryone?iccid=%s&accountFlag=%s";
  	
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			updateActiveTime();
		} catch (Exception e1) {
            a.setCode(500);
            a.setMsg("iccid激活时间更新异常");
            logger.error("[IccidActiveTimeJobHandler.updateActiveTime][iccid激活时间更新异常],e={}",e1);
            XxlJobLogger.log("[IccidActiveTimeJobHandler.updateActiveTime][iccid激活时间更新异常],e={0}",ExceptionUtils.getStackTrace(e1));
            return a;
		}
		
		a.setCode(200);
        a.setMsg("iccid激活时间更新成功");
		return a;
    }

    public void updateActiveTime() throws Exception {
    	JSONArray yds_result = null;
    	String result = "";
    	int successCount = 0;
    	//查询ICCID信息
    	yds_result = findYundashData(query_id1,yundash_api_key);
    	XxlJobLogger.log("[IccidActiveTimeJobHandler.updateActiveTime][],sum={0}",yds_result.size());
    	try {
			for (int i=0; i<yds_result.size(); i++) {
				JSONObject jsonObject = yds_result.getJSONObject(i);
				String iccid = jsonObject.getString("iccid");
				String card_type = jsonObject.getString("card_type");
				try {
					//查询卡信息
					result = iccidinfoQueryone(iccid,card_type);
					//更新卡激活时间
					successCount += updateIccidActivatedTime(result);
				} catch (Exception e) {
					XxlJobLogger.log("[IccidActiveTimeJobHandler.updateActiveTime][],iccid={0},card_type={1},e={2}",iccid,card_type,ExceptionUtils.getStackTrace(e));
				}
			}
			XxlJobLogger.log("[IccidActiveTimeJobHandler.updateActiveTime][],successCount={0}",successCount);
		} catch (Exception e) {
			XxlJobLogger.log("[IccidActiveTimeJobHandler.updateActiveTime][],e={0}",ExceptionUtils.getStackTrace(e));
		}
    }
 
	/**
	 * 查询卡信息
	 * @param message
	 * @return
	 */
	public String iccidinfoQueryone(String iccid,String card_type) {
		
		String url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,iccid,card_type);
		String result = httpPost(url);
		return result;
	}
	
	public JSONArray findYundashData(int query_id,String yundash_api_key) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id,yundash_api_key);
		logger.info("[IccidActiveTimeJobHandler][开始调yundash查询用户]  url={}",yds_query_url);
		XxlJobLogger.log("[IccidActiveTimeJobHandler][开始调yundash查询用户]  url={0}",yds_query_url);
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
		return yds_result;
	}
	
	public String httpPost(String url){
		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());

		HttpEntity<String> formEntity = new HttpEntity<String>(headers);
		return restTemplate.postForObject(url, formEntity, String.class);
	}
	
	//更新激活时间
	public int updateIccidActivatedTime(String result) throws Exception {
		JSONObject yds_result_json = null;
		String iccidInfo = "";
		String activatedTime = "";
		int sum = 0;
		String sql = "UPDATE cc_other_sim_card_info SET time_active = ?,update_by = 'job',update_datetime = NOW() WHERE iccid = ?";
		try {
			yds_result_json = JSONObject.parseObject(result);
			iccidInfo = yds_result_json.getJSONObject("data").getString("iccid");
			activatedTime = yds_result_json.getJSONObject("data").getString("activatedTime");
			if(StringUtils.isBlank(activatedTime) || "-".equals(activatedTime)) {
				logger.error("[IccidActiveTimeJobHandler.updateIccidActivatedTime][无激活时间],iccidInfo={},activatedTime={}",iccidInfo,activatedTime);
	          	XxlJobLogger.log("[IccidActiveTimeJobHandler.updateIccidActivatedTime][无激活时间],iccidInfo={0},activatedTime={1}",iccidInfo,activatedTime);
				return 0;
			}
			sum = jdbcTemplate.update(sql,activatedTime,iccidInfo);
			if(sum == 0) {
				logger.error("[IccidActiveTimeJobHandler.updateIccidActivatedTime][测试环境不存在该ICCID],iccidInfo={}",iccidInfo);
	          	XxlJobLogger.log("[IccidActiveTimeJobHandler.updateIccidActivatedTime][测试环境不存在该ICCID],iccidInfo={0}",iccidInfo);
			}
          	return 1;
		} catch (DataAccessException e) {
            logger.error("[IccidActiveTimeJobHandler.updateIccidActivatedTime][更新数据异常],sql={},iccidInfo={},activatedTime={},e={}",sql,iccidInfo,activatedTime,ExceptionUtils.getStackTrace(e));
          	XxlJobLogger.log("[IccidActiveTimeJobHandler.updateIccidActivatedTime][更新数据异常],sql={0},iccidInfo={},activatedTime={2},e={3}",sql,iccidInfo,activatedTime,ExceptionUtils.getStackTrace(e));
          	return 0;
		}
	}
}
