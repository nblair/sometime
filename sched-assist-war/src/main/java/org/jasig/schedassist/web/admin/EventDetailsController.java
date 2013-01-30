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


package org.jasig.schedassist.web.admin;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jasig.schedassist.impl.statistics.AppointmentEvent;
import org.jasig.schedassist.impl.statistics.StatisticsDao;
import org.jasig.schedassist.model.CommonDateOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@link Controller} to display the statistics events.
 * 
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: EventDetailsController.java 2978 2011-01-25 19:20:51Z npblair $
 */
@Controller
@RequestMapping("/admin/event-details.html")
public class EventDetailsController {

	private StatisticsDao statisticsDao;

	/**
	 * @param statisticsDao the statisticsDao to set
	 */
	@Autowired
	public void setStatisticsDao(StatisticsDao statisticsDao) {
		this.statisticsDao = statisticsDao;
	}

	/**
	 * @return the statisticsDao
	 */
	public StatisticsDao getStatisticsDao() {
		return statisticsDao;
	}

	/**
	 * 
	 * @param binder
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		SimpleDateFormat dateFormat = CommonDateOperations.getDateFormat();
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
	}

	/**
	 * Display events for the date specified by the "date" request parameter.
	 * 
	 * @param date
	 * @param model
	 * @return
	 */
	@RequestMapping(method=RequestMethod.GET)
	protected String getEventsForDate(@RequestParam(value="date",required=false) Date date, final ModelMap model) {
		if(null != date) {
			model.addAttribute("date", date);
			List<AppointmentEvent> events = this.statisticsDao.getEvents(
					CommonDateOperations.beginningOfDay(date), 
					CommonDateOperations.endOfDay(date));
			Collections.sort(events);
			model.addAttribute("events", events);
		}
		return "admin/event-details-day";
	}
}
