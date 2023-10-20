<%@ page import="org.apache.commons.codec.binary.Base64" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>

<c:if test="${not empty param['key']}">
	<% pageContext.setAttribute("requestKeyValue", new String(Base64.decodeBase64(request.getParameter("key")), "UTF-8")); %>
</c:if>
<c:if test="${not empty requestKeyValue}">
	<c:set var="userpath" value="${fn:substringBefore(requestKeyValue, '|')}" />
	<c:set var="authKey" value="${fn:substringAfter(requestKeyValue, '|')}" />
</c:if>
<c:choose>
    <c:when test="${not empty userpath && not empty authKey}">
    <template:addResources>
        <script type="text/javascript">
            document.addEventListener("DOMContentLoaded", function(){
                document.querySelector("#changePasswordForm_${currentNode.identifier}").addEventListener("submit", function(event) {
                    event.preventDefault();
                    var form = this;
                    var url = form.getAttribute('action');

                    var password = form.querySelector('input[name="password"]').value;
                    if (password === '') {
                        alert("<fmt:message key='passwordrecovery.recover.password.mandatory'/>");
                        return false;
                    }
                    var passwordconfirm = form.querySelector('input[name="passwordconfirm"]').value;
                    if (passwordconfirm !== password) {
                        alert("<fmt:message key='passwordrecovery.recover.password.not.matching'/>");
                        return false;
                    }


                    xmlhttp = new XMLHttpRequest();
                    xmlhttp.onreadystatechange=function() {
                        if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
                            var data = JSON.parse(xmlhttp.responseText);
                            alert(data['errorMessage']);
                            if (data['result'] === 'success') {
                                window.location.reload();
                            }
                        }
                    }
                    xmlhttp.open("POST", url, true);
                    xmlhttp.setRequestHeader("Accept", "application/json")
                    var formData = new FormData(form);
                    xmlhttp.send(formData);
                });
            });
        </script>
    </template:addResources>

    <template:tokenizedForm>
        <form id="changePasswordForm_${currentNode.identifier}"
              action="<c:url value='${url.base}${fn:escapeXml(userpath)}.unauthenticatedChangePassword.do'/>"
              method="post">
            <input type="hidden" name="authKey" value="${fn:escapeXml(authKey)}" />
            <p class="field">
                <label for="password_${currentNode.identifier}" class="left"><fmt:message key="label.password" /></label>
                <input type="password" id="password_${currentNode.identifier}" name="password" class="full" autocomplete="off" />
            </p>
            <p class="field">
                <label for="passwordconfirm_${currentNode.identifier}" class="left"><fmt:message key="userregistration.label.form.confirmPassword" /></label>
                <input type="password" id="passwordconfirm_${currentNode.identifier}" name="passwordconfirm" class="full" autocomplete="off" />
            </p>
            <p class="field">
                <input type="submit" value="<fmt:message key='label.ok' />" class="button" />
            </p>
        </form>
    </template:tokenizedForm>
    </c:when>
    <c:otherwise>
        <a href="<c:url value="${url.base}${renderContext.site.home.path}.html"/>"><fmt:message key='passwordrecovery.back'/></a>
    </c:otherwise>
</c:choose>

<c:if test="${renderContext.editMode}">
    <fmt:message key="${fn:replace(currentNode.primaryNodeTypeName, ':', '_')}" />
</c:if>
