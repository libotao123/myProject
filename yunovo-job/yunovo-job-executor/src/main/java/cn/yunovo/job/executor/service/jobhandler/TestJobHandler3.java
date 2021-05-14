package cn.yunovo.job.executor.service.jobhandler;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Consumer;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;


/**
 * 消费kafka行程数据,推送wx消息
 */
public class TestJobHandler3 extends IJobHandler {

    //上一次更新的日期
    SimpleDateFormat sdf_date = new SimpleDateFormat("yyyy-MM-dd");
    String last_time = "";
    int successNum = 0;
    int failNum = 0;
    int noSubNum = 0;
    int closeSwitchNum = 0;

    @Autowired
    CassandraSessionFactoryBean cassandraSessionFactoryBean;

    private String appid = "wxfcd75e38666c34eb";
    private String appsecret = "52c04f94a9a41cc2af906194f5779df4";
    String wx_link_url = "https://wx.yunovo.cn/wechat/view/drivereport/detail.html?device_sn=%s&start_time=%s&end_time=%s&openId=%s";
    //新设备跳转链接
    String newDevice_link_url = "pages/webView/main?page=trace&device_sn=%s&start_time=%s&end_time=%s&openId=%s";
    String oldDriver_link_url = "/pages/trip/main?page=oldDriver&device_sn=%s&ts=%s";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private boolean isStop = true;

    private ConsumerFactory<Integer, String> consumerFactory = null;

    private KafkaMessageListenerContainer<Integer, String> container  = null;

    private final String KAFKA_BOOTSTRAP_URL = "10.18.0.14:9092";
    private final String MTP_YIREN_DATA_TOPICS = "eventTopic";

    @Autowired
    private RestTemplate restTemplate;

    //外部第三方接口服务地址("http://isapi.prd.yunovo.cn      http://localhost:7000";)
    private final String THRID_API_URL = "http://isapi.prd.yunovo.cn";

    //获取accessToken api url
    private final String ACCESS_TOKEN_URL = "/rest/api/wechat/getToken?appid={appid}&secret={secret}&cleanCache={cleanCache}";

    //发送模板消息 api url
    private final String SEND_TEMPLATE_MESSAGE = "/rest/api/wechat/sendTemplateMessage?accessToken=%s";

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// 相差半个小时就不推送报告
    private long longTime = 1800000;

    public void init() {

        XxlJobLogger.log("[YrenDataJob.init]初始化job");
        //初始化kafka
        initKafka();
        XxlJobLogger.log("[YrenDataJob.init]msg={0}", "kafka 初始完成");
        isStop = false;
    }

    private void initKafka() {

        //初始化kafka 连接
        KafkaProperties kafkaProperties = new KafkaProperties();
        List<String> bootstrapServers = new ArrayList<>();

        bootstrapServers.add(KAFKA_BOOTSTRAP_URL); //设置服务器地址
        kafkaProperties.setBootstrapServers(bootstrapServers);

        Consumer customer = kafkaProperties.getConsumer();
        customer.setAutoOffsetReset("latest"); //如果客户端未提交offset/earliest  则从最早开始消费latest
        customer.setClientId("MTP-YIREN-REPORTING-DATA-01"); //如果设置clientId则客户端为单线程消费
        customer.setEnableAutoCommit(false); //设置非自动提交
        customer.setGroupId("MTP-DRIVINGANALYSIS-WX1");
        //创建customerFactory
        consumerFactory = new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
        ContainerProperties containerProps  = new ContainerProperties(MTP_YIREN_DATA_TOPICS);
        containerProps.setAckMode(AbstractMessageListenerContainer.AckMode.MANUAL); //设置手工ack
        containerProps.setMessageListener(new AcknowledgingMessageListener<Integer, String>() {

            @Override
            public void onMessage(ConsumerRecord<Integer, String> data, Acknowledgment acknowledgment) {


                acknowledgment.acknowledge();
                String value = data.value();
                //XxlJobLogger.log("[车萝卜埋点原始数据]msg={0}", value);

                String[] values = split(value, " ");
                if(values.length < 5) {
                    return;
                }
                String temp = values[4];
                if(!temp.startsWith("/reporting")) {
                    return ;
                }
                temp = getBody(value);

                if(temp == null || temp.length() < 1) {
                    return ;
                }

                temp = temp.replace("\\\"","\"");
                temp = temp.replace("\\\\\"","\\\"");
                JSONArray array = JSONArray.parseArray(temp);
                //保存数据库
                if(array == null || array.isEmpty()) {
                    return ;
                }


                //array为上报的所有数据
                for(int i = 0; i < array.size(); i ++) {
                    JSONObject reportData = array.getJSONObject(i);
                    if ("drivinganalysis".equals(reportData.get("event"))) {
                        //
                        save(reportData);
                        continue;
                    }
                }
            }

        });
        containerProps.setErrorHandler(new ErrorHandler() {

            @Override
            public void handle(Exception thrownException, ConsumerRecord<?, ?> data) {
                if (!(thrownException instanceof com.alibaba.fastjson.JSONException)) {
                    XxlJobLogger.log("[ErrorHandler]msg={0},data={1},exception={2}", "数据消费失败", String.valueOf(data.value()), ExceptionUtils.getStackTrace(thrownException));
                }
            }
        });
        container = new KafkaMessageListenerContainer<>(this.consumerFactory, containerProps);
        container.setAutoStartup(false);
    }

