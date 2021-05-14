package cn.yunovo.job.executor.service.jobhandler;

import java.util.HashMap;
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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

/*
 * 定时推送模板消息27_0930
 * 
 * 
 */
public class SendActivityStartMsgJobHandler27_0930 extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(SendActivityStartMsgJobHandler27_0930.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    
    private int query_id1 = 330;
    private int query_id2 = 331;
    private int query_id3 = 332;
    
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
 
    private String appid = "wxfcd75e38666c34eb";
	private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
	
  	private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
    //获取accessToken api url
  	private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
  	//发送模板消息 api url
  	private final String SEND_TEMPLATE_MESSAGE = "/rest/api/wechat/sendTemplateMessage?accessToken=%s";
  	
  	//跳转活动URL
  	private final String HOST = "http://"+"wx.yunovo.cn";
  	private final String WX_LINK_URL = "/wechat/view/flow/midyearactivity.html?activityTag=";
  	
  	
  	//服务开启通知模板id
  	private final String TEMPLATE_ID_SERVICE_START = "PcvDFsLCVNaB46ZJvUc3hxx5_8kV65gTgnZ4nGj6o98";//线上id
//  	private final String TEMPLATE_ID_SERVICE_START = "9AFKJQD4n5U3ZwLJ_m9mwqDg2m70Aekx6zZBLPhvS2k";//test
  	
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			sendMsgHandler();
		} catch (Exception e1) {
            a.setCode(500);
            a.setMsg("发送模板消息异常");
//            logger.error("[SendTemplateMessageJobHandler.sendMsgHandler][发送模板消息异常],e={}",e1);
            XxlJobLogger.log("[SendTemplateMessageJobHandler.sendMsgHandler][新增数据异常],e={0}",ExceptionUtils.getStackTrace(e1));
            return a;
		}
		
		a.setCode(200);
        a.setMsg("发送模板消息成功");
		return a;
    }

    public void sendMsgHandler() throws Exception {
    	JSONArray yds_result = null;
    	int result = 0;
    	//给A类用户发消息
    	yds_result = findYundashData(query_id1,yundash_api_key);
    	XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][A类用户总数],sumA={0}",yds_result.size());
    	try {
			for (int i=0; i<yds_result.size(); i++) {
				JSONObject by_wx = yds_result.getJSONObject(i);
				String open_id = by_wx.getString("open_id");
				try {
					result += sendTemplateMessage(open_id,appid, appsecret, toTemMesJson1(open_id,HOST+WX_LINK_URL+"userA"));
				} catch (Exception e) {
					XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][A类用户发送模板消息失败],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
				}
			}
			XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][A类用户发送成功数],successA={0}",result);
		} catch (Exception e) {
			XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][A类用户发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
    	
    	//给B类用户发消息
    	result = 0;
    	yds_result = findYundashData(query_id2,yundash_api_key);
    	XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][B类用户总数],sumB={0}",yds_result.size());
    	try {
			for (int i=0; i<yds_result.size(); i++) {
				JSONObject by_wx = yds_result.getJSONObject(i);
				String open_id = by_wx.getString("open_id");
				try {
					result += sendTemplateMessage(open_id,appid, appsecret, toTemMesJson2(open_id,HOST+WX_LINK_URL+"userB"));
				} catch (Exception e) {
					XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][B类用户发送模板消息失败],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
				}
			}
			XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][B类用户发送成功数],successB={0}",result);
		} catch (Exception e) {
            XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][B类用户发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
    	
    	//给C类用户发消息
    	result = 0;
    	yds_result = findYundashData(query_id3,yundash_api_key);
    	XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][C类用户总数],sumC={0}",yds_result.size());
    	try {
			for (int i=0; i<yds_result.size(); i++) {
				JSONObject by_wx = yds_result.getJSONObject(i);
				String open_id = by_wx.getString("open_id");
				try {
					result += sendTemplateMessage(open_id,appid, appsecret, toTemMesJson3(open_id,HOST+WX_LINK_URL+"userC"));
				} catch (Exception e) {
					XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][C类用户发送模板消息失败],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
				}
			}
			XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][C类用户发送成功数],successC={0}",result);
		} catch (Exception e) {
            XxlJobLogger.log("[SendActivityStartMsgJobHandler27_0930.sendMsgHandler][C类用户发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
    }
    
	public JSONArray findYundashData(int query_id,String yundash_api_key) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id,yundash_api_key);
