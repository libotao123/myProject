package cn.yunovo.job.executor.service.jobhandler;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

/**
 * 根据上报信息记录新增装机设备和设备活跃log
 */
public class TestJobHandler2 extends IJobHandler {

	private boolean isStop = true;
	
	private ConsumerFactory<Integer, String> consumerFactory = null;

	private KafkaMessageListenerContainer<Integer, String> container  = null;
	
	private final String KAFKA_BOOTSTRAP_URL = "10.18.0.14:9092";
	
	private final String MTP_YIREN_DATA_TOPICS = "eventTopic";
	
	private final long diff_time = 43200000;//12*3600*1000,12H
	
	private final long one_day_time = 86400000;//一天的毫秒数
	
	private final long max_time = 7776000000L;//90*86400000,90天
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	String getLastPoint_url = "http://lsapi.prd.yunovo.cn/rest/api/trace/getLastPointByDid?did=%s";
	
	/**根据经纬度获取地理位置*/
	String getLocation_url = "http://isapi.prd.yunovo.cn/rest/api/lbs/getLocation?longitude=%s&latitude=%s&coordtype=%s";
	
    // yundash配置
    private String yundash_url = "http://yundash.yunovo.cn";
    private String yundash_api_key = "EKUtEeFlJwBwbTM2O9fRglcoMycHLR1M2MjX5vZ9";
    private String query_id1 = "1045";
    private String query_id2 = "1050";
	
