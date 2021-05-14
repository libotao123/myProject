package cn.yunovo.job.executor.service.jobhandler;

import java.text.SimpleDateFormat;
import java.util.Date;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;


@JobHandler(value="send_msg_by_ysj_clb_12_12")
@Component
/*
 * 指定YSJAndCLB推送微信模板消息12_12
 * 
 * 
 */
public class SendMsgByYSJAndCLBJobHandler12_12 extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(SendMsgByYSJAndCLBJobHandler12_12.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
 // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    private int query_id1 = 584;
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
    
    private String appid = "wxfcd75e38666c34eb";//wx.yunovo.cn
	private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
    //private String appid = "wxf120e49da368b3fd";//test
	//private String appsecret = "e6e78cd8798604f4129f3f7c5f9e22b5";
	
 	private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
  	//private final String THRID_API_URL = "http://192.168.3.240:7000";
    //获取accessToken api url
  	private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
  	//发送模板消息 api url
  	private final String SEND_TEMPLATE_MESSAGE = "/rest/api/wechat/sendTemplateMessage?accessToken=%s";
  	
  	//跳转活动URL
  	//private final String HOST = "http://"+"t.wx.yunovo.cn";
  	private final String HOST = "http://"+"wx.yunovo.cn";
  	private final String WX_LINK_URL = "/wechat/view/flow/buylist.html?iccid=";
  	
  	
  	//服务到期提醒模板id
  	private final String TEMPLATE_ID_SERVICE_START = "nsWrBumtCeQJSUG3TKAfI9XAtVpq11lngXUPMOGR_oA";//线上id
  	//private final String TEMPLATE_ID_SERVICE_START = "2tNIRCvh_4LmdcV7bXfFaAFFQcqPLIQdIAsjlURDzB8";//test
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			sendMsgHandler();
		} catch (Exception e) {
            a.setCode(500);
            a.setMsg("发送模板消息异常");
            logger.error("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][发送模板消息异常],e={}",e);
            XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
            return a;
		}
		
		a.setCode(200);
        a.setMsg("发送模板消息成功");
		return a;
    }

    public void sendMsgHandler() throws Exception {
    	JSONArray yds_result = null;
    	int result = 0;
    	yds_result = findYundashData(query_id1,yundash_api_key);
    	XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][用户总数],sum={0}",yds_result.size());
    	
    	String open_id = null;
    	String iccid = null;
    	String time = null;
    	SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
    	Long longTime = null;
    	String dateString = null;
    	Integer count = 0;
    	Map<String,String> map = new HashMap<>();
    	try {
			for (int i=0; i<yds_result.size(); i++) {
				JSONObject by_wx = yds_result.getJSONObject(i);
				open_id = by_wx.getString("open_id");
				if(map.containsKey(open_id)) {
					count++;
					continue;
				}else {
					map.put(open_id, "");
				}
				iccid = by_wx.getString("card_iccid");
				time = by_wx.getString("time_expire");
				/*if(i==0) {
					open_id = "oXEDlt_p6Tzeqd8Yo0DyE1v-9IMg";
					iccid = "89860619000022009306";
					try {
						longTime = Long.parseLong(time);
						dateString = sf.format(new Date(longTime));
						result += sendTemplateMessage(open_id,appid, appsecret, toTemMesJson1(dateString,iccid,open_id,HOST+WX_LINK_URL+iccid+"&from=push"));
					} catch (Exception e) {
						XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][用户发送模板消息失败],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
					}
				}*/
				try {
				    longTime = Long.parseLong(time);
					dateString = sf.format(new Date(longTime));
					result += sendTemplateMessage(open_id,appid, appsecret, toTemMesJson1(dateString,iccid,open_id,HOST+WX_LINK_URL+iccid+"&from=push"));
				} catch (Exception e) {
					XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][用户发送模板消息失败],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
				}
			}
			logger.info("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][发送成功数及重复数]sum={},count={},",result,count);
			XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][发送成功数及重复数],success={0},count={1}",result,count);
		} catch (Exception e) {
			XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendMsgHandler][用户发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
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
			logger.info("[SendMsgByYSJOpenIdJobHandler.sendTemplateMessage][发送微信模板消息成功]open_id={},result={}",open_id,result);
            XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendTemplateMessage][发送微信模板消息成功]open_id={0},result={1}",open_id,result);
			return 1;
		}else {
			logger.info("[SendMsgByYSJOpenIdJobHandler.sendTemplateMessage][发送微信模板消息失败]open_id={},result={},message={},url={}",open_id,result,message,url);
            XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler.sendTemplateMessage][发送微信模板消息失败]open_id={0},result={1},message={2},url={3}",open_id,result,message,url);
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
			logger.info("[SendMsgByYSJOpenIdJobHandler.getAccessToken][ERROR]从网络获取access_token失败,result={},params={}",result,JSONObject.toJSONString(params));
		}else {
			logger.info("[SendMsgByYSJOpenIdJobHandler.getAccessToken]从网络获取到access_token成功，access_token={},params={}",json.getString("access_token"),JSONObject.toJSONString(params));
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
	//模板消息组装
	public String toTemMesJson1 (String time,String iccid,String openid, String url) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id",TEMPLATE_ID_SERVICE_START);
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("您的套餐流量已耗尽，酷我音乐、高德导航和爱奇艺已无法连接网络。\n\n若需在线获取实时内容，请保持网络畅通。\n","#FF2400"));
		data.put("keyword1", toVC_JsonObj("SIM卡套餐服务","#000001"));
		data.put("keyword2", toVC_JsonObj(time,"#000001"));
		data.put("keyword3", toVC_JsonObj(iccid,"#000001"));
		data.put("remark", toVC_JsonObj("#年终狂欢#全场流量5折来袭，更低价格，仅限今日！\n\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public JSONObject toVC_JsonObj(String value,String color){
		JSONObject json = new JSONObject();
		json.put("value", value);
		json.put("color", color);
		return json;
	}
	
	public JSONArray findYundashData(int query_id,String yundash_api_key) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id,yundash_api_key);
		logger.info("[SendMsgByYSJOpenIdJobHandler][开始调yundash查询用户]  url={}",yds_query_url);
		XxlJobLogger.log("[SendMsgByYSJOpenIdJobHandler][开始调yundash查询用户]  url={0}",yds_query_url);
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
		return yds_result;
	}
}
