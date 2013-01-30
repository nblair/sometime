/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.schedassist.portlet.webflow;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.WindowState;

import net.fortuna.ical4j.model.component.VEvent;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.schedassist.SchedulingException;
import org.jasig.schedassist.model.AvailableBlock;
import org.jasig.schedassist.model.CommonDateOperations;
import org.jasig.schedassist.model.IScheduleOwner;
import org.jasig.schedassist.model.Preferences;
import org.jasig.schedassist.model.Relationship;
import org.jasig.schedassist.model.VisibleSchedule;
import org.jasig.schedassist.model.VisibleScheduleRequestConstraints;
import org.jasig.schedassist.model.VisibleWindow;
import org.jasig.schedassist.portlet.EventCancellation;
import org.jasig.schedassist.portlet.IPortletScheduleVisitor;
import org.jasig.schedassist.portlet.IPortletScheduleVisitorFactory;
import org.jasig.schedassist.portlet.PortletSchedulingAssistantService;
import org.jasig.schedassist.portlet.ScheduleOwnerNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.webflow.context.ExternalContext;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

/**
 * Helper that wraps {@link PortletSchedulingAssistantService} functions for web flows.
 * 
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: FlowHelper.java $
 */
@Service("flowHelper")
public class FlowHelper {

	protected static final String YES = "yes";
	protected static final String NO = "no";
	private Log LOG = LogFactory.getLog(this.getClass());
	private PortletSchedulingAssistantService schedulingAssistantService;
	private IPortletScheduleVisitorFactory scheduleVisitorFactory;
	private String availableWebBaseUrl;
	private String advisorUrl;
	private String profileSearchUrl;

	public static final String CURRENT_USER_ATTR = FlowHelper.class.getName() + ".CURRENT_USER";
	
	/**
	 * @param schedulingAssistantService the portletAvailableService to set
	 */
	@Autowired
	public void setPortletAvailableService(
			@Qualifier("portlet") PortletSchedulingAssistantService schedulingAssistantService) {
		this.schedulingAssistantService = schedulingAssistantService;
	}
	/**
	 * @param scheduleVisitorFactory the scheduleVisitorFactory to set
	 */
	@Autowired
	public void setScheduleVisitorFactory(
			IPortletScheduleVisitorFactory scheduleVisitorFactory) {
		this.scheduleVisitorFactory = scheduleVisitorFactory;
	}
	/**
	 * @param availableWebBaseUrl the availableWebBaseUrl to set
	 */
	@Autowired
	public void setAvailableWebBaseUrl(String availableWebBaseUrl) {
		Validate.notEmpty(availableWebBaseUrl, "availableWebBaseUrl property must not be empty");
		this.availableWebBaseUrl = availableWebBaseUrl;
		if(!this.availableWebBaseUrl.endsWith("/")) {
			this.availableWebBaseUrl += "/";
		}
		this.advisorUrl = this.availableWebBaseUrl + "public/advisors.html";
		this.profileSearchUrl = this.availableWebBaseUrl + "public/index.html";
	}	
	/**
	 * @return the availableWebBaseUrl
	 */
	public String getAvailableWebBaseUrl() {
		return availableWebBaseUrl;
	}
	/**
	 * @return the advisorUrl
	 */
	public String getAdvisorUrl() {
		return advisorUrl;
	}
	/**
	 * @return the profileSearchUrl
	 */
	public String getProfileSearchUrl() {
		return profileSearchUrl;
	}
	
