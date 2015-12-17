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
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.engines.EngineMessage;
import org.jahia.engines.EngineMessages;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.pwdpolicy.JahiaPasswordPolicyService;
import org.jahia.services.pwdpolicy.PolicyEnforcementResult;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

/**
 * Action handler for change a forgotten password.
 *
 * @author qlamerand
 */
public class UnauthenticatedChangePasswordAction extends Action {

    private static final String BUNDLE = "resources.JahiaUserRegistration";

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        String authKey = getParameter(parameters, "authKey");
        RecoverPassword.PasswordToken passwordRecoveryToken = (RecoverPassword.PasswordToken) req.getSession().getAttribute("passwordRecoveryToken");
        if (StringUtils.isEmpty(authKey) || passwordRecoveryToken == null || !passwordRecoveryToken.getAuthkey().equals(authKey) || !passwordRecoveryToken.getUserpath().equals(resource.getNode().getPath())) {
            return ActionResult.BAD_REQUEST;
        }
        HttpSession httpSession = req.getSession();
        httpSession.removeAttribute("passwordRecoveryToken");
        httpSession.removeAttribute("passwordRecoveryAsked");

        String passwd = req.getParameter("password").trim();
        JSONObject json = new JSONObject();

        if (!resource.getNode().hasPermission("jcr:write_default") || !resource.getNode().isNodeType("jnt:user")) {
            return new ActionResult(HttpServletResponse.SC_FORBIDDEN, null, null);
        }

        if ("".equals(passwd)) {
            String userMessage = Messages.get(BUNDLE, "passwordrecovery.recover.password.mandatory", renderContext.getUILocale());
            json.put("errorMessage", userMessage);
        } else {
            String passwdConfirm = req.getParameter("passwordconfirm").trim();
            if (!passwdConfirm.equals(passwd)) {
                String userMessage = Messages.get(BUNDLE, "passwordrecovery.recover.password.not.matching", renderContext.getUILocale());
                json.put("errorMessage",userMessage);
            } else {
                JahiaPasswordPolicyService pwdPolicyService = ServicesRegistry.getInstance().getJahiaPasswordPolicyService();
                JCRUserNode user = ServicesRegistry.getInstance().getJahiaUserManagerService().lookupUser(resource.getNode().getName());

                PolicyEnforcementResult evalResult = pwdPolicyService.enforcePolicyOnPasswordChange(user, passwd, true);
                if (!evalResult.isSuccess()) {
                    EngineMessages policyMsgs = evalResult.getEngineMessages();
                    StringBuilder res = new StringBuilder();
                    for (EngineMessage message : policyMsgs.getMessages()) {
                        res.append((message.isResource() ? Messages.getInternalWithArguments(message.getKey(), renderContext.getUILocale(), message.getValues()) : message.getKey())).append("\n");
                    }
                    json.put("errorMessage", res.toString());
                } else {
                    // change password
                    user.setPassword(passwd);
                    json.put("errorMessage", Messages.get(BUNDLE, "passwordrecovery.recover.passwordChanged", renderContext.getUILocale()));

                    httpSession.setAttribute(Constants.SESSION_USER, user);

                    json.put("result", "success");
                }
            }
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, json);
    }
}
