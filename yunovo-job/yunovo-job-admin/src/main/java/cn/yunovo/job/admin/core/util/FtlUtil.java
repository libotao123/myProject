package cn.yunovo.job.admin.core.util;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.TemplateHashModel;

/**
 * ftl util
 *
 * @author xuxueli 2018-01-17 20:37:48
 */
public class FtlUtil {

    public static TemplateHashModel generateStaticModel(String packageName) {
        try {
            BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
            TemplateHashModel staticModels = wrapper.getStaticModels();
            TemplateHashModel fileStatics = (TemplateHashModel) staticModels.get(packageName);
            return fileStatics;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