	public void setNormalWindowState(ExternalContext context) {
		MutableAttributeMap map = context.getRequestMap();
		map.put("portletWindowState", WindowState.NORMAL);
	}
	/**
	 * 
	 * @param dateTimePhrase
	 * @return
	 * @throws ParseException
	 */
	public Date convertDateTime(String dateTimePhrase) throws ParseException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("convertDateTime called on " + dateTimePhrase);
		}
		SimpleDateFormat df = CommonDateOperations.getDateTimeFormat();
		return df.parse(dateTimePhrase);
	}
	/**
	 * 
	 * @return the current {@link IPortletScheduleVisitor} for the request.
	 */
	public IPortletScheduleVisitor getCurrentVisitor() {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		PortletRequest request = (PortletRequest) requestContext.getExternalContext().getNativeRequest();
		IPortletScheduleVisitor visitor = this.scheduleVisitorFactory.getPortletScheduleVisitor(request);
		PortletSession portletSession = request.getPortletSession();
		portletSession.setAttribute(CURRENT_USER_ATTR, visitor.getAccountId(), PortletSession.APPLICATION_SCOPE);
		return visitor;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean isCurrentVisitorEligible() {
		IPortletScheduleVisitor visitor = getCurrentVisitor();
		boolean result = visitor.isEligible();
		if(LOG.isDebugEnabled()) {
			LOG.debug(visitor + " has eligibility: " + result);
		}
		return result;
	}
	/**
	 * 
	 * @return
	 */
	public List<Relationship> getRelationshipsForCurrentVisitor() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("enter getRelationshipsForCurrentVisitor");
		}
		final String visitorUsername = getCurrentVisitor().getAccountId();
		List<Relationship> relationships = this.schedulingAssistantService.relationshipsForVisitor(visitorUsername);
		if(LOG.isDebugEnabled()) {
			LOG.debug("found " + relationships.size() + " relationships");
		}
		return relationships;
	}

	/**
	 * 
	 * @param owner
	 * @return
	 */
	public boolean isOwnerSamePersonAsCurrentVisitor(IScheduleOwner owner) {
		return owner.getCalendarAccount().getUsername().equals(getCurrentVisitor().getAccountId());
	}
	/**
	 * 
	 * @param ownerId
	 * @return
	 * @throws ScheduleOwnerNotFoundException
	 */
	public IScheduleOwner identifyTargetOwner(long ownerId) throws ScheduleOwnerNotFoundException {
		List<Relationship> relationships = getRelationshipsForCurrentVisitor(); 
		IScheduleOwner target = null;
		for(Relationship r : relationships) {
			if(r.getOwner().getId() == ownerId) {
				target = r.getOwner();
				if(LOG.isInfoEnabled()) {
					LOG.info(getCurrentVisitor() + " requested schedule of " + target);
				}
				break;
			}
		}

		if(null == target) {
			LOG.error(getCurrentVisitor() + " requested schedule for ownerId= " + ownerId + " and does not have a relationship, throwing ScheduleOwnerNotFoundException");
			throw new ScheduleOwnerNotFoundException(ownerId + " not found");
		}
		return target;
	}
	
	/**
	 * 
	 * @param owner
	 * @return
	 */
	public String getOwnerNoteboard(final IScheduleOwner owner) {
		if(null == owner) {
			return "";
		}
		return owner.getPreference(Preferences.NOTEBOARD);
	}
	
	/**
	 * 
	 * @param owner
	 * @return the owner's noteboard preference as a {@link List} of sentences
	 */
	public List<String> getOwnerNoteboardSentences(final IScheduleOwner owner) {
		if(null == owner) {
			return Collections.emptyList();
		}
		final String ownerNoteboard =  owner.getPreference(Preferences.NOTEBOARD);
		String [] sentences = ownerNoteboard.split("\n");
		return Arrays.asList(sentences);
	}
	/**
	 * 
	 * @param owner
	 * @param weekStart
	 * @return
	 */
	public String testExceededMeetingLimit(IScheduleOwner owner) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("enter testExceededMeetingLimit for " + owner);
		}
		final String visitorUsername = getCurrentVisitor().getAccountId();
		if(owner.hasMeetingLimit()) {
			VisibleSchedule schedule = this.schedulingAssistantService.getVisibleSchedule(visitorUsername, owner.getId());
			if(owner.isExceedingMeetingLimit(schedule.getAttendingCount())) {
				return YES;
			} else {
				return NO;
			}
		} else {
			// owner doesn't limit number of meetings, simply return no
			return NO;
		}
	}
	
	/**
	 * WebFlow can't apparently do type conversion on request parameters.
	 * 
	 * @param owner
	 * @param weekStartParam
	 * @return
	 * @throws ScheduleOwnerNotFoundException
	 */
	public VisibleSchedule getVisibleSchedule(IScheduleOwner owner, String weekStartParam) throws ScheduleOwnerNotFoundException {
		return getVisibleSchedule(owner, safeConvertWeekStartParam(weekStartParam));
	}
	/**
	 * 
	 * @param ownerId
	 * @param weekStart
	 * @return
	 * @throws ScheduleOwnerNotFoundException
	 */
	public VisibleSchedule getVisibleSchedule(IScheduleOwner owner, int weekStart) throws ScheduleOwnerNotFoundException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("enter getVisibleSchedule, weekstart: " + weekStart + ", owner: " + owner);
		}
		final String visitorUsername = getCurrentVisitor().getAccountId();
		VisibleSchedule schedule = this.schedulingAssistantService.getVisibleSchedule(visitorUsername, owner.getId(), weekStart);
		return schedule;
	}
	
	/**
	 * 
	 * @param owner
	 * @param weekStart
	 * @return
	 */
	public VisibleScheduleRequestConstraints getVisibleScheduleRequestConstraints(IScheduleOwner owner, int weekStart) {
		VisibleScheduleRequestConstraints result = VisibleScheduleRequestConstraints.newInstance(owner, weekStart);
		if(LOG.isDebugEnabled()) {
			LOG.debug(result);
		}
		return result;
	}
	/**
	 * 
	 * @param owner
	 * @param startDateTime
	 * @return
	 * @throws SchedulingException
	 */
	public CreateAppointmentFormBackingObject constructCreateAppointmentFormBackingObject(IScheduleOwner owner, Date startDateTime) throws SchedulingException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("enter constructCreateAppointmentFormBackingObject, start: " + startDateTime + ", owner: " + owner);
		}
		// can skip call to validateChosenStartTime as the flow will enforce the check
		AvailableBlock targetBlock = this.schedulingAssistantService.getTargetBlock(owner, startDateTime);
		if(LOG.isDebugEnabled()) {
			LOG.debug("getTargetBlock, startTime= " + startDateTime + " returns " + targetBlock);
		}
		if(targetBlock == null) {
			throw new SchedulingException("requested time is not available");
		}

		CreateAppointmentFormBackingObject fbo = new CreateAppointmentFormBackingObject(targetBlock,  owner.getPreferredMeetingDurations());
		return fbo;
	}

	/**
	 * 
	 * @param fbo
	 * @param owner
	 * @return
	 * @throws SchedulingException
	 */
	public VEvent createAppointment(CreateAppointmentFormBackingObject fbo, IScheduleOwner owner) throws SchedulingException {
		final IPortletScheduleVisitor currentVisitor = getCurrentVisitor();
		final String visitorUsername = currentVisitor.getAccountId();
		AvailableBlock targetBlock = fbo.getTargetBlock();
		if(NO.equals(validateChosenStartTime(owner.getPreferredVisibleWindow(), targetBlock.getStartTime()))) {
			throw new SchedulingException("requested time is no longer within visible window");
		}
		if(fbo.isDoubleLengthAvailable()) {
			LOG.debug("entering doubleLengthAvailable test");
        	// check if selected meeting duration matches meeting durations maxLength
			// if it's greater, then we need to look up the next block in the schedule and attempt to combine
			if(fbo.getSelectedDuration() == fbo.getMeetingDurations().getMaxLength()) {
				LOG.debug("selected duration matches double length");
				targetBlock = this.schedulingAssistantService.getTargetDoubleLengthBlock(owner, targetBlock.getStartTime());
				if(targetBlock == null) {
					throw new SchedulingException("second half of request time is not available");
				}
			}
		}
		if(LOG.isInfoEnabled()) {
			LOG.info(currentVisitor + " submitting scheduleAppointment request with " + owner + " at " + targetBlock);
		}
		VEvent event = this.schedulingAssistantService.scheduleAppointment(visitorUsername, owner.getId(), targetBlock, fbo.getReason());
		return event;
	}
	
	/**
	 * Verify the startTime argument is within the window; return {@link #NO} if not.
	 * 
	 * @param window
	 * @param startTime
	 * @return {@link #YES} for valid, {@link #NO} for invalid
	 */
	public String validateChosenStartTime(VisibleWindow window, Date startTime)  {
		final Date currentWindowEnd = window.calculateCurrentWindowEnd();
		if(startTime.before(window.calculateCurrentWindowStart()) || startTime.equals(currentWindowEnd) || startTime.after(currentWindowEnd)) {
			LOG.debug("selected startTime (" + startTime + ") is no longer within window " + window.getKey());
			return NO;
		}
		
		return YES;
	}

	/**
	 * 
	 * @param owner
	 * @param startTime
	 * @param endTime
	 * @return
	 * @throws SchedulingException
	 */
	public CancelAppointmentFormBackingObject constructCancelAppointmentFormBackingObject(IScheduleOwner owner, Date startTime, Date endTime) throws SchedulingException {
		if(LOG.isDebugEnabled()) {
			LOG.debug("enter constructCancelAppointmentFormBackingObject, start: " + startTime + ", end: " + endTime + " owner: " + owner);
		}
		// try to get the minimum size first
		AvailableBlock targetBlock = this.schedulingAssistantService.getTargetBlock(owner, startTime);
		if(null == targetBlock) {
			throw new SchedulingException("requested time is not available in schedule");
		} 

		if(!targetBlock.getEndTime().equals(endTime)) {
			// the returned block doesn't match the specified end time - try grabbing doublelength
			targetBlock = this.schedulingAssistantService.getTargetDoubleLengthBlock(owner, startTime);
			if(null != targetBlock && targetBlock.getEndTime().equals(endTime)) {
				CancelAppointmentFormBackingObject fbo = new CancelAppointmentFormBackingObject(targetBlock);
				return fbo;
			} else {
				throw new SchedulingException("requested time is not available in schedule");
			}

		} else {
			CancelAppointmentFormBackingObject fbo = new CancelAppointmentFormBackingObject(targetBlock);
			return fbo;
		}
	}

	/**
	 * 
	 * @param fbo
	 * @param owner
	 * @return the event cancellation details
	 * @throws SchedulingException
	 */
	public EventCancellation cancelAppointment(CancelAppointmentFormBackingObject fbo, IScheduleOwner owner) throws SchedulingException {
		final IPortletScheduleVisitor currentVisitor = getCurrentVisitor();
		final String visitorUsername = currentVisitor.getAccountId();
		if(LOG.isInfoEnabled()) {
			LOG.info(currentVisitor + " submitting cancelAppointment request with " + owner + " at " + fbo.getTargetBlock());
		}
		EventCancellation result = this.schedulingAssistantService.cancelAppointment(visitorUsername, owner.getId(), fbo.getTargetBlock(), fbo.getReason());
		return result;
	}
	
	/**
	 * 
	 * @param weekStartParam
	 * @return
	 */
	protected int safeConvertWeekStartParam(String weekStartParam) {
		try {
			int result = Integer.parseInt(weekStartParam);
			if(result < 1 ) {
				return 0;
			} else {
				return result;
			}
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
