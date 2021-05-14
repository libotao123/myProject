package cn.yunovo.job.admin.common;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.sunshine.dcda.system.service.model.SystemResourceVo;


import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

@Component
public class FtlCheckPermission implements TemplateMethodModelEx {
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override  
	public Object exec(List args) throws TemplateModelException {
		
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String rescCode = args.get(0).toString();  
          
        List<SystemResourceVo>  rolePermission = (List<SystemResourceVo>)request.getSession().getAttribute("userButtons");
        if(null == rolePermission || rolePermission.size() < 1){
        	return false;
        }
        for (SystemResourceVo resource : rolePermission) {  
            if (rescCode.equals(resource.getRescCode())) {  
                return true;  
            }  
        }  
  
        return false;  
    } 
}
