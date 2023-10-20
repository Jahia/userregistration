<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<c:if test="${not renderContext.liveMode || not renderContext.loggedIn}">
    <script type="text/javascript">
        function onChange() {
            const password = document.querySelector('input[name=password]');
            const confirm = document.querySelector('input[name=confirm_password]');
            if (confirm.value === password.value) {
                confirm.setCustomValidity('');
            } else {
                confirm.setCustomValidity('<fmt:message key='userregistration.label.validatePassword'/>');
            }
        }
    </script>
    <div class="Form">
<template:tokenizedForm>
    <form method="post" action="<c:url value='${url.base}${currentNode.path}.newUser.do'/>" name="newUser" id="newUser">
        <input type="hidden" name="userredirectpage" value="${currentNode.properties['userRedirectPage'].node.path}"/>
        <c:if test="${not empty currentNode.properties['from']}">
            <input type="hidden" name="from" value="${currentNode.properties['from'].string}"/>
        </c:if>
        <c:if test="${not empty currentNode.properties['to']}">
            <input type="hidden" name="to" value="${currentNode.properties['to'].string}"/>
        </c:if>
        <c:if test="${not empty currentNode.properties['cc']}">
            <input type="hidden" name="cc" value="${currentNode.properties['cc'].string}"/>
        </c:if>
        <c:if test="${not empty currentNode.properties['bcc']}">
            <input type="hidden" name="bcc" value="${currentNode.properties['bcc'].string}"/>
        </c:if>
        <input type="hidden" name="toAdministrator" value="${currentNode.properties['toAdministrator'].string}"/>

        <fieldset>
            <legend><fmt:message key="userregistration.label.form.name"/></legend>

            <p><label class="left" for="desired_login"><fmt:message key="userregistration.label.form.login"/></label>
                <input type="text" name="username" id="desired_login" value="" required minlength="2"/></p>


            <p><label class="left" for="desired_password"><fmt:message
                    key="userregistration.label.form.password"/></label><input type="password" name="password" required minlength="6"
                                                                               id="desired_password" autocomplete="off" onchange="onChange()"/></p>

            <p><label class="left" for="confirm_password"><fmt:message
                    key="userregistration.label.form.confirmPassword"/></label><input type="password" name="confirm_password" required minlength="6"
                                                                               id="confirm_password" autocomplete="off" onchange="onChange()"/></p>

            <p><label class="left" for="desired_email"><fmt:message
                    key="userregistration.label.form.email"/></label><input type="email" name="desired_email" required
                                                                            id="desired_email"/></p>

            <p><label class="left" for="desired_firstname"><fmt:message
                    key="userregistration.label.form.firstname"/></label><input type="text" name="desired_firstname" required
                                                                                id="desired_firstname"/></p>

            <p><label class="left" for="desired_lastname"><fmt:message
                    key="userregistration.label.form.lastname"/></label><input type="text" name="desired_lastname" required
                                                                               id="desired_lastname"/></p>

            <div class="formMarginLeft">
                <input type="submit" class="button"
                       value="<fmt:message key='userregistration.label.form.create'/>"/>
            </div>
        </fieldset>
    </form>
</template:tokenizedForm>
    </div>
</c:if>
