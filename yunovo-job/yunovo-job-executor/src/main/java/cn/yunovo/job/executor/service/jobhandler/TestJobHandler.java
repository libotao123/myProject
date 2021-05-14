package cn.yunovo.job.executor.service.jobhandler;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.util.TextUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Consumer;
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
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

/**
 * 根据上报信息收集设备信息的能力数据
 */
public class TestJobHandler extends IJobHandler {

	private boolean isStop = true;
	
	private ConsumerFactory<Integer, String> consumerFactory = null;

	private KafkaMessageListenerContainer<Integer, String> container  = null;
	
	private final String KAFKA_BOOTSTRAP_URL = "10.18.0.14:9092";
	
	private final String MTP_YIREN_DATA_TOPICS = "eventTopic";
	
	private final String ABLILITY_PACKAGE = "cn.yunovo.nxos";
	private final String ABLILITY_PACKAGE_KUWO = "cn.kuwo.kwmusiccar";
	private final String ABLILITY_PACKAGE_AUTONAVI = "com.autonavi.amapauto";
	private final String ABLILITY_PACKAGE_EDOG = "com.edog.car";
	private final String ABLILITY_PACKAGE_QIYI = "com.qiyi.video.pad";
	
	private final String ABLILITY_PACKAGE_SETTING = "cn.yunovo.car.settings";
	private final String ABLILITY_PACKAGE_WX = "com.spt.carengine.wechat";
	private final String ABLILITY_PACKAGE_MANAGER = "cn.yunovo.car.manager";
	private final String ABLILITY_PACKAGE_CONFIG = "cn.yunovo.config";
	
	
	
