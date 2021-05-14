package cn.yunovo.job.executor.service.jobhandler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import cn.yunovo.job.executor.util.redis.JedisPoolUtil;

/**
 * 0 0 18 1 2 ? 
 * 19年春节活动推送(晚上18点全推)
 */
public class GetTemplateidByDomain extends IJobHandler {
	
	@Autowired
	public RestTemplate restTemplate;
	
	@Autowired
	private JedisPoolUtil jedisPoolUtil;
	
	private String yundash_url = "http://yundash.yunovo.cn";
	
	private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
	
	private int by_query_id_1 = 82;//20位卡
	
	private int by_query_id_2 = 86;//19位卡
	
	//外部第三方接口服务地址("http://isapi.prd.yunovo.cn      http://localhost:7000";)
	private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
	
	//获取accessToken api url
	private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
	
	//发送模板消息 api url
	private final String SEND_TEMPLATE_MESSAGE = "/rest/api/wechat/sendTemplateMessage?accessToken=%s";
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
	SimpleDateFormat sdf_t = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//跳转活动URL
	private final String WX_LINK_URL = "%s%s/wechat/view/flow/chunjieactivity.html?activityTag=push&cardIccid=%s";

	@Override
	public ReturnT<String> execute(String param) throws Exception {
		XxlJobLogger.log("开始时间:={0}",sdf_t.format(new Date()));
		
		int total = 0;
		int send_succ_total = 0;
		
		//step1:获取微信信息
		Map<String, Map> wx_conf_map = jedisPoolUtil.getMap("wechat_configs", String.class, Map.class);
		
		//step2:查yundash获取数据(20位卡)
		JSONArray yds_result = findYundashData(by_query_id_1);
		total += yds_result.size();
		for (int i=0; i<yds_result.size(); i++) {
			JSONObject by_wx = yds_result.getJSONObject(i);
			String card_iccid = by_wx.getString("card_iccid");
			String open_id = by_wx.getString("open_id");
			String wx_domain = by_wx.getString("wx_domain");
			
			//redis获取
			if(null == wx_conf_map.get(wx_domain)) {
				XxlJobLogger.log("[WxActivitySendMes][根据yundash获取的用户微信域名获取redis配置信息获取不到]wx_domain:{0}",wx_domain);
				continue;
			}
			//获取活动跳转链接
			String http =  String.valueOf(wx_conf_map.get(wx_domain).get("HYPERTEXT_TRANSFER_PROTOCOL") == null ? "http://":wx_conf_map.get(wx_domain).get("HYPERTEXT_TRANSFER_PROTOCOL"));
			String wx_domain_link = String.valueOf(wx_conf_map.get(wx_domain).get("NEW_DOMAIN_URL") == null ? wx_domain:wx_conf_map.get(wx_domain).get("NEW_DOMAIN_URL"));
			String wx_link_url = String.format(WX_LINK_URL, http, wx_domain_link,card_iccid);
			//每个公众号的template_id
			String appid = String.valueOf(wx_conf_map.get(wx_domain).get("WX_APPID"));
			String appsecret = String.valueOf(wx_conf_map.get(wx_domain).get("WX_APPSECRET"));
			String template_id = String.valueOf(wx_conf_map.get(wx_domain).get("WX_TPL_MESSAGE"));
			if("clw.xianzhigps.com".equals(wx_domain)) {
				template_id = "Ww4r1_nQTBKqT9HaKNJWa2eZo2yZUxDTrddc9HTF13I";
			}
			
			send_succ_total += sendTemplateMessage(appid, appsecret, toTemMesJson4(open_id,template_id,wx_link_url), card_iccid);
		}
		
		//step3:查yundash获取数据(19位卡)
		yds_result = findYundashData(by_query_id_2);
		total += yds_result.size();
		for (int i=0; i<yds_result.size(); i++) {
			JSONObject by_wx = yds_result.getJSONObject(i);
			String card_iccid = by_wx.getString("card_iccid");
			String open_id = by_wx.getString("open_id");
			String wx_domain = by_wx.getString("wx_domain");
			
			//redis获取
			if(null == wx_conf_map.get(wx_domain)) {
				XxlJobLogger.log("[WxActivitySendMes][根据yundash获取的用户微信域名获取redis配置信息获取不到]wx_domain:{0}",wx_domain);
				continue;
			}
			//获取活动跳转链接
			String http =  String.valueOf(wx_conf_map.get(wx_domain).get("HYPERTEXT_TRANSFER_PROTOCOL") == null ? "http://":wx_conf_map.get(wx_domain).get("HYPERTEXT_TRANSFER_PROTOCOL"));
			String wx_domain_link = String.valueOf(wx_conf_map.get(wx_domain).get("NEW_DOMAIN_URL") == null ? wx_domain:wx_conf_map.get(wx_domain).get("NEW_DOMAIN_URL"));
			String wx_link_url = String.format(WX_LINK_URL, http, wx_domain_link,card_iccid);
			//每个公众号的template_id
			String appid = String.valueOf(wx_conf_map.get(wx_domain).get("WX_APPID"));
			String appsecret = String.valueOf(wx_conf_map.get(wx_domain).get("WX_APPSECRET"));
			String template_id = String.valueOf(wx_conf_map.get(wx_domain).get("WX_TPL_MESSAGE"));
			if("clw.xianzhigps.com".equals(wx_domain)) {
				template_id = "Ww4r1_nQTBKqT9HaKNJWa2eZo2yZUxDTrddc9HTF13I";
			}
			
			send_succ_total += sendTemplateMessage(appid, appsecret, toTemMesJson4(open_id,template_id,wx_link_url), card_iccid);
		}
		
		XxlJobLogger.log("结束时间:={0}",sdf_t.format(new Date()));
		ReturnT<String> a = new ReturnT<String>();
		a.setCode(200);
		a.setMsg("模版消息推送结果:总共发送="+total+",成功发送="+send_succ_total);
		return a;
	}
	
