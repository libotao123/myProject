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

@JobHandler(value="update_prize_sum_job_lbt")
@Component

/*
 * 追加转盘奖项数
 * 
 * 
 */
public class UpdatePrizeSumJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(UpdatePrizeSumJobHandler.class);
    
    @Autowired
    public RestTemplate restTemplate;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
			int prizeSum = Integer.valueOf(param);
			updatePrizeSum(prizeSum);
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[UpdatePrizeSumJobHandler.updatePrizeSum][追加转盘奖项数]");
            XxlJobLogger.log("[UpdatePrizeSumJobHandler.updatePrizeSum][追加转盘奖项数],e={0}",ExceptionUtils.getStackTrace(e));
		}
		
		return a;
    }
	
	//追加转盘奖项数
	public void updatePrizeSum(Integer prizeSum) throws Exception {
		int result = 0;
		String sql = "UPDATE cc_wx_activity_prizeewheel_number SET prizeA_number = (prizeA_number + ?),prizeB_number = (prizeB_number + ?)";
		try {
			result = jdbcTemplate.update(sql,prizeSum,prizeSum);
			if(result == 1) {
				XxlJobLogger.log("[UpdatePrizeSumJobHandler.updatePrizeSum][追加转盘奖项数成功],prizeSum={0}",prizeSum);
			}else {
				XxlJobLogger.log("[UpdatePrizeSumJobHandler.updatePrizeSum][追加转盘奖项数失败],sql={0},prizeSum={1}",sql,prizeSum);
			}
		} catch (Exception e1) {
			XxlJobLogger.log("[UpdatePrizeSumJobHandler.updatePrizeSum][追加转盘奖项数异常],e={0}",ExceptionUtils.getStackTrace(e1));
		}
	}
	
}
