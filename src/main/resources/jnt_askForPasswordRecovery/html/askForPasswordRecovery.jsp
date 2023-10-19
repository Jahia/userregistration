<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentAliasUser" type="org.jahia.services.usermanager.JahiaUser"--%>

<c:if test="${!renderContext.loggedIn || currentAliasUser.username eq 'guest'}">
    <template:addResources>
        <script type="text/javascript">
            document.addEventListener("DOMContentLoaded", function(){
                document.querySelector("#recoveryPasswordForm_${currentNode.identifier}").addEventListener("submit", function(event) {
                    event.preventDefault();
                    var form = this;
                    var url = form.getAttribute('action');

                    form.setAttribute('action','#');
                    var username = form.querySelector('input[name="username"]').value;
                    if (typeof(username) === 'undefined') {
                        return false;
                    }

                    xmlhttp=new XMLHttpRequest();
                    xmlhttp.onreadystatechange=function() {
                        if (xmlhttp.readyState === 4 && xmlhttp.status === 200) {
                            let data = JSON.parse(xmlhttp.responseText);
                            alert(data['message'])
                        }
                    }
                    xmlhttp.open("POST", url, true);
                    xmlhttp.setRequestHeader("Accept", "application/json")
                    var formData = new FormData(form);
                    xmlhttp.send(formData);
                    return false;
                });
            });
        </script>
    </template:addResources>

    <template:tokenizedForm>
        <form id="recoveryPasswordForm_${currentNode.identifier}"
              action="<c:url value='${url.base}${currentNode.properties.passChangePage.node.path}.recoverPassword.do'/>"
              method="post">
            <label for="username_${currentNode.identifier}" class="left"><fmt:message key='passwordrecovery.username' /></label>
            <input type="text" id="username_${currentNode.identifier}" name="username" class="full" />
            <input type="submit" value="<fmt:message key='passwordrecovery.recover' />" class="button" />
        </form>
    </template:tokenizedForm>
</c:if>

<c:if test="${renderContext.editMode}">
    <fmt:message key="passwordrecovery.editMessage" />
</c:if>