	public JSONArray findYundashData(int query_id) {
		String yds_query_url = String.format(yundash_url+"/api/queries/%s/results.json?api_key=%s", query_id,yundash_api_key);
		XxlJobLogger.log("[WxActivitySendMes][开始调yundash查询用户]  url={0}",yds_query_url);
		String yds_result_str = restTemplate.getForObject(yds_query_url, String.class);
		JSONObject yds_result_json = JSONObject.parseObject(yds_result_str);
		JSONArray yds_result = yds_result_json.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
		XxlJobLogger.log("[WxActivitySendMes][查yundash获取的用户]用户总数+{0}",yds_result.size());
		return yds_result;
	}
	
	
	/**
	 * 发送模板消息
	 * @param message
	 * @return
	 */
	public int sendTemplateMessage(String appid, String appsecret,String message,String card_iccid) {
		
		String accessToken =  this.getAccessToken(appid, appsecret, "0");
		
		String url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,accessToken);
		String result = httpPost(url, message);
		JSONObject jsonObject  = JSONObject.parseObject(result);
		
		if(jsonObject != null && (jsonObject.getInteger("errcode")==40001 || jsonObject.getInteger("errcode")==42001)) {
			accessToken =  this.getAccessToken(appid, appsecret, "1");
			url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,accessToken);
			result = httpPost(url, message);
			jsonObject = JSONObject.parseObject(result);
		}
		
		if(StringUtils.equals(jsonObject.getString("errcode"),"0")){
			XxlJobLogger.log("[WechatService.sendTemplateMessage][发送微信模板消息成功]result:{0},卡iccid:{1}",result,card_iccid);
			return 1;
		}else {
			XxlJobLogger.log("[WechatService.sendTemplateMessage][发送微信模板消息失败]result:{0},message:{1},url:{2},卡iccid:{3}",result,message,url,card_iccid);
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
			XxlJobLogger.log("[WxMassSendMessage.getAccessToken][ERROR]从网络获取access_token失败,result={0},params={1}",result,JSONObject.toJSONString(params));
		}else {
			XxlJobLogger.log("[WxMassSendMessage.getAccessToken]从网络获取到access_token成功，access_token={0},params={1}",json.getString("access_token"),JSONObject.toJSONString(params));
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
	
	public JSONObject toVC_JsonObj(String value,String color){
		JSONObject json = new JSONObject();
		json.put("value", value);
		json.put("color", color);
		return json;
	}
	
	
	public String toTemMesJson4 (String openid, String template_id, String url) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id",template_id);
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("#您的专属折扣仅剩最后3小时！#\r\n流量折扣多划算，不要等结束了才知道。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("春节狂欢购","#000001"));
		data.put("keyword2", toVC_JsonObj("2019/1/27-2019/2/1","#000001"));
		data.put("keyword3", toVC_JsonObj("微信公众号","#000001") );
		data.put("remark", toVC_JsonObj("\n车载流量限时88折，最后3小时\n立刻开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}

}
