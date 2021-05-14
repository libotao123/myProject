package cn.yunovo.job.executor.service.jobhandler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="delete_repeat_open_id")
@Component
/*
 * 删除重复的openid
 * 
 * 
 */
public class DeleteRepeatOpenidJobHandler extends IJobHandler {
	protected final static Logger logger = LoggerFactory.getLogger(DeleteRepeatOpenidJobHandler.class);
	
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public ReturnT<String> execute(String open_id) throws Exception {
    	ReturnT<String> a = new ReturnT<String>();
		try {
//			main(open_id);
//			main_main();
			updateData();
			a.setCode(200);
			a.setMsg("success");
		} catch (Exception e) {
			e.printStackTrace();
            a.setCode(500);
            logger.error("[DeleteRepeatOpenidJobHandler.execute][execute异常],e={}",ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[DeleteRepeatOpenidJobHandler.execute][execute异常],e={0}",ExceptionUtils.getStackTrace(e));
		}
		return a;
    }

    public void main_main() {
    	String open_ids = "";
    	String[] arr = open_ids.split(",");
    	for(int i = 0;i < arr.length;i++) {
    		main(arr[i]);
    	}
    }
    
    public void main(String open_id){
    	//通过open_id查询cc_customer_wx表里的wx_id
    	String sql = "SELECT wx_id FROM cc_customer_wx WHERE open_id = ? ";
    	List<Long> wx_ids = jdbcTemplate.queryForList(sql, Long.class, open_id);
    	
    	//查询union_id
    	String union_id = queryUnionid(open_id);
    	
    	//通过wx_id查询cc_device_bind表里是否存在有效绑定记录
    	int queryCount = 0;
    	int deleteCount = 0;
    	int updatCount = 0;
    	Long wx_id = null;
    	sql = "SELECT COUNT(*) FROM cc_device_bind WHERE status = 1 AND wx_id = ? ";
    	for(int i = 0;i < wx_ids.size();i++) {
    		wx_id = wx_ids.get(i);
    		queryCount = jdbcTemplate.queryForObject(sql, Integer.class, wx_id);
    		if(queryCount < 1) {
    			//删除该记录
    			deleteCount = deleteCustomerWxByWxid(wx_id);
    			logger.error("[DeleteRepeatOpenidJobHandler.main][删除微信表数据结果],open_id={},wx_id={},deleteCount={}",open_id,wx_id,deleteCount);
                XxlJobLogger.log("[DeleteRepeatOpenidJobHandler.main][删除微信表数据结果],open_id={0},wx_id={1},deleteCount={2}",open_id,wx_id,deleteCount);
    		}else {
    			//更新union_id
    			if(StringUtils.isNotBlank(union_id)) {
    				updatCount = undateUnionid(wx_id, union_id);
    				logger.error("[DeleteRepeatOpenidJobHandler.main][更新union_id结果],open_id={},wx_id={},union_id={},updatCount={}",open_id,wx_id,union_id,updatCount);
                    XxlJobLogger.log("[DeleteRepeatOpenidJobHandler.main][更新union_id结果],open_id={0},wx_id={1},union_id={2},updatCount={3}",open_id,wx_id,union_id,updatCount);
    			}
    		}
    	}
    	
    }
	
	public int deleteCustomerWxByWxid(Long wx_id){
		String sql = "DELETE FROM cc_customer_wx WHERE wx_id = ? ";
		return jdbcTemplate.update(sql, wx_id);
	}
	
	public String queryUnionid(String open_id){
		String sql = "SELECT union_id FROM cc_customer_wx WHERE open_id = ? AND union_id is not null LIMIT 1 ";
		try {
			return jdbcTemplate.queryForObject(sql, String.class, open_id);
		} catch (DataAccessException e) {
			logger.error("[DeleteRepeatOpenidJobHandler.queryUnionid][查询union_id异常],open_id={},e={}",open_id,ExceptionUtils.getStackTrace(e));
            XxlJobLogger.log("[DeleteRepeatOpenidJobHandler.queryUnionid][查询union_id异常],open_id={0},e={1}",open_id,ExceptionUtils.getStackTrace(e));
            return null;
		}
	}
	
	public int undateUnionid(Long wx_id,String union_id){
		String sql = "UPDATE cc_customer_wx SET union_id = ? WHERE wx_id = ? ";
		return jdbcTemplate.update(sql, union_id, wx_id);
	}
	
	public void updateData(){
		String sql = "UPDATE cc_wx_configuration_info SET set_val = 'Hello，我是达客出行，您的随身车管家。\r\n" + 
				"\r\n" + 
				"您还差一步即可完成设备激活绑定。\r\n" + 
				"☞<a href=\\'%s\\'>【戳我扫码绑定】</a>☜领取免费流量，让爱车实时在线。如有问题，回复【人工客服】将有专属客服为您答疑解惑。\r\n" + 
				"\r\n" + 
				"[礼物]车主福利 ↓↓\r\n" + 
				"活动①【4折洗车年卡】\r\n" + 
				"420元年卡，仅需198！\r\n" + 
				"领取>>>http://suo.im/6obINq\r\n" + 
				"\r\n" + 
				"活动③【ETC免费送】\r\n" + 
				"在线办理，包邮到家\r\n" + 
				"领取>>>http://suo.im/5xybWA\r\n" + 
				"\r\n" + 
				"活动②【自驾1折景点门票】\r\n" + 
				"2284元景点门票仅1折噢！\r\n" + 
				"速领>>>http://suo.im/699koA' WHERE set_key = 'SUBSCRIBE_REPLY' ";
		int count = jdbcTemplate.update(sql);
		logger.error("[updateData][result],count={}",count);
        XxlJobLogger.log("[updateData][result],count={0}",count);
	}
}
