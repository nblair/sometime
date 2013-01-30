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


package org.jasig.schedassist.web.owner.relationships;

import javax.validation.Valid;

import org.jasig.schedassist.CalendarAccountNotFoundException;
import org.jasig.schedassist.ICalendarAccountDao;
import org.jasig.schedassist.MutableRelationshipDao;
import org.jasig.schedassist.impl.owner.NotRegisteredException;
import org.jasig.schedassist.impl.visitor.NotAVisitorException;
import org.jasig.schedassist.impl.visitor.VisitorDao;
import org.jasig.schedassist.model.ICalendarAccount;
import org.jasig.schedassist.model.IScheduleOwner;
import org.jasig.schedassist.model.IScheduleVisitor;
import org.jasig.schedassist.web.security.CalendarAccountUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Form controller that invokes {@link MutableRelationshipDao#createRelationship(IScheduleOwner, IScheduleVisitor, String)}.
 *  
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: CreateAdhocRelationshipFormController.java 2049 2010-04-30 16:01:10Z npblair $
 */
@Controller
@RequestMapping(value={"/owner/create-adhoc-relationship.html", "/delegate/create-adhoc-relationship.html"})
@SessionAttributes("command")
public class CreateAdhocRelationshipFormController {

	private ICalendarAccountDao calendarAccountDao;
	private VisitorDao visitorDao;
	private MutableRelationshipDao mutableRelationshipDao;
	private String identifyingAttributeName = "uid";
	/**
	 * 
	 * @param identifyingAttributeName
	 */
	@Value("${users.visibleIdentifierAttributeName:uid}")
	public void setIdentifyingAttributeName(String identifyingAttributeName) {
		this.identifyingAttributeName = identifyingAttributeName;
	}
	/**
	 * 
	 * @return the attribute used to commonly uniquely identify an account
	 */
	public String getIdentifyingAttributeName() {
		return identifyingAttributeName;
	}
	/**
	 * @param calendarAccountDao the calendarAccountDao to set
	 */
	@Autowired
	public void setCalendarAccountDao(ICalendarAccountDao calendarAccountDao) {
		this.calendarAccountDao = calendarAccountDao;
	}
	/**
	 * @param visitorDao the visitorDao to set
	 */
	@Autowired
	public void setVisitorDao(VisitorDao visitorDao) {
		this.visitorDao = visitorDao;
	}
	/**
	 * @param mutableRelationshipDao the mutableRelationshipDao to set
	 */
	@Autowired
	public void setMutableRelationshipDao(
			MutableRelationshipDao mutableRelationshipDao) {
		this.mutableRelationshipDao = mutableRelationshipDao;
	}
	/**
	 * @return the calendarAccountDao
	 */
	public ICalendarAccountDao getCalendarAccountDao() {
		return calendarAccountDao;
	}
	/**
	 * @return the visitorDao
	 */
	public VisitorDao getVisitorDao() {
		return visitorDao;
	}
	/**
	 * @return the mutableRelationshipDao
	 */
	public MutableRelationshipDao getMutableRelationshipDao() {
		return mutableRelationshipDao;
	}
	/**
	 * 
	 * @param binder
	 */
	@InitBinder("command")
    protected void initBinder(WebDataBinder binder) {
        binder.setValidator(new ModifyAdhocRelationshipFormBackingObjectValidator(this.calendarAccountDao));
    }
	/**
	 * 
	 * @param noscript
	 * @return
	 */
	@RequestMapping(method=RequestMethod.GET)
	protected String setupForm(@RequestParam(value="noscript",required=false,defaultValue="false") boolean noscript, final ModelMap model) {
		model.addAttribute("command", new ModifyAdhocRelationshipFormBackingObject());
		if(noscript) {
			return "owner-relationships/create-adhoc-relationship-form-noscript";
		} else {
			return "owner-relationships/create-adhoc-relationship-form";
		}
	}
	/**
	 * 
	 * @param fbo
	 * @return
	 * @throws CalendarAccountNotFoundException
	 * @throws NotAVisitorException
	 * @throws NotRegisteredException 
	 */
	@RequestMapping(method=RequestMethod.POST)
	protected String createRelationship(@Valid @ModelAttribute("command") ModifyAdhocRelationshipFormBackingObject fbo, BindingResult bindResult, final ModelMap model) throws CalendarAccountNotFoundException, NotAVisitorException, NotRegisteredException {
		if(bindResult.hasErrors()) {
			return "owner-relationships/create-adhoc-relationship-form";
		}
		CalendarAccountUserDetails currentUser = (CalendarAccountUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		IScheduleOwner owner = currentUser.getScheduleOwner();
		ICalendarAccount visitorUser = calendarAccountDao.getCalendarAccount(this.identifyingAttributeName, fbo.getVisitorUsername());
		if(null == visitorUser) {
			throw new CalendarAccountNotFoundException(fbo.getVisitorUsername() + " does not exist or is not eligible for WiscCal");
		}
		IScheduleVisitor visitor = visitorDao.toVisitor(visitorUser);
		
		mutableRelationshipDao.createRelationship(owner, visitor, fbo.getRelationship());
		model.addAttribute("visitor", visitor);
		model.addAttribute("relationship", fbo.getRelationship());
		return "owner-relationships/create-adhoc-relationship-success";
		
	}
	
}
