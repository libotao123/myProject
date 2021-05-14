package cn.yunovo.job.executor.service.jobhandler;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


import cn.yunovo.job.core.biz.model.ReturnT;
import cn.yunovo.job.core.handler.IJobHandler;
import cn.yunovo.job.core.handler.annotation.JobHandler;
import cn.yunovo.job.core.log.XxlJobLogger;

@JobHandler(value="doi_device_job_zc2")
@Component
public class DOIDeviceBaseInfoDelBadData extends IJobHandler {
    
  
    
    @Autowired
    private JdbcTemplate jt;
    
    
    
    @Override
    public ReturnT<String> execute(String param) throws Exception {
        XxlJobLogger.log("开始");
        ReturnT<String> a = new ReturnT<String>();
        
        try
        {
//            StartUpdateDeviceInfo();
//            pageQueryDeviceInfoWithYundashi(10,10);
//            StartDeleBadDate();
            AddNewCount();
            a.setCode(202);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            a.setCode(424);
            XxlJobLogger.log("数据异常");
        }
       
        a.setMsg("yunovo:"+new SimpleDateFormat("yyyyMMdd HH:mm:sss").format(new Date()));
        return a;
    }
    
    
  
    // ocp_doi_devices_ability 添加一个新的字段
    private void AddNewCount()
    {
        String sql = " ALTER TABLE  `ocp_doi_devices_ability` ADD COLUMN `extra_info`  varchar(100) NULL DEFAULT -1 AFTER `package_name`";
        jt.update(sql);
    }




    private void StartDeleBadDate()
    {
        String sql = " DELETE FROM    ocp_doi_devices_ability WHERE id IN ( SELECT a.id FROM ( SELECT t.id, COUNT(*) num FROM ocp_doi_devices_ability t GROUP BY devices_sn,package_name HAVING num > 1 ) a ) ";;
        jt.update(sql);
        sql =  " ALTER TABLE `ocp_doi_devices_ability` DROP INDEX `apk` , ADD UNIQUE INDEX `apk` (`devices_sn`, `package_name`) USING BTREE ";
        jt.update(sql);
        
    }


    
}
