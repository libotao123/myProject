package cn.yunovo.job.core.biz;

import java.util.List;

import cn.yunovo.job.core.biz.model.HandleCallbackParam;
import cn.yunovo.job.core.biz.model.RegistryParam;
import cn.yunovo.job.core.biz.model.ReturnT;

/**
 * @author xuxueli 2017-07-27 21:52:49
 */
public interface AdminBiz {

    public static final String MAPPING = "/api";

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registry(RegistryParam registryParam);

    /**
     * registry remove
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registryRemove(RegistryParam registryParam);


    /**
     * trigger job for once
     *
     * @param jobId
     * @return
     */
    public ReturnT<String> triggerJob(int jobId);

}
