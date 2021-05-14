package cn.yunovo.job.executor.service.jobhandler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Producer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="datagrab_device_reg_job")
@Component

/*
 * 设备上报数据处理
 * 
 * 
 */
public class DatagrabDeviceRegJobHandler extends IJobHandler {

	private final String QUEUE_NAME = "MAPI_REGISTERED_DEVICE_REG_QUEUE";
	private final String EXCHANGE_NAME = "MAPI";
	private final String TOPIC_NAME = "topic.registeredDeviceReg";
	
	private final String KAFKA_BOOTSTRAP_URL = "192.168.3.241:9092";
	private final String INSERT_SQL = "INSERT INTO cc_device_datagrab_reg (sn,product,total_space,free_space,cpu,cpuz,dpi,package_name,authorized,create_datetime) VALUES (?,?,?,?,?,?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE update_datetime = NOW(),product=?, total_space = ?,free_space = ?,cpu = ?,cpuz = ?,dpi = ?,authorized = ?";
	
	private KafkaProducer<String, String> kafkaProducer = null;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RabbitAdmin rabbitAdmin;

	private boolean isStop = true;

	private SimpleMessageListenerContainer simpleMessageListenerContainer = null;

	@Autowired
	private ConnectionFactory connectionFactory;

	public void initKafka() {
		//初始化kafka 连接
		KafkaProperties kafkaProperties = new KafkaProperties();
		List<String> bootstrapServers = new ArrayList<>();
		
		bootstrapServers.add(KAFKA_BOOTSTRAP_URL); //设置服务器地址
		kafkaProperties.setBootstrapServers(bootstrapServers);
		
		Producer producer = kafkaProperties.getProducer();
		producer.setClientId("MTPDatagrabDeviceRegJobHandler");
		producer.setAcks("1");
		
		this.kafkaProducer = new KafkaProducer<>(kafkaProperties.buildProducerProperties());
	}
	
	public void init() {
		
		XxlJobLogger.log("初始化job");
		
		initKafka();
		
		Queue queue = new Queue(QUEUE_NAME, true, false, false, new HashMap<>());
		rabbitAdmin.declareQueue(queue);
		TopicExchange exchange = new TopicExchange(EXCHANGE_NAME, true, false, new HashMap<>());
		rabbitAdmin.declareExchange(exchange);
		Binding binding = BindingBuilder.bind(queue).to(exchange).with(TOPIC_NAME);
		rabbitAdmin.declareBinding(binding);

		simpleMessageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		simpleMessageListenerContainer.addQueues(queue);
		simpleMessageListenerContainer.setPrefetchCount(3);
		simpleMessageListenerContainer.setConcurrentConsumers(5);
		simpleMessageListenerContainer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
		simpleMessageListenerContainer.setErrorHandler(new ErrorHandler() {

			@Override
			public void handleError(Throwable t) {
				XxlJobLogger.log("任务处理失败，message={0},data={1}", ExceptionUtils.getStackTrace(t),JSONObject.toJSONString(t));

			}
		});
		simpleMessageListenerContainer.setMessageListener(new MessageListenerAdapter() {

			@Override
			public void onMessage(Message message, Channel channel) throws Exception {

				channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

				if (message.getBody() == null || message.getBody().length == 0) {
					XxlJobLogger.log("消息内容为空，message=" + JSONObject.toJSONString(message));
				} else {
					JSONObject task = null;
					task = JSONObject.parseObject(new String(message.getBody(), "utf-8"));
					JSONObject data = task.getJSONObject("data");
					try {
						
						//解析数据
						JSONArray list = parseData(data);
						if(list == null || list.isEmpty()) {
							return;
						}
						//数据入库
						saveDataToDatabase(list);
						//存储至kafka						
						sendDataToKafka(list);
						//}
					} catch (Exception e) {
						
						XxlJobLogger.log("{0} 数据处理失败！errmsg={1},message={2}", QUEUE_NAME, ExceptionUtils.getStackTrace(e),
								task);
					}
						
				}

			}
			
			public final JSONArray parseData(JSONObject data) {
				if(data == null || data.isEmpty()) {
					return null;
				}
				
				JSONArray list = new JSONArray();
				JSONObject baseinfo = (JSONObject) data.clone();
				baseinfo.remove("package_list");
				JSONObject temp = null;
				JSONArray package_list = data.getJSONArray("package_list");
				if(package_list == null || package_list.isEmpty()) {
					return null;
				}
				
				for(int i = 0; i < package_list.size(); i ++) {
					temp = (JSONObject) package_list.getJSONObject(i).clone();
					temp.putAll(baseinfo);
					list.add(temp);
				}
				
				return list;
			}
			
			
			public final void sendDataToKafka(JSONArray data) {
				
				JSONObject temp = null;
				for(int i = 0; i < data.size(); i ++) {
					temp = data.getJSONObject(i);
					try {
						Future<RecordMetadata> future = kafkaProducer.send(new ProducerRecord<String, String>("DatagrabDeviceAuthRegTopic", genId(),temp.toJSONString()));
						future.get();
					} catch (Exception e) {
						
						XxlJobLogger.log("{0}推送数据至kafka失败！errmsg={1},message={2}", QUEUE_NAME, ExceptionUtils.getStackTrace(e),
								temp);
					}
				}
				
			}
			
			public final void saveDataToDatabase(JSONArray data) {
				
				Object[] bData = null;
				List<Object[]> batchArgs = new ArrayList<>(5);
				JSONObject temp = null;
				for(int i = 0; i < data.size(); i ++) {
					temp = data.getJSONObject(i);
					bData = new Object[16];
					bData[0] = temp.getString("sn");
					bData[1] = temp.getString("product");
					bData[2] = temp.getLongValue("total_space");
					bData[3] = temp.getLongValue("free_space");
					bData[4] = temp.getString("cpu");
					bData[5] = temp.getString("cpuz");
					bData[6] = temp.getString("dpi");
					bData[7] = temp.getString("package_name");
					bData[8] = temp.getIntValue("authorized");
					bData[9] = temp.getString("product");
					bData[10] = temp.getLongValue("total_space");
					bData[11] = temp.getLongValue("free_space");
					bData[12] = temp.getString("cpu");
					bData[13] = temp.getString("cpuz");
					bData[14] = temp.getString("dpi");
					bData[15] = temp.getIntValue("authorized");
					
					batchArgs.add(bData);
				}
				jdbcTemplate.batchUpdate(INSERT_SQL, batchArgs);
				
			}
			
		});

		isStop = false;

	}
	
	public String genId() {
		Long m = System.currentTimeMillis();
		return String.valueOf(m * 1000 + RandomUtils.nextInt(100, 999));
	}


	@Override
	public ReturnT<String> execute(String param) throws Exception {

		XxlJobLogger.log("开始执行任务");
		simpleMessageListenerContainer.start();
		while (!isStop) {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				simpleMessageListenerContainer.stop();
				isStop = true;
				XxlJobLogger.log("执行异常,{0},{1}" ,new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()),e.getMessage());
				ReturnT<String> a = new ReturnT<String>();
				a.setCode(ReturnT.FAIL_CODE);
				a.setMsg("执行失败！");
				return a;
				
			}
		}
		// return ReturnT.SUCCESS;
		ReturnT<String> a = new ReturnT<String>();
		a.setCode(200);
		a.setMsg("通知机构任务:" + new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
		return a;
	}

	@Override
	public void destroy() {
		isStop = true;
		simpleMessageListenerContainer.stop();
		XxlJobLogger.log("终止任务" + new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
	}
	
}
