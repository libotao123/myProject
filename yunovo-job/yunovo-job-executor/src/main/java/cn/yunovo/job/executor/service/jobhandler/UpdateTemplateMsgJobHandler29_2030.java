package cn.yunovo.job.executor.service.jobhandler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="update_template_msg_job_lbt2")
@Component

/*
 * 更新为正常的模板消息29_2030
 * 
 * 
 */
public class UpdateTemplateMsgJobHandler29_2030 extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UpdateTemplateMsgJobHandler29_2030.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			XxlJobLogger.log("[entry1");
			updateTemplateMsg();
			XxlJobLogger.log("[entry2");
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[UpdateTemplateMsgJobHandler29_2030.updateTemplateMsg][更新通用模板消息异常]");
            XxlJobLogger.log("[UpdateTemplateMsgJobHandler29_2030.updateTemplateMsg][更新通用模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		
		return a;
    }
	
	//更新模板消息
	public void updateTemplateMsg() throws Exception {
		String sql1="";
		String value1 = "{\"first\":{\"value\":\"糟糕！套餐内流量已不足。流量用尽后，小达将无法为您提供远程定位、实时导航等服务。\\n\",\"color\":\"#FF2400\"},\\n\"keyword1\":{\"value\":\"${iccid}\",\"color\":\"#000001\"},\\n\"keyword2\":{\"value\":\"${balance}M\",\"color\":\"#000001\"},\\n\"keyword3\":{\"value\":\"${time}\",\"color\":\"#000001\"}\\n,\"keyword4\":{\"value\":\"${autocar_tag}\\n\",\"color\":\"#000001\"},\\n\"remark\":{\"value\":\"#周年聚惠#盛大开启！现在充值即返最高88元现金红包，更有机会获得免单、油卡……\\n\\n限量2019人，手慢无！戳我开抢☞☞☞\",\"color\":\"#FF2400\"}}";
		try {
			XxlJobLogger.log("[UpdateTemplateMsgJobHandler29_2030.updateTemplateMsg][entry3]");
			sql1 = "UPDATE cc_wx_configuration_info SET set_val = ? WHERE id = 8;";
			XxlJobLogger.log("[UpdateTemplateMsgJobHandler29_2030.updateTemplateMsg][entry4]");
		} catch (Exception e1) {
			XxlJobLogger.log("[UpdateTemplateMsgJobHandler29_2030.updateTemplateMsg][更新通用模板消息异常],sql,e={0}",ExceptionUtils.getStackTrace(e1));
		} 
		try {
			jdbcTemplate.update(sql1,value1);
		} catch (Exception e) {
			XxlJobLogger.log("[UpdateTemplateMsgJobHandler29_2030.updateTemplateMsg][更新通用模板消息异常],sql1,e={0}",ExceptionUtils.getStackTrace(e));
		}
	}
		
}
