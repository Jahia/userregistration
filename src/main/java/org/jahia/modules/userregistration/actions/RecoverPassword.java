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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Jahia;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.mail.MailService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.i18n.Messages;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Action handler for sending a password recovery e-mail. The e-mail body contains a link to a page with the
 * password recovery form.
 *
 * @author qlamerand
 */
public class RecoverPassword extends Action {

    private JahiaUserManagerService userManagerService;

    private MailService mailService;

    private String templatePath;


    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String username = getParameter(parameters, "username");
        if (StringUtils.isEmpty(username)) {
            return ActionResult.BAD_REQUEST;
        }

        if (req.getSession().getAttribute("passwordRecoveryAsked") != null) {
            JSONObject json = new JSONObject();
            String message = Messages.get("resources.JahiaUserRegistration", "passwordrecovery.mail.alreadysent",
                    resource.getLocale(), "Jahia User Registration");
            json.put("message", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        }

        JCRUserNode user = userManagerService.lookupUser(username);
        if (user == null ) {
            JSONObject json = new JSONObject();
            String message = Messages.get("resources.JahiaUserRegistration", "passwordrecovery.username.invalid",
                    resource.getLocale(), "Jahia User Registration");
            json.put("message", message);
            return new ActionResult(SC_OK, null, json);
        }

        String to = user.getPropertyAsString("j:email");
        if (to == null || !MailService.isValidEmailAddress(to, false)) {
            JSONObject json = new JSONObject();
            String message = Messages.get("resources.JahiaUserRegistration", "passwordrecovery.mail.invalid",
                    resource.getLocale(), "Jahia User Registration");
            json.put("message", message);
            return new ActionResult(SC_OK, null, json);
        }
        String from = mailService.getSettings().getFrom();

        String authKey = DigestUtils.md5Hex(username + System.currentTimeMillis());
        req.getSession().setAttribute("passwordRecoveryToken", new PasswordToken(authKey, user.getPath()));

        Map<String,Object> bindings = new HashMap<String,Object>();
        bindings.put("link", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() +
                Jahia.getContextPath() + Render.getRenderServletPath() + "/live/"
                + resource.getLocale().getLanguage() + resource.getNode().getPath() + ".html?key=" + authKey);
        bindings.put("user", user);

        mailService.sendMessageWithTemplate(templatePath, bindings, to, from, null, null, resource.getLocale(), "Jahia User Registration");
        req.getSession().setAttribute("passwordRecoveryAsked", true);

        JSONObject json = new JSONObject();
        String message = Messages.get("resources.JahiaUserRegistration", "passwordrecovery.mail.sent",
                resource.getLocale(), "Jahia User Registration");
        json.put("message", message);
        return new ActionResult(HttpServletResponse.SC_ACCEPTED, null, json);
    }

    public void setUserManagerService(JahiaUserManagerService userManagerService) {
        this.userManagerService = userManagerService;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }
    
    public class PasswordToken implements Serializable {
        private static final long serialVersionUID = 6457936874436104758L;
        String authkey;
        String userpath;

        public PasswordToken(String authkey, String userpath) {
            this.authkey = authkey;
            this.userpath = userpath;
        }

        public String getAuthkey() {
            return authkey;
        }

        public String getUserpath() {
            return userpath;
        }
    }
}
