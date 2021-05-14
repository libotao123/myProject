package cn.yunovo.job.executor.service.jobhandler;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="update_template_msg_job_lbt")
@Component

/*
 * 更新为活动的模板消息26_2030
 * 
 * 
 */
public class UpdateTemplateMsgJobHandler26_2030 extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UpdateTemplateMsgJobHandler26_2030.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			updateTemplateMsg();
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[UpdateTemplateMsgJobHandler26_2030.updateTemplateMsg][更新活动模板消息异常]");
            XxlJobLogger.log("[UpdateTemplateMsgJobHandler26_2030.updateTemplateMsg][更新活动模板消息异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		
		return a;
    }
	
	//更新模板消息
	public void updateTemplateMsg() throws Exception {
		String sql1 = "UPDATE cc_wx_configuration_info SET set_val = '{\"first\":{\"value\":\"糟糕！套餐内流量已不足。流量用尽后，小达将无法为您提供远程定位、实时导航等服务。\\n\",\"color\":\"#FF2400\"},\\n\"keyword1\":{\"value\":\"${iccid}\",\"color\":\"#000001\"},\\n\"keyword2\":{\"value\":\"${balance}M\",\"color\":\"#000001\"},\\n\"keyword3\":{\"value\":\"${time}\",\"color\":\"#000001\"}\\n,\"keyword4\":{\"value\":\"${autocar_tag}\\n\",\"color\":\"#000001\"},\\n\"remark\":{\"value\":\"#5折狂欢#来袭！\\n\\n9日至11日，全场流量5折，今年最后一次大福利，千万别错过！\\n\\n戳我开抢☞☞☞\",\"color\":\"#FF2400\"}}' WHERE id = 85";
		String sql2 = "UPDATE cc_wx_configuration_info SET set_val = '{\"first\":{\"value\":\"车主大大，您的套餐流量用完了，小达无法继续为您提供远程定位、实时导航等服务。\\n\",\"color\":\"#FF2400\"},\\n\"keyword1\":{\"value\":\"${iccid}\",\"color\":\"#000001\"},\\n\"keyword2\":{\"value\":\"${balance}M\",\"color\":\"#FF0000\"},\\n\"keyword3\":{\"value\":\"${time}\",\"color\":\"#000001\"}\\n,\"keyword4\":{\"value\":\"${autocar_tag}\\n\",\"color\":\"#000001\"},\\n\"remark\":{\"value\":\"#5折狂欢#来袭！\\n\\n9日至11日，全场流量5折，今年最后一次大福利，千万别错过！\\n\\n戳我开抢☞☞☞\",\"color\":\"#FF2400\"}}' WHERE id = 86"; 
		String sql3 = "UPDATE cc_wx_configuration_info SET set_val = '{\"first\":{\"value_\":\"糟糕！套餐有效期已不足。流量用尽后，小达将无法为您提供远程定位、实时导航等服务。\\n\",\"color_\":\"#FF2400\",\"value\":\"车主大大，套餐服务已到期，小达无法继续为您提供远程定位、实时导航等服务。\\n\",\"color\":\"#FF0000\"},\\n\"keyword1\":{\"value\":\"SIM卡套餐服务\",\"color\":\"#000001\"},\\n\"keyword2\":{\"value\":\"${time}\",\"color\":\"#000001\"},\\n\"keyword3\":{\"value\":\"${iccid}\\n\",\"color\":\"#000001\"},\\n\"remark\":{\"value\":\"#5折狂欢#来袭！\\n\\n9日至11日，全场流量5折，今年最后一次大福利，千万别错过！\\n\\n戳我开抢☞☞☞\",\"color\":\"#FF2400\"}}' WHERE id = 87"; 
		jdbcTemplate.update(sql1);
		jdbcTemplate.update(sql2);
		jdbcTemplate.update(sql3);
	}
	
}
