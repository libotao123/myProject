package cn.yunovo.job.executor.service.jobhandler;

import java.util.HashMap;
import java.util.Map;

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

import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="send_msg_by_openid")
@Component
/*
 * 指定OpenId推送微信模板消息
 * 
 * 
 */
public class SendMsgByOpenIdJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(SendMsgByOpenIdJobHandler.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private String appid = "wx91fe667d89b01e6d";//wx.yunovo.cn
	private String appsecret = "24c7e96b4eefb471bc19b15abd28572d";
//    private String appid = "wxf120e49da368b3fd";
//	private String appsecret = "e6e78cd8798604f4129f3f7c5f9e22b5";
  	//服务开启通知模板id(个人消息通知)
//	private final String TEMPLATE_ID_SERVICE_START = "dW0yuJqoABf94XurnjXbY-XfX9UOs-JfNiSz03iSV4k";//测试id
	private final String TEMPLATE_ID_SERVICE_START = "Ergo3eEF_fbn7B89Kly5R6F98UrFffLmXezlRTqDoYM";//线上id
	
 	private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";
//  	private final String THRID_API_URL = "http://192.168.3.240:7000";
    //获取accessToken api url
  	private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";
  	//发送模板消息 api url
  	private final String SEND_TEMPLATE_MESSAGE = "/rest/api/wechat/sendTemplateMessage?accessToken=%s";
  	
  	//跳转活动URL
//  	private final String HOST = "http://"+"t.wx.yunovo.cn";
  	private final String HOST = "http://"+"wx.yunovo.cn";
  	private final String WX_LINK_URL = "http://mp.weixin.qq.com/s?__biz=MzA4NTI1NDc0OA==&mid=502121011&idx=1&sn=444e2fbc6af9cc9c72c87083baf005ed&chksm=07c3d50030b45c162feab7f3683f7c3da93b85a9b4cf6a0a2163155670b24896fb07ef2a6ca0#rd";
  	
  	
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			sendMsgHandler();
		} catch (Exception e) {
            a.setCode(500);
            a.setMsg("发送模板消息异常");
            logger.error("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送模板消息异常],e={}",e);
            XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
            return a;
		}
		
		a.setCode(200);
        a.setMsg("发送模板消息成功");
		return a;
    }

    public void sendMsgHandler() throws Exception {
    	String openids = "oYwc71BdOnKlkO0THJ5q5G40N0Zk,oYwc71DwEs68fgwNNZKVrcXVd-64";
    	String[] arr = openids.split(",");
    	Integer count = arr.length;
    	logger.info("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送微信模板总数]sum={}",count);
    	XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendMsgHandler],sum={0}",count);
    	String open_id = null;
		int result = 0;
    	try {
			for(int i=0;i<count;i++) {
				open_id = arr[i];
              	try {
					result += sendTemplateMessage(open_id,appid, appsecret, toTemMesJson1(open_id,WX_LINK_URL));
				} catch (Exception e) {
					XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送模板消息失败],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
				}
			}
			logger.info("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送成功数]sum={}",result);
			XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送成功数],successA={0}",result);
		} catch (Exception e) {
			XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendMsgHandler][发送模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
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
			logger.info("[SendMsgByOpenIdJobHandler.sendTemplateMessage][发送微信模板消息成功]open_id={},result={}",open_id,result);
            XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendTemplateMessage][发送微信模板消息成功]open_id={0},result={1}",open_id,result);
			return 1;
		}else {
			logger.info("[SendMsgByOpenIdJobHandler.sendTemplateMessage][发送微信模板消息失败]open_id={},result={},message={},url={}",open_id,result,message,url);
            XxlJobLogger.log("[SendMsgByOpenIdJobHandler.sendTemplateMessage][发送微信模板消息失败]open_id={0},result={1},message={2},url={3}",open_id,result,message,url);
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
			logger.info("[SendMsgByOpenIdJobHandler.getAccessToken][ERROR]从网络获取access_token失败,result={},params={}",result,JSONObject.toJSONString(params));
		}else {
			logger.info("[SendMsgByOpenIdJobHandler.getAccessToken]从网络获取到access_token成功，access_token={},params={}",json.getString("access_token"),JSONObject.toJSONString(params));
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
	public String toTemMesJson1 (String openid, String url) {
		JSONObject message = new JSONObject();
		message.put("touser", openid);
		message.put("template_id",TEMPLATE_ID_SERVICE_START);
		message.put("url",url);
		
		JSONObject data = new JSONObject();
		data.put("first", toVC_JsonObj("您收到一份核心用户群邀请，此为定向特邀，请您尽快处理。\n","#000001"));
		data.put("keyword1", toVC_JsonObj("达客出行","#000001"));
//		data.put("keyword2", toVC_JsonObj(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss\n"),"#000001"));
		data.put("keyword2", toVC_JsonObj("达客体验官","#000001"));
		data.put("keyword3", toVC_JsonObj("日赚百元红包，还有行车记录仪免费拿","#000001"));
		data.put("keyword4", toVC_JsonObj("深圳\n","#000001"));
		data.put("remark", toVC_JsonObj("达客出行作为您智能车机的出行服务商，为提升您的车机使用体验，现特邀您进入深圳达客群，在群内您可免费享受现金红包、专属客服、特定优惠、行车记录仪等多重福利。\n\n【名额有限，速速前来，点击详情扫码入群】","#000001"));
		
		message.put("data",data);
		
		return message.toString();
	}
	
	public JSONObject toVC_JsonObj(String value,String color){
		JSONObject json = new JSONObject();
		json.put("value", value);
		json.put("color", color);
		return json;
	}
	
	public void other() throws Exception {
		String content ="Hello，欢迎光临，您的随身车管家为您服务！\\r\\n\\r\\n让我们先绑定设备吧\\r\\n☞<a href=\\'%s\\'>戳我绑定设备</a>☜领取免费流量，让爱车24小时都在线\\r\\n\\r\\n点击【智能车联】，随时随地手机控车，让用车更便利；\\r\\n\\r\\n而在【服务中心】，您可以便捷的查询、办理流量相关业务；\\r\\n\\r\\n此外，【个人中心】还能为您提供设备绑定、解绑等相关服务！\\r\\n\\r\\n还不够？那就回复【人工客服】专属客服将第一时间为您答疑解惑[咖啡][咖啡][咖啡]";
		String sql = "UPDATE cc_wx_configuration_info SET set_val = 'Hello，欢迎光临，您的随身车管家为您服务！\\r\\n\\r\\n让我们先绑定设备吧\\r\\n☞<a href=\\'%s\\'>戳我绑定设备</a>☜领取免费流量，让爱车24小时都在线\\r\\n\\r\\n点击【智能车联】，随时随地手机控车，让用车更便利；\\r\\n\\r\\n而在【服务中心】，您可以便捷的查询、办理流量相关业务；\\r\\n\\r\\n此外，【个人中心】还能为您提供设备绑定、解绑等相关服务！\\r\\n\\r\\n还不够？那就回复【人工客服】专属客服将第一时间为您答疑解惑[咖啡][咖啡][咖啡]' WHERE id = 83";
		try {
			int result = jdbcTemplate.update(sql);
          	XxlJobLogger.log("[SendMsgByOpenIdJobHandler.other][更新数据成功],sql={0},result={1}",sql,result);
		} catch (DataAccessException e) {
          	XxlJobLogger.log("[SendMsgByOpenIdJobHandler.other][更新数据异常],sql={0},e={1}",sql,ExceptionUtils.getStackTrace(e));
		}
	}
}
