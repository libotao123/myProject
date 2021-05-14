package cn.yunovo.job.executor.service.jobhandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="ding_job_lbt")
@Component

/*
 * 发碰撞消息到钉钉
 * 
 * 
 */
public class DINGJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(DINGJobHandler.class);
	
	//cdp
	//private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
    private final String THRID_API_URL = "http://192.168.3.240:7000";
    
    //语音通知
    private final String VOICE_NOTIFY_SMS = "/rest/api/yzx/voiceNotifySms?phone={phone}&templateId={templateId}&variable={variable}";
    //private String PHONE = "13425118998";
    private String PHONE = "13714808421";
    private String TEMPLATE_ID = "TP18040816";
    private String VARIABLE = "code:123456";
    private String VOICE_SWITCH = "false";
    
    //钉钉消息
    private final String SEND_DING_URL = "/rest/api/ding/sendDing?title={title}&dingUrl={dingUrl}&text={text}";
	private final String DING_URL = "https://oapi.dingtalk.com/robot/send?access_token=d957b0c087fd44d6c0c0f4534062065637f4f9efc1d10414a2bd2b7c50221d56";
    //测试url
	//private final String DING_URL = "https://oapi.dingtalk.com/robot/send?access_token=c83ea190a5d5e58e8e60f91b52de5f29c4b43611d8b4f215de2173e67b3e9aed";
    
    //碰撞详情链接
    private final String GSENSOR_DETAIL_DOMAIN = "http://t.wx.yunovo.cn/easyWork/mobile/sop/sopDetail?eventid=%s";
	//private final String GSENSOR_DETAIL_DOMAIN = "https://m.yunovo.cn/easyWork/mobile/sop/sopDetail?eventid=%s";
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			main();
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[execute][main方法异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[execute][main方法异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

	//
	public void main() throws Exception {
		//1、查询
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		String now_time = sdf.format(cal.getTime());
		cal.add(Calendar.MINUTE, -5);//减少5分钟
		String five_min_ago = sdf.format(cal.getTime());
		cal.add(Calendar.HOUR,-2);
		String two_hour_ago = sdf.format(cal.getTime());
		
		//查询未响应且未发无响应消息的工单
    	List<Map<String,Object>> ids_list = queryRecordIds(two_hour_ago,five_min_ago);
		//查询5分钟前用户未查阅的碰撞记录
    	List<Map<String,Object>> un_read_list = queryUnreadRecords(five_min_ago);
    	logger.info("[main][未查阅的碰撞记录number],un_read_list_size={}",un_read_list.size());
    	XxlJobLogger.log("[main][未查阅的碰撞记录number],un_read_list_size={0}",un_read_list.size());
    	
    	String autocar_name = null;
    	String autocar_tag = null;
    	String gsensor_event_id = null;
    	Integer gsensor_level = null;
    	String location = null;
    	String gsensor_datetime = null;
    	String create_datetime = null;
    	Long differMinutes = 0L;
    	Map<String,Object> map = null;
    	String soft_version = null;
    	String operating_mode = null;
    	
    	Integer count = 0;
    	Integer success_num = 0;
    	Integer success_num_voice = 0;
    	String title = null;
    	String text = null;
		Date date = null;
		String record_ids = "";
		String redirectUrl = null;
		//2、护行日志未查阅发消息
    	for(int i=0;i<un_read_list.size();i++) {
    		map = un_read_list.get(i);
    		if(StringUtils.isEmpty(record_ids)) {
    			record_ids = map.get("id").toString();
    		}else {
    			record_ids = record_ids + "," + map.get("id").toString();
    		}
    		autocar_name = map.get("firstname").toString();
    		autocar_tag = map.get("autocar_tag").toString();
    		gsensor_event_id = map.get("gsensor_event_id").toString();
    		gsensor_level = Integer.valueOf(map.get("gsensor_level").toString());
    		location = map.get("location").toString();
    		gsensor_datetime = map.get("gsensor_datetime").toString();
    		create_datetime = map.get("create_datetime").toString();
    		
    		try {
    			date = sdf.parse(gsensor_datetime);
    			gsensor_datetime = sdf.format(date);
			} catch (Exception e) {
				XxlJobLogger.log("[无查阅发钉钉消息][时间转换异常],e={0}",ExceptionUtils.getStackTrace(e));
			}
    		differMinutes = getDifferMinutes(create_datetime, now_time);
    		
    		//组装模板
    		if(gsensor_level == 1) {
    			title = "轻度碰撞";
    		}else if(gsensor_level == 2) {
    			title = "中度碰撞";
    		}else if(gsensor_level == 3) {
    			title = "重度碰撞";
    		}else {
    			title = "其它碰撞";
    		}
    		
    		if(map.get("operating_mode") == null) {
    			operating_mode = "未知";
    		}else if("1".equals(map.get("operating_mode").toString())) {
    			operating_mode = "生产";
    		}else if("2".equals(map.get("operating_mode").toString())){
    			operating_mode = "演示";
    		}else {
    			operating_mode = "未知";
    		}
    		soft_version = map.get("soft_version") == null ? "" : map.get("soft_version").toString();
    		
    		text = "【待处理工单通知】发生一起新的事故，用户%smin内未查阅碰撞消息通知，请及时处理！\n\n工单号：[%s](%s)\n\n作业模式：%s\n\nROM版本：%s\n\n车主名：%s\n\n车牌号：%s\n\n碰撞程度：%s\n\n碰撞时间：%s\n\n碰撞位置：%s";
    		redirectUrl = String.format(GSENSOR_DETAIL_DOMAIN,gsensor_event_id);
    		text = String.format(text, differMinutes,gsensor_event_id,redirectUrl,operating_mode,soft_version,autocar_name,autocar_tag,title,gsensor_datetime,location);
        	
    		//2.1、发送钉钉消息
    		count++;
    		success_num = success_num + sendDing(title, text);
    		
    		//2.2、打钉钉电话
    		try {
    			if("true".equals(VOICE_SWITCH)) {
    				success_num_voice = success_num_voice + voiceNotifySms(PHONE, TEMPLATE_ID, VARIABLE);
    			}else {
    				XxlJobLogger.log("[voiceNotifySms][钉钉电话已关闭],VOICE_SWITCH={0}",VOICE_SWITCH);
    			}
			} catch (Exception e) {
				XxlJobLogger.log("[voiceNotifySms][打钉钉电话异常]e={0}",JSONObject.toJSONString(e));
			}
        	
    	}
    	logger.info("[UserInfoUnionIDJobHandler][未查阅记录发送钉钉消息统计(总数,钉钉消息,钉钉语音)]count={},success_num={},success_num_voice={}",count,success_num,success_num_voice);
		XxlJobLogger.log("[UserInfoUnionIDJobHandler][未查阅记录发送钉钉消息统计(总数,钉钉消息,钉钉语音)]count={0},success_num={1},success_num_voice={2}",count,success_num,success_num_voice);
    	//3、cc_watchmen_ding_notice表里插入数据(存快照)
    	if(null != un_read_list && un_read_list.size() > 0) {
    		save(count, success_num, five_min_ago ,record_ids ,now_time);
    	}
    	
    	//4、发工单无响应消息
    	String ids_string = null;
    	List<Map<String,Object>> un_deal_list = null;
    	String snapshot_create_datetime = null;
    	String gsensor_level_string = null;
    	Integer snapshot_id = null;
    	logger.info("[UserInfoUnionIDJobHandler][发工单无响应消息统计]ids_list_size={}",ids_list.size());
		XxlJobLogger.log("[UserInfoUnionIDJobHandler][发工单无响应消息统计]ids_list_size={0}",ids_list.size());
    	for(int i=0;i<ids_list.size();i++) {
    		map = ids_list.get(i);
    		snapshot_id = Integer.valueOf(map.get("id").toString());
    		ids_string = map.get("record_ids").toString();
    		snapshot_create_datetime = map.get("create_datetime").toString();
    		differMinutes = getDifferMinutes(snapshot_create_datetime, now_time);
    		//查询状态为0（还没处理）的记录
    		un_deal_list = queryUndealRecords(ids_string);
    		logger.info("[查询状态为0（还没处理）的记录结果]ids_string={},un_deal_list_size={}",ids_string,un_deal_list.size());
    		XxlJobLogger.log("[查询状态为0（还没处理）的记录结果]ids_string={0},un_deal_list_size={1}",ids_string,un_deal_list.size());
    		count = 0;
	    	success_num = 0;
	    	success_num_voice = 0;
    		for(int j=0;j<un_deal_list.size();j++) {
    			map = un_deal_list.get(j);
    			autocar_name = map.get("firstname").toString();
    			autocar_tag = map.get("autocar_tag").toString();
        		gsensor_event_id = map.get("gsensor_event_id").toString();
        		gsensor_level = Integer.valueOf(map.get("gsensor_level").toString());
        		location = map.get("location").toString();
        		gsensor_datetime = map.get("gsensor_datetime").toString();
        		
        		try {
        			date = sdf.parse(gsensor_datetime);
        			gsensor_datetime = sdf.format(date);
    			} catch (Exception e) {
    				XxlJobLogger.log("[无接单发钉钉消息][时间转换异常],e={0}",ExceptionUtils.getStackTrace(e));
    			}
        		if(gsensor_level == 1) {
        			gsensor_level_string = "轻度碰撞";
        		}else if(gsensor_level == 2) {
        			gsensor_level_string = "中度碰撞";
        		}
        		else if(gsensor_level == 3) {
        			gsensor_level_string = "重度碰撞";
        		}else {
        			gsensor_level_string = "其它碰撞";
        		}
        		
        		if(map.get("operating_mode") == null) {
        			operating_mode = "未知";
        		}else if("1".equals(map.get("operating_mode").toString())) {
        			operating_mode = "生产";
        		}else if("2".equals(map.get("operating_mode").toString())){
        			operating_mode = "演示";
        		}else {
        			operating_mode = "未知";
        		}
        		soft_version = map.get("soft_version") == null ? "" : map.get("soft_version").toString();
        		
    			//4.1、发钉钉消息
    			title = "工单无应答提醒";
    			text = "【工单无应答提醒】工单%smin内无人响应处理,请负责人及时协调处理！\n\n工单号：[%s](%s)\n\n作业模式：%s\n\nROM版本：%s\n\n车主名：%s\n\n车牌号：%s\n\n碰撞程度：%s\n\n碰撞时间：%s\n\n碰撞位置：%s";
    			redirectUrl = String.format(GSENSOR_DETAIL_DOMAIN,gsensor_event_id);
    			text = String.format(text, differMinutes,gsensor_event_id,redirectUrl,operating_mode,soft_version,autocar_name,autocar_tag,gsensor_level_string,gsensor_datetime,location);
    			count++;
    			success_num = success_num + sendDing(title, text);
    			
    			//4.2、打钉钉电话
    			try {
    				if("true".equals(VOICE_SWITCH)) {
    					success_num_voice = success_num_voice + voiceNotifySms(PHONE, TEMPLATE_ID, VARIABLE);
    				}else {
    					XxlJobLogger.log("[voiceNotifySms][钉钉电话已关闭],VOICE_SWITCH={0}",VOICE_SWITCH);
    				}
    			} catch (Exception e) {
    				XxlJobLogger.log("[voiceNotifySms2][打钉钉电话异常]e={0}",JSONObject.toJSONString(e));
    			}
    		}
    		//5、更新工单无响应确认时间
    		int update_count = updatConfirmDatetime(snapshot_id,now_time);
    		logger.info("[UserInfoUnionIDJobHandler][更新快照结果]snapshot_id={},success_num={},success_num_voice={},update_count={}",snapshot_id,success_num,success_num_voice,update_count);
    		XxlJobLogger.log("[UserInfoUnionIDJobHandler][更新快照结果]snapshot_id={0},success_num={1},success_num_voice={2},update_count={3}",snapshot_id,success_num,success_num_voice,update_count);
    	}
    	
	}
	
	/**
	 * 发送钉钉消息
	 * @param 
	 * @return
	 */
	public int sendDing(String title , String text) {
		Map<String,String> param = new HashMap<>();
		param.put("title", title);
		param.put("text", text);
		param.put("dingUrl", DING_URL);
		
    	String result = null;
    	result = httpGet(THRID_API_URL + SEND_DING_URL,param);
    	JSONObject json = JSONObject.parseObject(result);
    	if(null == result || json.getInteger("code") != 0) {
    		return 0;
    	}else {
    		return 1;
    	}
	}
	
	/**
	 * 发送语音通知
	 * @param 
	 * @return
	 */
	public int voiceNotifySms(String phone , String templateId, String variable) {
		Map<String,String> param = new HashMap<>();
		param.put("phone", phone);
		param.put("templateId", templateId);
		param.put("variable", variable);
		
    	String result = null;
    	result = httpGet(THRID_API_URL + VOICE_NOTIFY_SMS,param);
    	JSONObject json = JSONObject.parseObject(result);
    	if(null == result || json.getInteger("code") != 0) {
    		return 0;
    	}else {
    		return 1;
    	}
	}
	
	//查询日志
	public List<Map<String,Object>> queryUnreadRecords(String end_time) throws Exception {
		List<Map<String,Object>> list = new ArrayList<>();
		String sql = "SELECT r.id,r.firstname,r.autocar_tag,r.gsensor_event_id,r.gsensor_level,r.location,r.gsensor_datetime,r.create_datetime,g.soft_version,g.operating_mode " + 
				"FROM cc_watchmen_gsensor_record r " + 
				"LEFT JOIN cc_gsensor_record g ON g.event_id = r.gsensor_event_id " +
				"WHERE r.`read` = 0 AND (r.gsensor_level = 1 OR r.gsensor_level = 2) " + 
				"AND r.create_datetime > (SELECT MAX(record_end_time) FROM cc_watchmen_ding_snapshot) AND r.create_datetime <= ? ";
		try {
			list = jdbcTemplate.queryForList(sql,end_time);
		} catch (Exception e1) {
			XxlJobLogger.log("[queryUnreadRecords][查询日志异常],e={0}",ExceptionUtils.getStackTrace(e1));
		}
		return list;
	}
	
	//查询record_ids
	public List<Map<String,Object>> queryRecordIds(String two_hour_ago,String five_min_ago) throws Exception {
		List<Map<String,Object>> list = new ArrayList<>();
		String sql = "SELECT id,record_ids,create_datetime FROM cc_watchmen_ding_snapshot " + 
				"WHERE (confirm_datetime IS NULL OR confirm_datetime = '') " + 
				"AND (record_ids IS NOT NULL AND record_ids != '') " +
				"AND create_datetime > ? " + 
				"AND create_datetime <= ? ";
		try {
			list = jdbcTemplate.queryForList(sql,two_hour_ago,five_min_ago);
		} catch (Exception e1) {
			XxlJobLogger.log("[queryRecordIds][查询record_ids异常],e={0}",ExceptionUtils.getStackTrace(e1));
		}
		return list;
	}
	
	//cc_watchmen_gsensor_record.status
	public List<Map<String,Object>> queryUndealRecords(String ids_string) throws Exception {
		List<Map<String,Object>> list = new ArrayList<>();
		logger.info("[queryUndealRecords]ids_string={}",ids_string);
		XxlJobLogger.log("[queryUndealRecords]ids_string={0}",ids_string);
		String sql = "SELECT r.id,r.firstname,r.autocar_tag,r.gsensor_event_id,r.gsensor_level,r.gsensor_datetime,r.location,g.soft_version,g.operating_mode " +
				 "FROM cc_watchmen_gsensor_record r " +
				 "LEFT JOIN cc_gsensor_record g ON g.event_id = r.gsensor_event_id " +
				 "WHERE r.`status` = 0 AND r.id in(" + ids_string + ")";
		logger.info("[queryUndealRecords]sql={}",sql);
		XxlJobLogger.log("[queryUndealRecords]sql={0}",sql);
		try {
			list = jdbcTemplate.queryForList(sql);
		} catch (Exception e1) {
			XxlJobLogger.log("[queryRecordStatus][queryRecordStatus异常],e={0}",ExceptionUtils.getStackTrace(e1));
		}
		return list;
	}
	
	//update cc_watchmen_ding_snapshot.confirm_datetime
	public int updatConfirmDatetime(Integer id,String now_time) throws Exception {
		String sql = "UPDATE cc_watchmen_ding_snapshot SET confirm_datetime = ?,update_datetime = NOW() WHERE id = ? ";
		try {
			return jdbcTemplate.update(sql, now_time, id);
		} catch (Exception e1) {
			XxlJobLogger.log("[updatConfirmDatetime][updatConfirmDatetime异常],now_time={0},e={1}",now_time,ExceptionUtils.getStackTrace(e1));
		}
		return 0;
	}
	
	//保存消息通知快照
	public void save(int count,int success_num,String five_min_ago,String record_ids,String now_time) {
		logger.info("[UserInfoUnionIDJobHandler][保存消息通知快照]count={},success_num={},five_min_ago={},record_ids={}",count,success_num,five_min_ago,record_ids);
		XxlJobLogger.log("[UserInfoUnionIDJobHandler][保存消息通知快照]count={0},success_num={1},five_min_ago={2},record_ids={3}",count,success_num,five_min_ago,record_ids);
		int result = 0;
		String sql = "INSERT INTO cc_watchmen_ding_snapshot (record_ids,count,success_number,record_end_time,create_by,create_datetime) VALUES (?,?,?,?,'job',?)";
		try {
			result = jdbcTemplate.update(sql,record_ids,count,success_num,five_min_ago,now_time);
			if(result == 1) {
				XxlJobLogger.log("[update][快照保存成功],record_ids={0},result={1}",record_ids,result);
			}else {
				XxlJobLogger.log("[update][快照保存失败],sql={0},result={1}",sql,result);
			}
		} catch (Exception e1) {
			XxlJobLogger.log("[update][快照保存异常],sql={0},e={1}",sql,ExceptionUtils.getStackTrace(e1));
		}
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
	

	// 获取两个时间相差分钟数
    public static long getDifferMinutes(String oldTime,String newTime) {
 
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long NTime = 0L;
		//从对象中拿到时间
		long OTime = 0L;
		try {
			NTime = df.parse(newTime).getTime();
			OTime = df.parse(oldTime).getTime();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        long diff=(NTime-OTime)/1000/60;
		return diff;
    }
    
}