    @Override
    public ReturnT<String> execute(String param) throws Exception {

        last_time = sdf_date.format(new Date());

        XxlJobLogger.log("[YrenDataJob.execute]msg={0}", "kafka 开始消费,任务开始执行");
        container.start();

        while(!isStop) {
            Thread.sleep(1000);
        }
        XxlJobLogger.log("[YrenDataJob.execute]msg={0}", "kafka 任务执行结束");
        // return ReturnT.SUCCESS;
        ReturnT<String> a = new ReturnT<String>();
        a.setCode(200);
        a.setMsg("执行弈人任务:" + new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
        return a;
    }

    /**
     * Parse Nginx log '$remote_addr [$time_local] $request $request_length $request_body'
     * @param line
     * @return
     */

    private final static String getBody(String line) {
        int idx = 0;
        int j = 0;
        for (int i=0;i<line.length();i++) {
            if (line.charAt(i) == ' ' && j++ == 6) {
                idx = i;
                break;
            }

        }
        return line.substring(++idx);
    }


    private final static String[] split(String line, String charactor) {
        return line.split(charactor);
    }

    @Override
    public void destroy() {
        XxlJobLogger.log("[YrenDataJob.destroy]msg={0}", "执行销毁操作");
        container.stop();
        XxlJobLogger.log("[YrenDataJob.destroy]msg={0}", "kafka停止消费");
        isStop = true;
        XxlJobLogger.log("[YrenDataJob.destroy]msg={0}", "终止任务");
        XxlJobLogger.log("[WechatService.sendTemplateMessage][{0}],成功={1},失败={2},取关={3},公众号关闭消息通知={4}", last_time, successNum, failNum, noSubNum, closeSwitchNum);

    }


    public void save(JSONObject reportData) {
        String did = reportData.getString("did");

        JSONObject attributes = reportData.getJSONObject("attributes");
        String iccid = attributes.getString("ICCID");
        long start_time = attributes.getLong("start_time")-60000l;
        long end_time = attributes.getLong("end_time");
        long nowDay = System.currentTimeMillis();
        // 当当前时间和行车报告的结束时间相比较，如果时间相差半小时 那就发报告
        if( (nowDay - end_time > longTime) || (end_time - nowDay > longTime)){
          //  XxlJobLogger.log("[YrenDataJob.save]当前sn={0}的该次行程的结束时间为end_time={1},当前时间为now={2},相差超过={3},所以不发行程报告", did,end_time,nowDay,longTime);
            return;
        }

        int mileage = attributes.getInteger("mileage");
        int cost_time = attributes.getInteger("cost_time");
        String cost_time_str = secondToTime(cost_time);

        int max_speed = attributes.getInteger("max_speed");
        int avg_speed = attributes.getInteger("avg_speed");

        JSONArray fast_speed_up = attributes.getJSONArray("fast_speed_up");
        JSONArray fast_brake = attributes.getJSONArray("fast_brake");
        JSONArray fast_swerve = attributes.getJSONArray("fast_swerve");
        JSONArray overspeed = attributes.getJSONArray("overspeed");

        int fast_speed_up_num = fast_speed_up.size();
        int fast_brake_num = fast_brake.size();
        int fast_swerve_num = fast_swerve.size();
        int overspeed_num = overspeed.size();


        int score = 100-fast_speed_up_num*3-fast_brake_num*2-fast_swerve_num*3-overspeed_num*5;

        if(cost_time<180 || mileage<2000) {
            return;
        }

        //是否是新设备
        boolean isNewDevice = isNewDevice(did);
        //boolean isNewDevice = false;

        //step1.判断是否有开关
        if (!"1".equals(isCloseSwitch(did))) {
            //step2.查openid
            List<String> params = new ArrayList<>();
            String sql = "SELECT d.device_sn,d.device_name,cw.open_id, cw.wx_domain FROM cc_device d INNER JOIN cc_device_bind db ON d.device_sn = db.device_sn INNER JOIN cc_customer_wx cw ON cw.wx_id = db.wx_id where db.device_sn = ? and wx_domain ='wx.yunovo.cn' and  db.status = 1";
            params.add(did);
            jdbcTemplate.query(sql,params.toArray(), new RowMapper() {
                @Override
                public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                    //step2.模版消息内容
                    JSONObject message = new JSONObject();
                    message.put("touser", rs.getString("open_id"));
                    message.put("template_id","Aifr2-PhnA4ngB8JYi-H3M5wFYxUB_kQcyRnuZGoLPY");

                    JSONObject data = new JSONObject();
                    if(isNewDevice){
                        JSONObject j = new JSONObject();
                        j.put("appid","wx142320d8af0a105c");
                        //j.put("pagepath",String.format(newDevice_link_url,did,start_time,end_time,rs.getString("open_id")));
                        j.put("pagepath",String.format(oldDriver_link_url,did,System.currentTimeMillis()));
                        message.put("miniprogram",j);
                        data.put("first", toVC_JsonObj("您本次的【驾驶分析报告】已经生成!\n"+"超速"+overspeed_num+"次,急加速"+fast_speed_up_num+"次,急转弯"+fast_swerve_num+"次,急刹车"+fast_brake_num+"次","#38383C"));
                        data.put("remark", toVC_JsonObj("【现金福利】行车报告有奖评比,点击详情报名参与>>","#FF7000"));
                    }else {
                        message.put("url", String.format(wx_link_url, did, start_time, end_time,rs.getString("open_id")));
                        data.put("first", toVC_JsonObj("您本次的驾驶行为报告已经生成!\n","#38383C"));
                        data.put("remark", toVC_JsonObj("\n驾驶行为汇总:超速"+overspeed_num+"次,急加速"+fast_speed_up_num+"次,急转弯"+fast_swerve_num+"次,急刹车"+fast_brake_num+"次,点击查看详情。","#FF7000"));
                    }




                    data.put("keyword1", toVC_JsonObj(rs.getString("device_name"),"#38383C"));//车名
                    data.put("keyword2", toVC_JsonObj((double)mileage/(double)1000+"km","#38383C"));//行驶里程
                    data.put("keyword3", toVC_JsonObj(cost_time_str,"#38383C"));//驾驶时长
                    data.put("keyword4", toVC_JsonObj(avg_speed+"km/h","#38383C"));//avgspeed
                    data.put("keyword5", toVC_JsonObj(sdf.format(new Date()),"#38383C"));//时间
//                    data.put("remark", toVC_JsonObj("\n驾驶行为汇总:超速"+overspeed_num+"次,急加速"+fast_speed_up_num+"次,急转弯"+fast_swerve_num+"次,急刹车"+fast_brake_num+"次,点击查看详情。","#FF7000"));
//                    data.put("remark", toVC_JsonObj("行车报告可参加有奖评比，公众号内回复 1 即可参与。","#FF7000"));

                    message.put("data",data);

                    //step3.发送
                    sendTemplateMessage(message.toString());
                  //  message.put("touser", "oXEDlt43449I27nG6caJswBrxBBA");
                  //   sendTemplateMessage(message.toString());
                   if("106D121909007681".equals(did)) {
                        XxlJobLogger.log("查收不到报告,烟行程:"+reportData.toJSONString());
                    }

                  /*if (isNewDevice) {
                    	message.put("touser", "oXEDltzas5o9gZvyTtPk8ji-ZiGY");
                    	sendTemplateMessage(message.toString());
                    }*/
                    if("L1110ED032107724".equals(did)) {
                        message.put("touser", "oXEDltzas5o9gZvyTtPk8ji-ZiGY");
                        JSONObject data2 = message.getJSONObject("data");
                        data.put("remark", toVC_JsonObj(attributes.toJSONString(),"#FF7000"));
                        message.put("data",data2);
                        sendTemplateMessage(message.toString());
                    }

                    return null;
                }
            });
        } else {
            closeSwitchNum++;
        }



    }

