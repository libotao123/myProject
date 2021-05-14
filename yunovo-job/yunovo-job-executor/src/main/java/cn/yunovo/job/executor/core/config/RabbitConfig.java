package cn.yunovo.job.executor.core.config;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	public static final String MAPI_NOTIFY_ORG_QUEUE = "MAPI_NOTIFY_ORG_QUEUE";
	public static final String MAPI_TRACE_QUEUE = "MAPI_TRACE_QUEUE";
	public static final String MAPI_EXEC_SQL_QUEUE = "MAPI_EXEC_SQL_QUEUE";
	
	public static final String MAPI_Exchange = "MAPI";
	
	
	public static final String MAPI_NOTIFY_ORG_TOPIC = "topic.notifyOrg";
	public static final String MAPI_TRACE_TOPIC  = "topic.trace";
	public static final String MAPI_EXEC_SQL_TOPIC  = "topic.execSql";
	
	@Value("${rabbitmq.host}")
	private String host;
	@Value("${rabbitmq.port}")
	private String port;
	@Value("${rabbitmq.username}")
	private String username;
	@Value("${rabbitmq.password}")
	private String password;
	@Value("${rabbitmq.virtualHost}")
	private String virtualHost;
	@Value("${rabbitmq.concurrentConsumers}")
	private Integer concurrentConsumers;
	@Value("${rabbitmq.maxConcurrentConsumers}")
	private Integer maxConcurrentConsumers;
	
	@Bean(name = "connectionFactory")
	 public ConnectionFactory connectionFactory() {
			
		 CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		 connectionFactory.setUsername(username);
		 connectionFactory.setPassword(password);
		 connectionFactory.setVirtualHost(virtualHost);
		 connectionFactory.setPublisherConfirms(true);
		 connectionFactory.setAddresses(host);
		 return connectionFactory;
	 }
	
	@Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
		
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        
        //factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }
	
	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		
		return new RabbitAdmin(connectionFactory);
	}

	
//    @Bean
//    public Queue queueNotifyOrg() {
//        return new Queue(RabbitConfig.MAPI_NOTIFY_ORG_QUEUE);
//    }
//
//    @Bean
//    public Queue queueTrace() {
//        return new Queue(RabbitConfig.MAPI_TRACE_QUEUE);
//    }
//
//    @Bean
//    public Queue queueExecSql() {
//        return new Queue(RabbitConfig.MAPI_EXEC_SQL_QUEUE);
//    }
//
//    /**
//     * 交换机(Exchange) 描述：接收消息并且转发到绑定的队列，交换机不存储消息
//     */
//    @Bean
//    TopicExchange yunovoMapiapiExchange() {
//        return new TopicExchange(RabbitConfig.MAPI_Exchange);
//    }
//
//    //綁定队列 queueNotifyOrg() 到 yunovoMapiapiExchange 交换机,路由键只接受完全匹配 topic.message 的队列接受者可以收到消息
//    @Bean
//    Binding bindingExchangeMessage(Queue queueNotifyOrg, TopicExchange yunovoMapiapiExchange) {
//        return BindingBuilder.bind(queueNotifyOrg).to(yunovoMapiapiExchange).with(RabbitConfig.MAPI_NOTIFY_ORG_TOPIC);
//    }
//
//    //綁定队列 queueTrace() 到 yunovoMapiapiExchange 交换机,路由键只要是以 topic.message 开头的队列接受者可以收到消息
//    @Bean
//    Binding bindingExchangeMessages(Queue queueTrace, TopicExchange yunovoMapiapiExchange) {
//        return BindingBuilder.bind(queueTrace).to(yunovoMapiapiExchange).with(RabbitConfig.MAPI_TRACE_TOPIC);
//    }
//
//    //綁定队列 queueExecSql() 到 yunovoMapiapiExchange 交换机,路由键只要是以 topic 开头的队列接受者可以收到消息
//    @Bean
//    Binding bindingExchangeYmq(Queue queueExecSql, TopicExchange yunovoMapiapiExchange) {
//        return BindingBuilder.bind(queueExecSql).to(yunovoMapiapiExchange).with(RabbitConfig.MAPI_EXEC_SQL_TOPIC);
//    }

	
}
