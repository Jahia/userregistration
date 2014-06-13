/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
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
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.userregistration.actions;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Jahia;
import org.jahia.bin.Render;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.mail.MailService;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.utils.i18n.JahiaResourceBundle;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author : qlamerand
 * @since : JAHIA 6.6
 */
public class RecoverPassword extends Action {

    private static final Logger logger = LoggerFactory.getLogger(RecoverPassword.class);

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
            String message = JahiaResourceBundle.getString("JahiaUserRegistration", "passwordrecovery.mail.alreadysent",
                    resource.getLocale(), "Jahia User Registration");
            json.put("message", message);
            return new ActionResult(HttpServletResponse.SC_OK, null, json);
        }

        JahiaUser user = userManagerService.lookupUser(username);
        if (user == null ) {
            JSONObject json = new JSONObject();
            String message = JahiaResourceBundle.getString("JahiaUserRegistration", "passwordrecovery.username.invalid",
                    resource.getLocale(), "Jahia User Registration");
            json.put("message", message);
            return new ActionResult(SC_OK, null, json);
        }

        String to = user.getProperty("j:email");
        if (to == null || !MailService.isValidEmailAddress(to, false)) {
            JSONObject json = new JSONObject();
            String message = JahiaResourceBundle.getString("JahiaUserRegistration", "passwordrecovery.mail.invalid",
                    resource.getLocale(), "Jahia User Registration");
            json.put("message", message);
            return new ActionResult(SC_OK, null, json);
        }
        String from = mailService.getSettings().getFrom();

        String authKey = DigestUtils.md5Hex(username + System.currentTimeMillis());
        req.getSession().setAttribute("passwordRecoveryToken", new PasswordToken(authKey, user.getLocalPath()));

        Map<String,Object> bindings = new HashMap<String,Object>();
        bindings.put("link", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() +
                Jahia.getContextPath() + Render.getRenderServletPath() + "/live/"
                + resource.getLocale().getLanguage() + resource.getNode().getPath() + ".html?key=" + authKey);
        bindings.put("user", user);

        mailService.sendMessageWithTemplate(templatePath, bindings, to, from, null, null, resource.getLocale(), "Jahia User Registration");
        req.getSession().setAttribute("passwordRecoveryAsked", true);

        JSONObject json = new JSONObject();
        String message = JahiaResourceBundle.getString("JahiaUserRegistration", "passwordrecovery.mail.sent",
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