	@Autowired
    public RestTemplate restTemplate;
	
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
		customer.setClientId("MTP-USERMARK-REPORTING-DATA-01"); //如果设置clientId则客户端为单线程消费
		customer.setEnableAutoCommit(false); //设置非自动提交
		customer.setGroupId("MTP-USERMARK");
		//创建customerFactory 
		consumerFactory = new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
		ContainerProperties containerProps  = new ContainerProperties(MTP_YIREN_DATA_TOPICS);
		containerProps.setAckMode(AbstractMessageListenerContainer.AckMode.MANUAL); //设置手工ack
		containerProps.setMessageListener(new AcknowledgingMessageListener<Integer, String>() {

			@Override
			public void onMessage(ConsumerRecord<Integer, String> data, Acknowledgment acknowledgment) {
//				acknowledgment.acknowledge();
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
				JSONArray array = null;
				try {
					array = JSONArray.parseArray(temp);
				} catch (Exception e) {
					//XxlJobLogger.log("[Job.temp]参数打印，temp={0},temp={1}",temp,getBody(value));
				}
				//保存数据库
				if(array == null || array.isEmpty()) {
					return ;
				}
				JSONObject reportData = parseData(array);
				if(reportData != null && !reportData.isEmpty()) {
					if (reportData.getInteger("device_state") == 1 && checkIsInstallCar(reportData.getString("device_sn"))<1) {
						try {
							String device_sn = reportData.getString("device_sn");
							boolean last_location_flag = false;
							long origin_time;
							//原始上报的装车时间
							long load_time = reportData.getLongValue("time");
							long curr_time = System.currentTimeMillis();
							
							//装车时间定义：当前时间与上报的装车时间相差90天以上则用当前时间作为装机时间
							if(Math.abs(curr_time-load_time) > max_time) {
//								XxlJobLogger.log("[AddDeviceInstallCarJob][getLocation]当前时间与上报的装车时间超过90天，device_sn={0},curr_time={1},load_time={2},相差天数={3}",
//										device_sn,curr_time,load_time,(curr_time-load_time)/one_day_time);
								//记录原始上报的装车时间
								origin_time = load_time;
								reportData.put("origin_time", origin_time);
								//当前时间设为装车时间
								load_time = curr_time;
								reportData.put("time", load_time);
							}
							
							//装车位置定义：装机时间与设备最后位置（gpspoints）时间差未超过12H，则取最后位置作为装车位置；否则查gpstraces（轨迹表），取与装车时间最近（12H内）的一次轨迹起始点
							String result = getLastGpspoints(device_sn);
							if(StringUtils.isNotBlank(result)) {
								JSONObject json = JSONObject.parseObject(result).getJSONObject("data");
								if(json != null && json.containsKey("point")) {
									long last_time = json.getLongValue("timestamp");
									
									//装机时间与设备最后位置时间差未超过12H，则取最后位置作为装车位置；
									if(Math.abs(last_time-load_time) <= diff_time) {
										last_location_flag = true;
										
										String[] point_arr = json.getString("point").split(",");
										//根据经纬度调cdp的接口获取地址详情
										JSONObject location_obj = getLocation(Double.valueOf(point_arr[0]), Double.valueOf(point_arr[1]), json.getInteger("ver"), device_sn);
										if(location_obj != null && location_obj.size() > 0) {
											reportData.put("province", location_obj.getString("province"));
											reportData.put("city", location_obj.getString("city"));
											reportData.put("district", location_obj.getString("district"));
											reportData.put("location", location_obj.getString("location"));
	                                      	reportData.put("point", point_arr[0] + "," + point_arr[1] + "," + json.getInteger("ver"));
										}
									}else {
										XxlJobLogger.log("[AddDeviceInstallCarJob][装机时间与设备最后位置点时间差超过12H]，device_sn={0},load_time={1},last_time={2}",device_sn,load_time,last_time);
									}
								}
							}
							
							//查轨迹获取位置
							if(!last_location_flag) {
								//否则查gpstraces，取与装车时间最近的一次轨迹起始点（12H内）
								long start_at1 = -1;
								long start_at2 = -1;
								
								Map<String, Object> param = new HashMap<>();
								param.put("did", device_sn);
								param.put("time_added", load_time);
								
								//查询轨迹表（gpstraces）取经纬度(points),start_at < time_added <= end_at
								JSONArray arr1 = queryMainYundash(param, query_id1);
								if(arr1 != null && arr1.size() > 0) {
									start_at1 = arr1.getJSONObject(0).getLongValue("timestamp");
									//判断轨迹时间是否与装车时间在12H内
									if(Math.abs(start_at1-load_time) > diff_time) {
										XxlJobLogger.log("[AddDeviceInstallCarJob.queryMainYundash]轨迹时间1与装车时间差值大于12H，device_sn={0},load_time={1},start_at1={2}",device_sn,load_time,start_at1);
                                      	start_at1 = -1;
									}
								}
								JSONArray arr2 = queryMainYundash(param, query_id2);
								if(arr2 != null && arr2.size() > 0) {
									start_at2 = arr2.getJSONObject(0).getLongValue("timestamp");
									//判断轨迹时间是否与装车时间在12H内
									if(Math.abs(start_at2-load_time) > diff_time) {
										XxlJobLogger.log("[AddDeviceInstallCarJob.queryMainYundash]轨迹时间2与装车时间差值大于12H，device_sn={0},load_time={1},start_at2={2}",device_sn,load_time,start_at2);
                                      	start_at2 = -1;
									}
								}
								
								JSONArray arr = null;
								if(start_at1 == -1 && start_at2 == -1) {
									XxlJobLogger.log("[AddDeviceInstallCarJob.queryMainYundash]yundash查询数据为空，device_sn={0}",device_sn);
								}else if(start_at1 == -1 && start_at2 != -1) {
									arr = arr2;
								}else if(start_at1 != -1 && start_at2 == -1) {
									arr = arr1;
								}else if(start_at1 != -1 && start_at2 != -1) {
									if(Math.abs(load_time - start_at1) < Math.abs(start_at2 - load_time)) {
										arr = arr1;
									}else {
										arr = arr2;
									}
								}
								if(arr != null && arr.size() > 0) {
									JSONObject obj = arr.getJSONObject(0);
									JSONArray points_arr = obj.getJSONArray("points");
									String point = points_arr.getString(0);
									double longitude = Double.valueOf(point.split(",")[0]);
									double latitude = Double.valueOf(point.split(",")[1]);
									int ver = obj.getInteger("ver");
									
									//调cdp获取位置信息
									JSONObject loction_json = getLocation(longitude, latitude, ver);
									if(loction_json != null && loction_json.size() > 0) {
										reportData.put("province", loction_json.getString("province"));
										reportData.put("city", loction_json.getString("city"));
										reportData.put("district", loction_json.getString("district"));
										reportData.put("location", loction_json.getString("formatted_address"));
									}
								}
							}
						} catch (Exception e) {
							XxlJobLogger.log("[AddDeviceInstallCarJob][ERROR]获取位置信息异常，device_sn={0}",reportData.getString("device_sn"));
						}
						
						//XxlJobLogger.log("[Job.reportData]参数打印，reportData={0}",JSONObject.toJSONString(reportData));
						addDeviceInstallCar2(reportData);
					}
					//try {
						//if (checkIsDayReportLog(reportData.getString("device_sn"), sdf.parse(sdf.format(reportData.getDate("time")))) < 1) {
							//addDeviceReportLog(reportData);
						//}
					//} catch (ParseException e) {
						//XxlJobLogger.log("[YrenDataJob.onMessage]日期转换错误={0}", ExceptionUtils.getStackTrace(e));
					//}
				}
				reportData = null;
				acknowledgment.acknowledge();
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
	
	private JSONObject parseData(JSONArray data){
		
		JSONObject reportData = null;
		JSONObject deviceReport = new JSONObject();
		for(int i = 0; i < data.size(); i ++) {
			reportData = data.getJSONObject(i);
			if ("usermark_info".equals(reportData.get("event"))) {
				deviceReport.put("device_sn", reportData.getString("did"));
				deviceReport.put("time", reportData.getLong("time"));
				JSONObject attributes = reportData.getJSONObject("attributes");
				deviceReport.put("device_state", attributes.getInteger("usermark_state")) ;//是否车主
//				deviceReport.put("device_iccid", attributes.containsKey("\$iccid") ? attributes.getString("\$iccid") : "");
				deviceReport.put("device_iccid", attributes.containsKey("$iccid") ? attributes.getString("$iccid") : "");
				deviceReport.put("organ_code", reportData.getString("project"));//机构
				
				continue;
			}
		}
		
		return deviceReport;
	}
	
	/**
	 * 根据经纬度调cdp的接口获取地址详情
	 */
	public JSONObject getLocation(double longitude, double latitude, int ver) {
		JSONObject  resut = new JSONObject();
		String result_location = null;
		try {
			result_location = restTemplate.getForObject(String.format(getLocation_url,longitude,latitude,(ver==1?"bd09ll":ver==2?"gcj02ll":"")), String.class);
		} catch (Exception e) {
			XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocation][ERROR]根据经纬度调cdp接口获取地理位置异常，longitude={0},latitude={1},result_location={2}",
					longitude,latitude,result_location);
		}
		JSONObject json = JSONObject.parseObject(result_location);
		if(json != null && json.containsKey("code") && json.getInteger("code") == 0) {
			JSONObject  data = json.getJSONObject("data");
			resut.put("formatted_address", data.getString("formatted_address"));
			/*resut.put("sematic_description", data.getString("sematic_description"));
			resut.put("cityCode", data.getInteger("cityCode"));*/
			
			JSONObject  addressComponent = json.getJSONObject("data").getJSONObject("addressComponent");
			resut.put("province", addressComponent.getString("province"));
			resut.put("city", addressComponent.getString("city"));
			resut.put("district", addressComponent.getString("district"));
			/*resut.put("street", addressComponent.getString("street"));
			resut.put("city_level", addressComponent.getInteger("city_level"));
			resut.put("country", addressComponent.getString("country"));
			resut.put("adcode", addressComponent.getString("adcode"));
			resut.put("country_code", addressComponent.getInteger("country_code"));*/
		} else {
			XxlJobLogger.log("[UpdateUsermarkLocationJob.getLocation][WARN]根据经纬度调cdp接口获取地理位置失败，longitude={0},latitude={1},result_location={2}",
					longitude,latitude,result_location);
		} 
		return resut;
	}
	
	/**
     * 主查询
     * @return
     */
    public JSONArray queryMainYundash (Map<String, Object> param, String query_id) throws Exception {
        String param_url = "";
        Set<String> set = param.keySet();
        for (String key:set) {
            param_url += "&p_" + key + "=" + param.get(key);
        }
        JSONObject refresh_result = null;
        String result_url = String.format(yundash_url+"/api/queries/%s/refresh?api_key=%s", query_id,yundash_api_key) + param_url;
        refresh_result = refreshYundash(result_url);
        String id = refresh_result.getJSONObject("job").getString("id");
        Integer query_result_id = getYundashJobId(yundash_url+"/api/jobs/%s?api_key=%s",id,1);
        if (query_result_id == -1) {
            XxlJobLogger.log("yundash查询超时");
            return null;
        }
        JSONArray doc_count_arr = getYundashQueryResultId(yundash_url+"/api/query_results/%s?api_key=%s",query_result_id);
        return doc_count_arr;
    }
    
    /**
     * 刷新yundash
     */
    public JSONObject refreshYundash(String url){
        HttpHeaders headers = new HttpHeaders();
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        headers.setContentType(type);
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        String result = restTemplate.postForObject(url, headers, String.class);
        return JSONObject.parseObject(result);
    }
    
    /**
     * 获取yundash的jobid 
     */
    public int getYundashJobId(String url, String id, int index) throws InterruptedException{
        String result = restTemplate.getForObject(String.format(url, id,yundash_api_key), String.class);
        JSONObject job_result = JSONObject.parseObject(result);
        if (null != job_result.getJSONObject("job").getInteger("query_result_id")) {
            return job_result.getJSONObject("job").getInteger("query_result_id");
        } else {
            if (index > 200) {
                return -1;
            }
            Thread.sleep(300);
            index++;
            return getYundashJobId(url, id, index);
        }
    }
    
    /**
     * 根据查询id获取yundash数据 
     */
    public JSONArray getYundashQueryResultId(String url,Integer query_result_id) {
        String result = restTemplate.getForObject(String.format(url, query_result_id,yundash_api_key), String.class);
        JSONObject res = JSONObject.parseObject(result);
        return res.getJSONObject("query_result").getJSONObject("data").getJSONArray("rows");
    }
	
	/**
	 * 查询device是否已经装车
	 */
	public int checkIsInstallCar(String device_sn) {
		List<Object> params = new ArrayList<>(); 
		String sql = "SELECT count(1) FROM cc_device_event_usermark where device_sn = ?";
		params.add(device_sn);
		return jdbcTemplate.queryForObject(sql.toString(),params.toArray(), Integer.class);
	}
	
	/**
	 * 插入设备装车记录
	 */
	public int addDeviceInstallCar(JSONObject reportData) {
		Object[] params = new Object[5];
		String sql = "INSERT INTO cc_device_event_usermark(device_sn,device_iccid,time_added,device_state,organ_code,created_time,updated_time) VALUES(?, ?, ?, ?, ?, now(), now())";
		params[0] = reportData.getString("device_sn");
		params[1] = reportData.getString("device_iccid");
		params[2] = reportData.getDate("time");
		params[3] = reportData.getInteger("device_state");
		params[4] = reportData.getString("organ_code");
		return jdbcTemplate.update(sql, params);
	}
	
	/**
	 * 插入设备装车记录2
	 */
	public int addDeviceInstallCar2(JSONObject reportData) {
		Object[] params = new Object[10];
		String sql = "INSERT INTO cc_device_event_usermark(device_sn,device_iccid,time_added,device_state,organ_code,created_time,updated_time,province,city,district,location,origin_time) VALUES(?, ?, ?, ?, ?, now(), now(),?,?,?,?,?)";
		params[0] = reportData.getString("device_sn");
		params[1] = reportData.getString("device_iccid");
		params[2] = reportData.getDate("time");
		params[3] = reportData.getInteger("device_state");
		params[4] = reportData.getString("organ_code");
		params[5] = reportData.getString("province");
		params[6] = reportData.getString("city");
		params[7] = reportData.getString("district");
		params[8] = reportData.getString("location");
		params[9] = reportData.getDate("origin_time");
		return jdbcTemplate.update(sql, params);
	}
	
	/**
	 * 查询设备最后位置
	 */
	public String getLastGpspoints(String did) {
		String result_location = null;
		try {
			result_location = restTemplate.getForObject(String.format(getLastPoint_url,did), String.class);
		} catch (Exception e) {
			XxlJobLogger.log("[AddDeviceInstallCarJob.getLastGpspoints][ERROR]调ls获取设备最后经纬度失败，did={0}",did);
		}
		return result_location;
	}
	
	/**
	 * 根据经纬度调cdp的接口获取地址详情
	 */
	public JSONObject getLocation(double longitude, double latitude, int ver, String device_sn) {
		JSONObject  resut = new JSONObject();
		String result_location = null;
		try {
			result_location = restTemplate.getForObject(String.format(getLocation_url,longitude,latitude,(ver==1?"bd09ll":ver==2?"gcj02ll":"")), String.class);
		} catch (Exception e) {
			XxlJobLogger.log("[AddDeviceInstallCarJob.getLocation][ERROR]根据经纬度调cdp的接口获取地址详情失败，longitude={0},latitude={1},ver={2},device_sn={3}",
					longitude,latitude,ver,device_sn);
		}
		JSONObject json = JSONObject.parseObject(result_location);
		if(json != null && json.containsKey("code") && json.getInteger("code") == 0) {
			JSONObject  data = json.getJSONObject("data");
			resut.put("location", data.getString("formatted_address"));
			
			JSONObject  addressComponent = json.getJSONObject("data").getJSONObject("addressComponent");
			resut.put("province", addressComponent.getString("province"));
			resut.put("city", addressComponent.getString("city"));
			resut.put("district", addressComponent.getString("district"));
		} else {
			XxlJobLogger.log("[AddDeviceInstallCarJob.getLocation][WARN]根据经纬度调cdp接口获取地理位置失败，longitude={0},latitude={1},ver={2},device_sn={3},result_location={4}",
					longitude,latitude,ver,device_sn,result_location);
		} 
		return resut;
	}
	
	/**
	 * 查询device今天是否已有上报记录
	 */
	public int checkIsDayReportLog(String device_sn, Date date_added) {
		List<Object> params = new ArrayList<>(); 
		String sql = "SELECT count(1) FROM cc_device_event_usermark_history where device_sn = ? and date_added = ?";
		params.add(device_sn);
		params.add(date_added);
		return jdbcTemplate.queryForObject(sql.toString(),params.toArray(), Integer.class);
	}
	
	/**
	 * 插入device上报日志
	 */
	public int addDeviceReportLog(JSONObject reportData) {
		Object[] params = new Object[4];
		String sql = "INSERT INTO cc_device_event_usermark_history(device_sn,device_iccid,date_added,organ_code,created_time) VALUES(?, ?, ?, ?, now())";
		params[0] = reportData.getString("device_sn");
		params[1] = reportData.getString("device_iccid");
		params[2] = reportData.getDate("time");
		params[3] = reportData.getString("organ_code");
		return jdbcTemplate.update(sql, params);
	}

	@Override
	public ReturnT<String> execute(String param) throws Exception {
		
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
	}

}

