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

/**
 * 0 0 21 30 12 ? *
 * 元旦预热晚上9点消息推送
 */
public class PrizeewheelAddCollectionEventJobHandlerBBB extends IJobHandler {
	
	@Autowired
	public RestTemplate restTemplate;
	
	private String yundash_url = "http://yundash.yunovo.cn";
	
	private String yundash_api_key = "5G2lSabdbnxOCmI4bcFCo96JXZPFp5A8pgFPHrkZ";
	
	private int by_query_id_1 = 329;
	
	private int by_query_id_2 = 75;
	
	//外部第三方接口服务地址("http://localhost:7000";)
	private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
	
	private String appid = "wxfcd75e38666c34eb";
	
	private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
	
	//获取accessToken api url
	private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
	
	//发送模板消息 api url
	private final String SEND_TEMPLATE_MESSAGE = "/rest/api/wechat/sendTemplateMessage?accessToken=%s";
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
	
	//跳转活动URL
	private final String WX_LINK_URL = "http://wx.yunovo.cn/wechat/view/flow/yuandanactivity.html?activityTag=push";

	@Override
	public ReturnT<String> execute(String param) throws Exception {
		int total = 0;
		int gprs_total_suc = 0;
		int valid_total_suc = 0;
		int other_total_suc = 0;
		
		//step2:查yundash获取数据
		JSONArray yds_result = findYundashData(by_query_id_1);
		total += yds_result.size();
		for (int i=0; i<yds_result.size(); i++) {
			JSONObject by_wx = yds_result.getJSONObject(i);
			String card_iccid = by_wx.getString("card_iccid");
			String autocar_tag = by_wx.getString("autocar_tag");
			String max_unused = by_wx.getString("max_unused");
			String time_expire = (by_wx.getLong("time_expire") == null ? "":sdf.format(new Date(by_wx.getLong("time_expire"))));
			String open_id = by_wx.getString("open_id");
			String wx_link_url = WX_LINK_URL;
			
			int flag = by_wx.getInteger("flag"); 
/**测试*/	
			if ("89860117750037937563".equals(card_iccid) || "89860617040000600280".equals(card_iccid)) {
				//1流量预警 ; 2有效期预警; 0其它
				if (flag == 1) {
					gprs_total_suc += sendTemplateMessage(appid, appsecret, toTemMesJson2(open_id,wx_link_url, card_iccid, autocar_tag, max_unused, time_expire));
				} else if (flag == 2) {
					valid_total_suc += sendTemplateMessage(appid, appsecret, toTemMesJson3(open_id,wx_link_url, card_iccid, autocar_tag, max_unused, time_expire));
				} else if (flag == 0) {
					other_total_suc += sendTemplateMessage(appid, appsecret, toTemMesJson1(open_id,wx_link_url, card_iccid, autocar_tag, max_unused, time_expire));
				}
			}
			
/**测试end*/	
		}
		
		yds_result = findYundashData(by_query_id_2);
		total += yds_result.size();
		for (int i=0; i<yds_result.size(); i++) {
			JSONObject by_wx = yds_result.getJSONObject(i);
			String card_iccid = by_wx.getString("card_iccid");
			String autocar_tag = by_wx.getString("autocar_tag");
			String max_unused = by_wx.getString("max_unused");
			String time_expire = (by_wx.getLong("time_expire") == null ? "":sdf.format(new Date(by_wx.getLong("time_expire"))));
			String open_id = by_wx.getString("open_id");
			String wx_link_url = WX_LINK_URL;
			
			int flag = by_wx.getInteger("flag"); 
/**测试*/	
			if ("89860117750037937563".equals(card_iccid) || "89860617040000600280".equals(card_iccid)) {
				//1流量预警 ; 2有效期预警; 0其它
				if (flag == 1) {
					gprs_total_suc += sendTemplateMessage(appid, appsecret, toTemMesJson2(open_id,wx_link_url, card_iccid, autocar_tag, max_unused, time_expire));
				} else if (flag == 2) {
					valid_total_suc += sendTemplateMessage(appid, appsecret, toTemMesJson3(open_id,wx_link_url, card_iccid, autocar_tag, max_unused, time_expire));
				} else if (flag == 0) {
					other_total_suc += sendTemplateMessage(appid, appsecret, toTemMesJson1(open_id,wx_link_url, card_iccid, autocar_tag, max_unused, time_expire));
				}
			}
			
/**测试end*/			
			
			
		}

		ReturnT<String> a = new ReturnT<String>();
		a.setCode(202);
		a.setMsg("模版消息推送结果:总共发送="+total+",流量预警发送="+gprs_total_suc+",有效期预警发送="+valid_total_suc+",其它发送成功="+other_total_suc);
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
	public int sendTemplateMessage(String appid, String appsecret,String message) {
		
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
			XxlJobLogger.log("[WechatService.sendTemplateMessage][发送微信模板消息成功]result:{0}",result);
			return 1;
		}else {
			XxlJobLogger.log("[WechatService.sendTemplateMessage][发送微信模板消息失败]result:{0},message={1},url={2}",result,message,url);
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
	
	
	public String toTemMesJson1 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","Wpz7lIZQJXxecO24cfoTrc-A3szIKO0SoX2bPklvia8");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("#跨年钜惠，现金红包明天开抢！#\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("元旦放价，跨年钜惠","#000001"));
		data.put("keyword2", toVC_JsonObj("2018/12/31-2019/1/2","#000001"));
		data.put("keyword3", toVC_JsonObj("微信：小达在线","#000001") );
		data.put("remark", toVC_JsonObj("\n一年就一次，充值即返现金红包，最高99元！\r\n限量2019人，戳我了解☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson2 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","8P_TsuX7jTcfSXc63cQsvPcvCefo1PbYZdRh85TICrA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐内流量已不足200M。\r\n用尽后您将无法使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj(card_iccid,"#000001"));
		data.put("keyword2", toVC_JsonObj(max_unused+" MB","#000001"));
		data.put("keyword3", toVC_JsonObj(time_expire,"#000001") );
		if (null == autocar_tag || "".equals(autocar_tag)) {
			data.put("keyword4", toVC_JsonObj("未设置","#000001") );
		} else {
			data.put("keyword4", toVC_JsonObj(autocar_tag,"#000001") );
		}
		data.put("remark", toVC_JsonObj("\n跨年钜惠，充值即返现金红包，最高99元！\r\n限量2019人，戳我了解☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson3 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","nsWrBumtCeQJSUG3TKAfI9XAtVpq11lngXUPMOGR_oA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐有效期已不足15天。\r\n到期后您将无法继续使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("2G畅享体验包","#000001"));
		data.put("keyword2", toVC_JsonObj(time_expire,"#000001"));
		data.put("keyword3", toVC_JsonObj(card_iccid,"#000001") );
		data.put("remark", toVC_JsonObj("\n跨年钜惠，充值即返现金红包，最高99元！\r\n限量2019人，戳我了解☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson4 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","PcvDFsLCVNaB46ZJvUc3hxx5_8kV65gTgnZ4nGj6o98");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("#跨年红包火爆进行中！#\r\n万水千山总是情，充值即可返红包，最高99元！\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("元旦放价，跨年钜惠","#000001"));
		data.put("keyword2", toVC_JsonObj("充值专享，红包返现","#000001"));
		data.put("remark", toVC_JsonObj("\n红包抢得好，生活没烦恼！\r\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson5 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","8P_TsuX7jTcfSXc63cQsvPcvCefo1PbYZdRh85TICrA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐内流量已不足200M。\r\n用尽后您将无法使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj(card_iccid,"#000001"));
		data.put("keyword2", toVC_JsonObj(max_unused+" MB","#000001"));
		data.put("keyword3", toVC_JsonObj(time_expire,"#000001") );
		if (null == autocar_tag || "".equals(autocar_tag)) {
			data.put("keyword4", toVC_JsonObj("未设置","#000001"));
		} else {
			data.put("keyword4", toVC_JsonObj(autocar_tag,"#000001") );
		}
		data.put("remark", toVC_JsonObj("\n跨年钜惠，充值即返现金红包，最高99元！\r\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson6 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","nsWrBumtCeQJSUG3TKAfI9XAtVpq11lngXUPMOGR_oA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐有效期已不足15天。\r\n到期后您将无法继续使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("2G畅享体验包","#000001"));
		data.put("keyword2", toVC_JsonObj(time_expire,"#000001"));
		data.put("keyword3", toVC_JsonObj(card_iccid,"#000001") );
		data.put("remark", toVC_JsonObj("\n跨年钜惠，充值即返现金红包，最高99元！\r\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson7 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","q24JgZ83fkOQlYQWFVtqUney5DusGXQ4N6gGiv-NkmA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("#跨年钜惠火爆进行中！#\r\n春风十里，不如红包送你！\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("元旦放价，跨年钜惠","#000001"));
		data.put("keyword2", toVC_JsonObj("充值即享红包返现","#000001"));
		data.put("keyword3", toVC_JsonObj("2018/12/31-2019/1/2","#000001") );
		data.put("remark", toVC_JsonObj("\n返现名额仅剩1000人，先到先得！戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson8 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","8P_TsuX7jTcfSXc63cQsvPcvCefo1PbYZdRh85TICrA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐内流量已不足200M。\r\n用尽后您将无法使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj(card_iccid,"#000001"));
		data.put("keyword2", toVC_JsonObj(max_unused+" MB","#000001"));
		data.put("keyword3", toVC_JsonObj(time_expire,"#000001") );
		if (null == autocar_tag || "".equals(autocar_tag)) {
			data.put("keyword4", toVC_JsonObj("未设置","#000001") );
		} else {
			data.put("keyword4", toVC_JsonObj(autocar_tag,"#000001") );
		}
		data.put("remark", toVC_JsonObj("\n充值即返现金红包，最高99元，名额仅剩1000人，先到先得\r\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	
	public String toTemMesJson9 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","nsWrBumtCeQJSUG3TKAfI9XAtVpq11lngXUPMOGR_oA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐有效期已不足15天。\r\n到期后您将无法继续使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("2G畅享体验包","#000001"));
		data.put("keyword2", toVC_JsonObj(time_expire,"#000001"));
		data.put("keyword3", toVC_JsonObj(card_iccid,"#000001") );
		data.put("remark", toVC_JsonObj("\n充值即返现金红包，最高99元，名额仅剩1000人，先到先得\r\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson10 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","zWNeeoCDA6zHgeVmHqTveYwuPOIYpXJ3Ig0RYV9mpLY");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("#跨年钜惠，返现最后一天#\r\n下单即返现金红包，错过今天再等一年…\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("元旦放价，跨年钜惠","#000001"));
		data.put("keyword2", toVC_JsonObj("2018/12/31-2019/1/2","#000001"));
		data.put("remark", toVC_JsonObj("\n红包我出，你开心就好！\r\n戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson11 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","8P_TsuX7jTcfSXc63cQsvPcvCefo1PbYZdRh85TICrA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐内流量已不足200M。\r\n用尽后您将无法使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用。\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj(card_iccid,"#000001"));
		data.put("keyword2", toVC_JsonObj(max_unused+" MB","#000001"));
		data.put("keyword3", toVC_JsonObj(time_expire,"#000001") );
		if (null == autocar_tag || "".equals(autocar_tag)) {
			data.put("keyword4", toVC_JsonObj("未设置","#000001") );
		} else {
			data.put("keyword4", toVC_JsonObj(autocar_tag,"#000001") );
		}
		data.put("remark", toVC_JsonObj("\n最后一天！充值即返现金红包，最高99元。\r\n错过等一年，戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson12 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","nsWrBumtCeQJSUG3TKAfI9XAtVpq11lngXUPMOGR_oA");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("糟糕！套餐有效期已不足15天。\r\n到期后您将无法继续使用车辆定位、在线导航等联网服务。\r\n您可充值套餐，以恢复使用\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("2G畅享体验包","#000001"));
		data.put("keyword2", toVC_JsonObj(time_expire,"#000001"));
		data.put("keyword3", toVC_JsonObj(card_iccid,"#000001") );
		data.put("remark", toVC_JsonObj("\n最后一天！充值即返现金红包，最高99元。\r\n错过等一年，戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public String toTemMesJson13 (String openid, String url, String card_iccid,String autocar_tag,String max_unused,String time_expire) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id","zWNeeoCDA6zHgeVmHqTveYwuPOIYpXJ3Ig0RYV9mpLY");
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("#红包返现，最后3小时！#\r\n充值即返现金红包，错过今天再等一年…\n","#3232CD"));
		data.put("keyword1", toVC_JsonObj("元旦放价，跨年钜惠","#000001"));
		data.put("keyword2", toVC_JsonObj("2018/12/31-2019/1/2","#000001"));
		data.put("remark", toVC_JsonObj("\n春风十里，不如送你红包！\r\n仅剩3小时，戳我开抢☞☞☞","#FF2400"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	

}