	@Autowired
	private JdbcTemplate jt;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
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
		customer.setAutoOffsetReset("latest"); //如果客户端未提交offset 则从最早开始消费
		customer.setClientId("MTP-DOICOLLECTIONEVENTWITHKAFKA-DATA-01"); //如果设置clientId则客户端为单线程消费 
		customer.setEnableAutoCommit(false); //设置非自动提交
		customer.setGroupId("MTP-DOICOLLECTIONEVENTWITHKAFKA");
		//创建customerFactory 
		consumerFactory = new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
		ContainerProperties containerProps  = new ContainerProperties(MTP_YIREN_DATA_TOPICS);
		containerProps.setAckMode(AbstractMessageListenerContainer.AckMode.MANUAL); //设置手工ack
		containerProps.setMessageListener(new AcknowledgingMessageListener<Integer, String>() {

			@Override
			public void onMessage(ConsumerRecord<Integer, String> data, Acknowledgment acknowledgment) {
				acknowledgment.acknowledge();
				String value = data.value();
				
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
				List<JSONObject> reportData = parseData(array);
				if(!reportData.isEmpty()) {
				    // delDateBysn(reportData.get(0).getString("device_sn"));
					insertDB(reportData);
				    
				}
				reportData = null;
			}

            
			
		});
		containerProps.setErrorHandler(new ErrorHandler() {
			
			@Override
			public void handle(Exception thrownException, ConsumerRecord<?, ?> data) {
				XxlJobLogger.log("[ErrorHandler]msg={0},data={1},exception={2}", "数据消费失败", String.valueOf(data.value()), ExceptionUtils.getStackTrace(thrownException));
			}
		});
		container = new KafkaMessageListenerContainer<>(this.consumerFactory, containerProps);
		container.setAutoStartup(false);
	}
	
   private void delDateBysn(String sn)
    {
        String sql = " DELETE  FROM `ocp_doi_devices_ability` WHERE devices_sn = '"+sn.trim()+"'";
        jt.update(sql);
        
    }
	
	private List<JSONObject> parseData(JSONArray data){
		
		JSONObject reportData = null;
		List<JSONObject> list = new ArrayList<>();
		JSONObject deviceReport = new JSONObject();
		for(int i = 0; i < data.size(); i ++) {
			reportData = data.getJSONObject(i);
			
			if ("apk_list".equals(reportData.get("event"))) {
				JSONObject attributes = reportData.getJSONObject("attributes");
				JSONArray json = (JSONArray)attributes.get("apk_list");
				String device_sn = reportData.getString("did");
				Date date = new Date(reportData.getLong("time"));
				for (int j = 0;j<json.size();j++)
                {
				    deviceReport = json.getJSONObject(j);
				    if(deviceReport.containsKey("package_name")) {
				        String packageName = deviceReport.get("package_name").toString().trim();
				        if(packageName.contains(ABLILITY_PACKAGE) || packageName.equals(ABLILITY_PACKAGE_KUWO) ||  packageName.equals(ABLILITY_PACKAGE_AUTONAVI) || packageName.equals(ABLILITY_PACKAGE_EDOG) || packageName.equals(ABLILITY_PACKAGE_QIYI) || packageName.equals(ABLILITY_PACKAGE_SETTING) || packageName.equals(ABLILITY_PACKAGE_WX) || packageName.equals(ABLILITY_PACKAGE_MANAGER)  || packageName.equals(ABLILITY_PACKAGE_CONFIG)    ) 
				        {
				            String version = deviceReport.get("version_name").toString().trim();
				            int len = version.indexOf("_");
				            String str_ver = version;
				            if(len != -1) {
				                str_ver = version.substring(0, version.indexOf("_"));
				            }
				            Long verCode = getNum(str_ver);
				            if(verCode != 0) {
				                deviceReport.put("device_sn", device_sn);
				                deviceReport.put("time", date);
				                deviceReport.put("version_code", verCode);
				                deviceReport.put("version_name", str_ver);
				                deviceReport.put("package_name", packageName);
				                
				                // 新加屏幕分辨率手机
				                deviceReport.put("resolution_ratio", "-1");
				                list.add(deviceReport);
				            }
				        }
				    }
                }
				
				continue;
			}else if("device_info".equals(reportData.get("event"))) {
			    String device_sn = reportData.getString("did");
			    JSONObject attributes = reportData.getJSONObject("attributes");
			    String resolution_ratio = attributes.get("resolution_ratio").toString();
			    
			    Date date = new Date(reportData.getLong("time"));
			    deviceReport.put("time", date);
			    deviceReport.put("device_sn", device_sn);
			    deviceReport.put("version_code", "-1");
                deviceReport.put("version_name", "-1");
                deviceReport.put("package_name", "resolution");
                deviceReport.put("apk_name", "resolution");
                deviceReport.put("resolution_ratio", resolution_ratio);
                list.add(deviceReport);
			}else if("theme_usinginfo".equals(reportData.get("event"))) {
                String device_sn = reportData.getString("did");
                JSONObject attributes = reportData.getJSONObject("attributes");
                String theme_id = attributes.get("theme_id").toString().trim();
                
                Date date = new Date(reportData.getLong("time"));
                deviceReport.put("time", date);
                deviceReport.put("device_sn", device_sn);
                deviceReport.put("version_code", theme_id);
                deviceReport.put("version_name", "-1");
                deviceReport.put("package_name", "theme_id");
                deviceReport.put("apk_name", "theme_id");
                deviceReport.put("resolution_ratio", "-1");
                list.add(deviceReport);
            }
			
			
			
			
		}
		
		return list;
	}
	
	private int[] insertDB(List<JSONObject> reportData)
    {
	    final List<JSONObject> tempRep = reportData;
        // 查出当前sn已经含有的package
        String select_pack_sql = " SELECT  package_name,component_version_code FROM ocp_doi_devices_ability WHERE devices_sn =  ? ";
        List<Object> params = new ArrayList<>();
        params.add(reportData.get(0).getString("device_sn").trim());
        List<Integer> hashcode_list = new ArrayList<>();
        List<String> pack_name_list = new ArrayList<>();
        jt.query(select_pack_sql, new RowMapper() {

            @Override
            public Object mapRow(ResultSet rs, int rowNum)
                throws SQLException
            {
                pack_name_list.add(rs.getString("package_name").trim());
                hashcode_list.add((rs.getString("package_name").trim()+"#"+rs.getString("component_version_code").trim()).hashCode());
                return null;
            }
            
        },params.toArray());
//      String sql = " INSERT INTO `clw`.`ocp_doi_devices_ability` (`devices_sn`, `time`, `component_name`, `component_version`, `component_version_code`, `package_name`,`create_by`, `update_by`, `create_datetime`, `update_datetime`,`extra_info`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now(),? )   ON DUPLICATE KEY  UPDATE `devices_sn` = ?, `time`=?,`component_name`=?,`component_version`=?,`component_version_code`=?,  `package_name`= ?,`update_by` = ?,`update_datetime` = now() ,extra_info = ? ";
	
     	//XxlJobLogger.log("[YrenDataJob.insertDB]插入sn:"+reportData.get(0).getString("device_sn").trim());
       // XxlJobLogger.log("[YrenDataJob.insertDB]插入pack_name_list:"+pack_name_list.toString());
      //  XxlJobLogger.log("[YrenDataJob.insertDB]插入hashcode_list:"+hashcode_list.toString());
      
      
        String updateSql = "  UPDATE  `clw`.`ocp_doi_devices_ability`  set  `time`=?,`component_name`=?,`component_version`=?,`component_version_code`=?,`update_datetime` = now() ,extra_info = ?     WHERE devices_sn = ? AND package_name = ?   ";
        String insertSQL = " INSERT INTO `clw`.`ocp_doi_devices_ability` (`devices_sn`, `time`, `component_name`, `component_version`, `component_version_code`, `package_name`,`create_by`, `update_by`, `create_datetime`, `update_datetime`,`extra_info`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now(),? ) ";
        List<Object> update_params = new ArrayList<>();
        for(int i = 0; i< tempRep.size();i++) {
            JSONObject json =  tempRep.get(i);
            String package_name  = json.getString("package_name").trim();
            Integer strHash = (package_name+"#"+json.getString("version_code").trim()).hashCode();
          	
         // 	XxlJobLogger.log("[YrenDataJob.insertDB]插入,for循环中的packagename:"+package_name);
          //  XxlJobLogger.log("[YrenDataJob.insertDB]插入,for循环中的packagename对应的hashcode:"+strHash);
          
            if(pack_name_list.contains(package_name)) {
                // 如果包名是分辨率话不用去修改
                if("resolution".equals(package_name)) {
                    break ;
                }
                if(!hashcode_list.contains(strHash)) {
                    update_params.clear();
                    update_params.add(json.getString("time"));
                    update_params.add(json.getString("apk_name"));
                    update_params.add(json.getString("version_name"));
                    update_params.add(json.getString("version_code"));
                    if("resolution".equals(package_name)) {
                        update_params.add(json.getString("resolution_ratio"));
                    }else {
                        update_params.add("-1");
                    }
                    update_params.add(json.getString("device_sn"));
                    update_params.add(json.getString("package_name"));
                    jt.update(updateSql, update_params.toArray());
                }
                
            }else {
                update_params.clear();
                update_params.add(json.getString("device_sn"));
                update_params.add(json.getString("time"));
                update_params.add(json.getString("apk_name"));
                update_params.add(json.getString("version_name"));
                update_params.add(json.getString("version_code"));
                update_params.add(json.getString("package_name"));
                update_params.add("JOB定时更新");
                update_params.add("JOB定时更新");
                
                // 额外添加的字段，存我们需要的数据，暂时存的是分辨率
                if("resolution".equals(package_name)) {
                    update_params.add(json.getString("resolution_ratio"));
                }else {
                    update_params.add("-1");
                }
                
                jt.update(insertSQL, update_params.toArray());
            }
        }
        
        return null;
        
    }
	
	
	
	

	@Override
	public ReturnT<String> execute(String param) throws Exception {
		
		XxlJobLogger.log("[DOICollectionEventWithKafkaJobHandler.execute]msg={0}", "kafka 开始消费,任务开始执行");
		container.start();
		
		while(!isStop) {
			Thread.sleep(1000);
		}
		XxlJobLogger.log("[DOICollectionEventWithKafkaJobHandler.execute]msg={0}", "kafka 任务执行结束");
		// return ReturnT.SUCCESS;
		ReturnT<String> a = new ReturnT<String>();
		a.setCode(200);
		a.setMsg("执行弈人任务:" + new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
		return a;
	}
	
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
		XxlJobLogger.log("[DOICollectionEventWithKafkaJobHandler.destroy]msg={0}", "执行销毁操作");
		container.stop();
		XxlJobLogger.log("[DOICollectionEventWithKafkaJobHandler.destroy]msg={0}", "kafka停止消费");
		isStop = true;
		XxlJobLogger.log("[DOICollectionEventWithKafkaJobHandler.destroy]msg={0}", "终止任务");
	}

	
	private Long getNum(String version) {
	    try
        {
	        StringBuilder sb = new StringBuilder();
	        String[] strNum = version.split("\\.");
	        
	        for(int i = 0; i< strNum.length;i++) {
	            String t = strNum[i];
	            if(t.length() == 1) {
	                sb.append("0"+t);
	            }else {
	                sb.append(t);
	            }
	        }
	        return Long.valueOf(sb.toString());
        }
        catch (Exception e)
        {
             return 0l;
        }
        
    }
	
	
	 
	    /**
	     * 去除字符串前后的空格, 包括半角空格和全角空格(中文)等各种空格, java的string.trim()只能去英文半角空格
	     * @param str
	     */
	    public  String trim(String str) {
	        if (TextUtils.isEmpty(str)) {
	            return str;
	        }
	 
	        char[] val = str.toCharArray();
	        int st = 0;
	        int len=val.length;
	        while ((st < len) && isSpace(val[st])) {
	            st++;
	        }
	        while ((st < len) && isSpace(val[len - 1])) {
	            len--;
	        }
	        return ((st > 0) || (len < val.length)) ? str.substring(st, len) : str;
	    }
	 
	    public static boolean isSpace(char aChar) {
	         
            return aChar == SPACE_32 || aChar == SPACE_12288 || aChar == SPACE_160 || aChar == SPACE_8194 || aChar == SPACE_8195 || aChar == SPACE_8197 || aChar == SPACE_8201;
	    }
	

	    /**普通的英文半角空格Unicode编码*/
	    private static final int SPACE_32 = 32;
	 
	    /**中文全角空格Unicode编码(一个中文宽度)*/
	    private static final int SPACE_12288 = 12288;
	 
	    /**普通的英文半角空格但不换行Unicode编码(==   ==   == no-break space)*/
	    private static final int SPACE_160 = 160;
	 
	    /**半个中文宽度(==   == en空格)*/
	    private static final int SPACE_8194 = 8194;
	 
	    /**一个中文宽度(==   == em空格)*/
	    private static final int SPACE_8195 = 8195;
	 
	    /**四分之一中文宽度(四分之一em空格)*/
	    private static final int SPACE_8197 = 8197;
	 
	    /**窄空格*/
	    private static final int SPACE_8201 = 8201;
}




