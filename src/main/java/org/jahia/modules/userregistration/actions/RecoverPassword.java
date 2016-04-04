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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
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
import org.jahia.utils.Url;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_OK;

/**
 * Action handler for sending a password recovery e-mail. The e-mail body contains a link to a page with the
 * password recovery form.
 *
 * @author qlamerand
 */
public class RecoverPassword extends BaseAction {

    static final String PROPERTY_PASSWORD_RECOVERY_TOKEN = "j:passwordRecoveryToken";

    static final String SESSION_ATTRIBUTE_PASSWORD_RECOVERY_ASKED = "passwordRecoveryAsked";

    private static String generateToken(JCRUserNode user, int passwordRecoveryTimeout) throws RepositoryException {
        String path = user.getPath();
        long timestamp = System.currentTimeMillis();
        String authKey = DigestUtils.md5Hex(path + timestamp) + '|' + (timestamp + passwordRecoveryTimeout * 1000L);
        user.setProperty(PROPERTY_PASSWORD_RECOVERY_TOKEN, authKey);
        user.getSession().save();

        try {
            return Base64.encodeBase64URLSafeString((path + "|" + authKey).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private int passwordRecoveryTimeoutSeconds;

    @Override
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        String username = getParameter(parameters, "username");
        if (StringUtils.isEmpty(username)) {
            return ActionResult.BAD_REQUEST;
        }
        
        Locale locale = renderContext.getUILocale();

        if (req.getSession().getAttribute(SESSION_ATTRIBUTE_PASSWORD_RECOVERY_ASKED) != null) {
            return result(SC_OK, "passwordrecovery.mail.alreadysent", locale);
        }
        String siteKey = resource.getNode().getResolveSite().getSiteKey();
        JCRUserNode user = userManagerService.lookupUser(username,siteKey,true);
        if (user == null || user.isRoot() || JahiaUserManagerService.isGuest(user)) {
            return result(SC_OK, "passwordrecovery.username.invalid", locale);
        }

        String to = user.getPropertyAsString("j:email");
        if (to == null || !MailService.isValidEmailAddress(to, false)) {
            return result(SC_OK, "passwordrecovery.mail.invalid", locale);
        }
        String from = mailService.getSettings().getFrom();

        String token = generateToken(user, passwordRecoveryTimeoutSeconds > 0 ? passwordRecoveryTimeoutSeconds
                : req.getSession().getMaxInactiveInterval());

        Map<String,Object> bindings = new HashMap<String,Object>();
        bindings.put("link", Url.getServer(req) + Jahia.getContextPath() + Render.getRenderServletPath() + "/live/"
                + resource.getLocale().getLanguage() + resource.getNode().getPath() + ".html?key=" + token);
        bindings.put("user", user);

        mailService.sendMessageWithTemplate(templatePath, bindings, to, from, null, null, resource.getLocale(), "Jahia User Registration");
        req.getSession().setAttribute(SESSION_ATTRIBUTE_PASSWORD_RECOVERY_ASKED, true);

        return result(SC_ACCEPTED, "passwordrecovery.mail.sent", locale);
    }

    private ActionResult result(int code, String messageKey, Locale locale) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("message", getI18nMessage(messageKey, locale));

        return new ActionResult(code, null, json);
    }

    /**
     * Set the timeout in seconds, after which the password reset token expires. If a positive non-zero value is provided here, it will be
     * used. Otherwise, a current value of the HTTP session timeout will be used for expiration.
     * 
     * @param passwordRecoveryTimeoutSeconds
     *            the timeout in seconds, after which the password reset token expires. If a positive value is provided here, it will be
     *            used. Otherwise, a current value of the HTTP session timeout will be used for expiration
     */
    public void setPasswordRecoveryTimeoutSeconds(int passwordRecoveryTimeoutSeconds) {
        this.passwordRecoveryTimeoutSeconds = passwordRecoveryTimeoutSeconds;
    }
}
