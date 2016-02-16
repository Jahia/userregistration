/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.userregistration.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action handler for creating new user and sending an e-mail notification.
 *
 * @author rincevent
 */
public class NewUser extends BaseAction {
    
    private static Logger logger = LoggerFactory.getLogger(NewUser.class);

    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, final Resource resource,
                                  JCRSessionWrapper session, final Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        final String username = getParameter(parameters, "username");
        final String password = getParameter(parameters, "password");
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return ActionResult.BAD_REQUEST;
        }

        final Properties properties = new Properties();
        properties.put("j:email",parameters.get("desired_email").get(0));
        properties.put("j:firstName",parameters.get("desired_firstname").get(0));
        properties.put("j:lastName",parameters.get("desired_lastname").get(0));
        for (Map.Entry<String, List<String>> param : parameters.entrySet()) {
            if (param.getKey().startsWith("j:")) {
                String value = getParameter(parameters, param.getKey());
                if (value != null) {
                    properties.put(param.getKey(), value);
                }
            }
        }

        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Boolean>() {
            @Override
            public Boolean doInJCR(JCRSessionWrapper session) throws RepositoryException {
                final JCRUserNode user = userManagerService.createUser(username, password, properties, session);
                session.save();
                if (mailService.isEnabled()) {
                    // Prepare mail to be sent :
                    boolean toAdministratorMail = Boolean.valueOf(getParameter(parameters, "toAdministrator", "false"));
                    String to = toAdministratorMail ? mailService.getSettings().getTo():getParameter(parameters, "to");
                    String from = parameters.get("from")==null?mailService.getSettings().getFrom():getParameter(parameters, "from");
                    String cc = parameters.get("cc")==null?null:getParameter(parameters, "cc");
                    String bcc = parameters.get("bcc")==null?null:getParameter(parameters, "bcc");
                    
                    Map<String,Object> bindings = new HashMap<String,Object>();
                    bindings.put("newUser",user);
                    try {
                        mailService.sendMessageWithTemplate(templatePath,bindings,to,from,cc,bcc,resource.getLocale(),"Jahia User Registration");
                    } catch (ScriptException e) {
                        logger.error("Error sending e-mail notification for user creation", e);
                    }
                }
                
                return true;
            }
        });

        return new ActionResult(HttpServletResponse.SC_ACCEPTED,parameters.get("userredirectpage").get(0), new JSONObject());
    }
}
