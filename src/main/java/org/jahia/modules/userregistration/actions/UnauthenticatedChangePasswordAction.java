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

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
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
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.i18n.Messages;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Action handler for change a forgotten password.
 *
 * @author qlamerand
 */
public class UnauthenticatedChangePasswordAction extends BaseAction {

    private static boolean isExpired(String timestamp) {
        try {
            return System.currentTimeMillis() > Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return true;
        }
    }
    
    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource, JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {
        Locale locale = renderContext.getUILocale();
        JSONObject json = new JSONObject();
        
        JCRUserNode user = getTargetUser(resource, parameters, json, locale);
        if (user == null) {
            return json.length() > 0 ? new ActionResult(HttpServletResponse.SC_OK, null, json)
                    : ActionResult.BAD_REQUEST;
        }
        
        if (!resource.getNode().hasPermission("jcr:write_default") || !resource.getNode().isNodeType("jnt:user")) {
            // user is not allowed to change the password
            json.put("errorMessage",
                    Messages.getInternal("org.jahia.engines.pwdpolicy.passwordChangeNotAllowed", locale));
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        }
        
        String passwd = getParameter(parameters, "password", StringUtils.EMPTY).trim();

        if ("".equals(passwd)) {
            String userMessage = getI18nMessage("passwordrecovery.recover.password.mandatory", locale);
            json.put("errorMessage", userMessage);
        } else {
            String passwdConfirm = getParameter(parameters, "passwordconfirm", StringUtils.EMPTY).trim();
            if (!passwdConfirm.equals(passwd)) {
                json.put("errorMessage", getI18nMessage("passwordrecovery.recover.password.not.matching", locale));
            } else {
                JahiaPasswordPolicyService pwdPolicyService = ServicesRegistry.getInstance().getJahiaPasswordPolicyService();

                PolicyEnforcementResult evalResult = pwdPolicyService.enforcePolicyOnPasswordChange(user, passwd, true);
                if (!evalResult.isSuccess()) {
                    EngineMessages policyMsgs = evalResult.getEngineMessages();
                    StringBuilder res = new StringBuilder();
                    for (EngineMessage message : policyMsgs.getMessages()) {
                        res.append((message.isResource() ? Messages.getInternalWithArguments(message.getKey(), locale, message.getValues()) : message.getKey())).append("\n");
                    }
                    json.put("errorMessage", res.toString());
                } else {
                    // change password
                    user.setPassword(passwd);
                    json.put("errorMessage", getI18nMessage("passwordrecovery.recover.passwordChanged", locale));

                    HttpSession httpSession = req.getSession();
                    httpSession.removeAttribute("passwordRecoveryAsked");
                    
                    httpSession.setAttribute(Constants.SESSION_USER, user.getJahiaUser());

                    // remove the token
                    user.getProperty(RecoverPassword.PROPERTY_PASSWORD_RECOVERY_TOKEN).remove();
                    user.getSession().save();

                    json.put("result", "success");
                }
            }
        }

        return new ActionResult(HttpServletResponse.SC_OK, null, json);
    }

    private JCRUserNode getTargetUser(Resource resource, Map<String, List<String>> parameters, JSONObject json,
            Locale locale) throws RepositoryException, JSONException {
        JCRUserNode user = null;
        String authKey = getParameter(parameters, "authKey");
        if (StringUtils.isEmpty(authKey)) {
            return null;
        }
        String siteKey = resource.getNode().getResolveSite().getSiteKey();
        user = userManagerService.lookupUser(resource.getNode().getName(),siteKey,true);
        // check valid user
        if (user == null) {
            json.put("errorMessage", getI18nMessage("passwordrecovery.username.invalid", locale));
            return null;
        }
        // check that it is not root and not guest
        if (user.isRoot() || JahiaUserManagerService.isGuest(user)) {
            json.put("errorMessage",
                    Messages.getInternal("org.jahia.engines.pwdpolicy.passwordChangeNotAllowed", locale));
            return null;
        }

        // we've found our user: get the reference token
        String token = user.getPropertyAsString(RecoverPassword.PROPERTY_PASSWORD_RECOVERY_TOKEN);

        if (token == null) {
            // we do not have a token for that user
            json.put("errorMessage", getI18nMessage("passwordrecovery.token.invalid", locale));
            return null;
        }

        // check if the token has expired
        if (isExpired(StringUtils.substringAfter(token, "|"))) {
            // remove the expired token
            user.getProperty(RecoverPassword.PROPERTY_PASSWORD_RECOVERY_TOKEN).remove();
            user.getSession().save();

            json.put("errorMessage", getI18nMessage("passwordrecovery.token.invalid", locale));
            return null;
        }

        // compare the submitted and referenced tokens
        if (!token.equals(authKey)) {
            json.put("errorMessage", getI18nMessage("passwordrecovery.token.invalid", locale));
            return null;
        }

        // we are all good
        return user;
    }
}