    public String isCloseSwitch(String device_sn) {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT set_val FROM cc_device d inner join cc_device_setting ds on d.device_id = ds.device_id where device_sn = ? and ds.set_key = 'noTraceReport'";
        params.add(device_sn);
        Map<String, String> result = new HashMap<>();
        jdbcTemplate.query(sql,params.toArray(),new RowMapper(){
            @Override
            public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                result.put("set_val", rs.getString("set_val"));
                return null;
            }
        });
        return result.isEmpty() ? "" : result.get("set_val");
    }

    //是否是新设备
    public boolean isNewDevice(String device_sn) {
        List<Object> params = new ArrayList<>();
        String sql = "select count(1) as c from cc_device d inner join cc_device_product_model m on d.device_sn = m.device_sn inner join cc_gray_plan p on (d.organ_code = p.organ_code and m.pro_name = p.pro_name)  where d.device_sn = ? and  d.organ_code != 'OG-000166'";
        params.add(device_sn);
        return jdbcTemplate.queryForObject(sql, params.toArray(), Integer.class) == 0 ? false : true;
    }

    /**
     * 发送模板消息
     * @param message
     * @return
     */
    public int sendTemplateMessage(String message) {

        String accessToken =  this.getAccessToken("0");

        String url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,accessToken);
        String result = httpPost(url, message);
        JSONObject jsonObject  = JSONObject.parseObject(result);

        if(jsonObject != null && (jsonObject.getInteger("errcode")==40001 || jsonObject.getInteger("errcode")==42001)) {
            accessToken =  this.getAccessToken("1");
            url = THRID_API_URL+String.format(SEND_TEMPLATE_MESSAGE,accessToken);
            result = httpPost(url, message);
            jsonObject = JSONObject.parseObject(result);
        }

        //第二天
        if (!last_time.equals(sdf_date.format(new Date()))) {
            XxlJobLogger.log("[WechatService.sendTemplateMessage][{0}],成功={1},失败={2},取关={3},公众号关闭消息通知={4}", last_time, successNum, failNum, noSubNum, closeSwitchNum);
            successNum = 0;
            failNum = 0;
            noSubNum = 0;
            closeSwitchNum = 0;
            last_time = sdf_date.format(new Date());
        }
        if(StringUtils.equals(jsonObject.getString("errcode"),"0")){
            //XxlJobLogger.log("[WechatService.sendTemplateMessage][发送微信模板消息成功]result:{0}",message);
            successNum++;
            return 1;
        }else if(StringUtils.equals(jsonObject.getString("errcode"),"43004")){
            noSubNum++;
            return 0;
        } else {
            failNum++;
            XxlJobLogger.log("[WechatService.sendTemplateMessage][发送微信模板消息失败]result:{0},message:{1}",result,message);
            return 0;
        }

    }

    /**
     * 获取微信访问token
     * @return
     */
    public String getAccessToken( String cleanCache) {
        Map<String,String> params = new HashMap<>();
        params.put("appid", appid);
        params.put("secret", appsecret);
        params.put("cleanCache", cleanCache);
        String  result = httpGet(THRID_API_URL + ACCESS_TOKEN_URL,params);
        JSONObject json = JSONObject.parseObject(result);
        if(!json.containsKey("access_token") || "".equals(json.getString("access_token"))) {
            XxlJobLogger.log("[WxMassSendMessage.getAccessToken][ERROR]从网络获取access_token失败,result={0},params={1}",result,JSONObject.toJSONString(params));
        }else {
            //XxlJobLogger.log("[WxMassSendMessage.getAccessToken]从网络获取到access_token成功，access_token={0},params={1}",json.getString("access_token"),JSONObject.toJSONString(params));
        }
        return json.getString("access_token");
    }

    public JSONObject toVC_JsonObj(String value,String color){
        JSONObject json = new JSONObject();
        json.put("value", value);
        json.put("color", color);
        return json;
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

    public static String secondToTime(int second){
        int days = second / 86400;            //转换天数
        second = second % 86400;            //剩余秒数
        int hours = second / 3600;            //转换小时
        second = second % 3600;                //剩余秒数
        int minutes = second /60;            //转换分钟
        second = second % 60;                //剩余秒数
        if(days>0){
            return days + "天" + hours + "小时" + minutes + "分" + second + "秒";
        }else{
            return hours + "小时" + minutes + "分" + second + "秒";
        }
    }

    public void createFile() {
    	String xmlstr="aaa";
    	
    }


}













