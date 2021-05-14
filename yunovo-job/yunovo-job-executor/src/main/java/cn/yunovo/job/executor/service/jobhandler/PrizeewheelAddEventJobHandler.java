package cn.yunovo.job.executor.service.jobhandler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="prizeewheel_event_job_lbt")
@Component

/*
 * 初始化转盘抽奖用户名单
 * 
 * 
 */
public class PrizeewheelAddEventJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(PrizeewheelAddEventJobHandler.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private int query_id = 329;
    
    // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    
    private String yundash_api_key = "5G2lSabdbnxOCmI4bcFCo96JXZPFp5A8pgFPHrkZ";
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
    	
		JSONArray yds_result = null;
		
		try {
			yds_result = findYundashData(query_id);
			deletePrizeewheelInfo();
			insertPrizeewheelInfo(yds_result);
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            XxlJobLogger.log("[PrizeewheelAddEventJobHandler.insertPrizeewheelInfo][新增数据异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		
		return a;
    }

	public JSONArray findYundashData(int query_id) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id,yundash_api_key);
//		logger.info("[PrizeewheelAddEventJobHandler][开始调yundash查询用户]  url={}",yds_query_url);
		XxlJobLogger.log("[PrizeewheelAddEventJobHandler.findYundashData][开始调yundash查询用户]  url={0}",yds_query_url);
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
//		logger.info("[PrizeewheelAddEventJobHandler][查yundash获取的用户]用户总数={}",yds_result.size());
		XxlJobLogger.log("[PrizeewheelAddEventJobHandler.findYundashData][查yundash获取的用户]用户总数={0}",yds_result.size());
		return yds_result;
	}
	
	public void insertPrizeewheelInfo(JSONArray yds_result) throws Exception {
		String sql = "INSERT INTO cc_wx_activity_prizeewheel_info (open_id,wx_id,wx_domain,customer_id,iccid,create_datetime,init_chance) VALUES (?,?,?,?,?,NOW(),1)";
		String querySql = "SELECT COUNT(1) FROM cc_wx_activity_prizeewheel_info WHERE open_id = ?";
		int result = 0;
		for (int i=0; i<yds_result.size(); i++) {
			JSONObject by_wx = yds_result.getJSONObject(i);
			String open_id = by_wx.getString("open_id");
			String wx_id = by_wx.getString("wx_id");
			String wx_domain = by_wx.getString("wx_domain");
			String customer_id = by_wx.getString("customer_id");
			String card_iccid = by_wx.getString("card_iccid");
			try {
				result += jdbcTemplate.update(sql,open_id,wx_id,wx_domain,customer_id,card_iccid);
			} catch (Exception e) {
//	            logger.error("[PrizeewheelAddEventJobHandler.insertPrizeewheelInfo][新增数据异常]open_id={},wx_id={},wx_domain={},customer_id={},iccid={},create_by={},e={}",open_id,wx_id,wx_domain,customer_id,card_iccid,e);
	            XxlJobLogger.log("[PrizeewheelAddEventJobHandler.insertPrizeewheelInfo][新增数据异常]open_id={0},wx_id={1},wx_domain={2},customer_id={3},iccid={4},create_by={5},e={6}",open_id,wx_id,wx_domain,customer_id,card_iccid,ExceptionUtils.getStackTrace(e));
			}
		}
		XxlJobLogger.log("[PrizeewheelAddEventJobHandler.insertPrizeewheelInfo][成功插入初始化数据]result={0}",result);
	}
	
	//清除之前的数据导入新数据
	public void deletePrizeewheelInfo() throws Exception {
		String sql = "DELETE FROM clw.cc_wx_activity_prizeewheel_info";
		try {
			jdbcTemplate.update(sql);
          	XxlJobLogger.log("[PrizeewheelAddEventJobHandler.deletePrizeewheelInfo][删除数据成功],sql={0}",sql);
		} catch (DataAccessException e) {
//            logger.error("[PrizeewheelAddEventJobHandler.deletePrizeewheelInfo][删除数据异常],sql={},e={}",sql,ExceptionUtils.getStackTrace(e));
          	XxlJobLogger.log("[PrizeewheelAddEventJobHandler.deletePrizeewheelInfo][删除数据异常],sql={0},e={1}",sql,ExceptionUtils.getStackTrace(e));
		}
	}
}