//		logger.info("[SendTemplateMessageJobHandler][开始调yundash查询用户]  url={}",yds_query_url);
		XxlJobLogger.log("[SendTemplateMessageJobHandler][开始调yundash查询用户]  url={0}",yds_query_url);
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
		return yds_result;
	}
	

	/**
	 * 发送模板消息
	 * @param message
	 * @return
	 */
	public int sendTemplateMessage(String open_id,String appid, String appsecret,String message) {
		
		String accessToken =  this.getAccessToken(appid, appsecret, "0");
		
		String url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,accessToken);
		String result = httpPost(url, message);
		JSONObject jsonObject  = JSONObject.parseObject(result);
		
		if(jsonObject != null && StringUtils.equals(jsonObject.getString("errcode"),"40001")) {
			accessToken =  this.getAccessToken(appid, appsecret, "1");
			url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,accessToken);
			result = httpPost(url, message);
			jsonObject = JSONObject.parseObject(result);
		}
		
		if(StringUtils.equals(jsonObject.getString("errcode"),"0")){
			logger.info("[SendActivityStartMsgJobHandler27_0930.sendTemplateMessage][发送微信模板消息成功]open_id={},result={}",open_id,result);
			return 1;
		}else {
			logger.info("[SendActivityStartMsgJobHandler27_0930.sendTemplateMessage][发送微信模板消息失败]open_id={},result={},message={},url={}",open_id,result,message,url);
			return 0;
		}
		
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
			logger.info("[SendActivityStartMsgJobHandler27_0930.getAccessToken][ERROR]从网络获取access_token失败,result={},params={}",result,JSONObject.toJSONString(params));
		}else {
			logger.info("[SendActivityStartMsgJobHandler27_0930.getAccessToken]从网络获取到access_token成功，access_token={},params={}",json.getString("access_token"),JSONObject.toJSONString(params));
		}
		return json.getString("access_token");
	}
	
	public String httpGet(String url,Map<String,String> param){
		return restTemplate.getForObject(url, String.class, param);
	}
	
	public String httpPost(String url,String param){
		HttpHeaders headers = new HttpHeaders();
		MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
		headers.setContentType(type);
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());

		HttpEntity<String> formEntity = new HttpEntity<String>(param, headers);
		return restTemplate.postForObject(url, formEntity, String.class);
	}
	//A类用户模板消息组装
	public String toTemMesJson1 (String openid, String url) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id",TEMPLATE_ID_SERVICE_START);
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("小达三周年，感谢您的使用！\n\n#周年聚惠#盛大开启！现在充值套餐即返最高88元现金红包，更有机会获得免单、油卡……\n","#FF2400"));
		data.put("keyword1", toVC_JsonObj("3周年庆典！周年聚惠趴，好礼2连发！","#000001"));
		data.put("keyword2", toVC_JsonObj("2019.7.27-2019.7.29","#000001"));
		data.put("remark", toVC_JsonObj("\n\n限量2019人，手慢无！戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	//B类用户模板消息组装
	public String toTemMesJson2 (String openid, String url) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id",TEMPLATE_ID_SERVICE_START);
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("小达三周年，感谢您的使用！\n\n#周年聚惠#盛大开启！现在充值套餐即返最高88元现金红包，网络音乐时时听，在线导航随处享，更有机会获得免单、油卡……\n","#FF2400"));
		data.put("keyword1", toVC_JsonObj("3周年庆典！周年聚惠趴，好礼2连发！","#000001"));
		data.put("keyword2", toVC_JsonObj("2019.7.27-2019.7.29","#000001"));
		data.put("remark", toVC_JsonObj("\n\n限量2019人，手慢无！戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	//C类用户模板消息组装
	public String toTemMesJson3 (String openid, String url) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id",TEMPLATE_ID_SERVICE_START);
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("小达三周年，感谢您的使用！\n\n#周年聚惠#盛大开启！幸运转盘，转出好运，免单、油卡免费抽，充值套餐更返最高88元现金红包！\n","#FF2400"));
		data.put("keyword1", toVC_JsonObj("3周年庆典！周年聚惠趴，好礼2连发！","#000001"));
		data.put("keyword2", toVC_JsonObj("2019.7.27-2019.7.29","#000001"));
		data.put("remark", toVC_JsonObj("\n\n限量2019人，手慢无！戳我转出好运☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public JSONObject toVC_JsonObj(String value,String color){
		JSONObject json = new JSONObject();
		json.put("value", value);
		json.put("color", color);
		return json;
	}
}
