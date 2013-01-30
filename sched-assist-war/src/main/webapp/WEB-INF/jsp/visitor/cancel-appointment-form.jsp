<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

--%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
<head>
<c:choose>
<c:when test="${command.multipleVisitors }">
<title><spring:message code="application.name"/> - <spring:message code="leave.appointment" arguments="${owner.calendarAccount.displayName }" argumentSeparator=";"/></title>
<spring:message code="leave" var="cancelorleave"/>
</c:when>
<c:otherwise>
<title><spring:message code="application.name"/> - <spring:message code="cancel.appointment" arguments="${owner.calendarAccount.displayName }" argumentSeparator=";"/></title>
<spring:message code="cancel" var="cancelorleave"/>
</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/themes/jasig/head-elements.jsp" %>

<style type="text/css">
#content: {
margin-top: 5px;
margin-left: 5px;
}
#helpmesg	{
margin-bottom: 10px;
}
#formContainer {
width: 475px;
}
form div {
clear: both;
margin-bottom: 18px;
overflow: hidden;
}
form fieldset {
border: #E5E5E5 2px solid;
clear: both;
margin-bottom: 9px;
overflow: hidden;
padding: 9px 18px;
}
form legend {
background: #E5E5E5;
color: #262626;
margin-bottom: 9px;
padding: 2px 11px;
}
</style>
<script type="text/javascript" src="<c:url value="/js/jquery.lockSubmit.js"/>"></script>
<script type="text/javascript">
$(document).ready(function(){
	$(':submit').lockSubmit();
	if ($.browser.msie) {
		// IE is ridiculous and doesn't fire the change event until you click elsewhere in the page AFTER changing a checkbox or radio
		// so, deal with this by doing what IE should be doing and simulate the change event on the 'click' event
		$('#confirmCancel').click(function() {
			this.blur();
		    this.focus();
		});
	}
			
	$('#confirmCancel').change(function() {
		if($(this).is(':checked')) {
			$(':submit').removeAttr('disabled');
		} else {
			$(':submit').attr('disabled','disabled');
		}
	});
});
</script>

</head>

<body>
<%@ include file="/WEB-INF/jsp/themes/jasig/body-start.jsp" %>
<%@ include file="/WEB-INF/jsp/login-info.jsp" %>
<div id="content" class="main col">

<c:if test="${redirected}">
<div class="alert" style="margin-bottom: 1em;">
<p><spring:message code="attending.shortdescription"/>.</p>
</div>
</c:if>

<div id="helpmesg" class="info">
<c:choose>
<c:when test="${command.multipleVisitors }">
<spring:message code="leave.appointment.help"/>
</c:when>
<c:otherwise>
<spring:message code="cancel.appointment.help"/>
</c:otherwise>
</c:choose>
</div>

<div id="formContainer">
<form:form>
<fieldset>
<fmt:formatDate value="${command.targetBlock.startTime}" type="time" pattern="EEE MMM d, h:mm a" var="startTimeFormatted"/>
<legend>
<c:choose>
<c:when test="${command.multipleVisitors }">
<spring:message code="leave.appointment.legend" arguments="${owner.calendarAccount.displayName};${startTimeFormatted}" argumentSeparator=";"/>
</c:when>
<c:otherwise>
<spring:message code="cancel.appointment.legend" arguments="${owner.calendarAccount.displayName};${startTimeFormatted}" argumentSeparator=";"/>
</c:otherwise>
</c:choose>
</legend>
<div class="formerror"><form:errors path="*"/></div>
<form:label path="confirmCancel"><spring:message code="cancelleave.confirm" arguments="${cancelorleave}"/>:</form:label>
<form:checkbox path="confirmCancel" id="confirmCancel"/>
<br/>
<c:if test="${not command.multipleVisitors}">
<form:label path="reason"><spring:message code="reason"/>:</form:label><br/>
<form:textarea rows="3" cols="40" path="reason"/>
</c:if>
<br/>
<c:choose>
<c:when test="${command.multipleVisitors }">
<input type="submit" value="<spring:message code="leave.appointment.this"/>" disabled="disabled"/>
</c:when>
<c:otherwise>
<input type="submit" value="<spring:message code="cancel.appointment.this"/>" disabled="disabled"/>
</c:otherwise>
</c:choose>
</fieldset>
</form:form>
</div> <!--  end formContainer -->
<a href="view.html">&laquo;<spring:message code="return.to.schedule"/></a>
</div> <!--  content -->

<%@ include file="/WEB-INF/jsp/themes/jasig/body-end.jsp" %>
</body>
</html>