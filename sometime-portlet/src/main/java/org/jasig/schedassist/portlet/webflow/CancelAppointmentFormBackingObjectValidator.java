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

import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;
import org.springframework.binding.validation.ValidationContext;
import org.springframework.stereotype.Component;


/**
 * Validator of {@link CancelAppointmentFormBackingObject}s.
 * 
 * @author Nicholas Blair, nblair@doit.wisc.edu
 * @version $Id: CancelAppointmentFormBackingObjectValidator.java $
 */
@Component
public class CancelAppointmentFormBackingObjectValidator {

	/**
	 * Validate the {@link CancelAppointmentFormBackingObject} after transitioning
	 * from the "showCancelForm" state.
	 * 
	 * @param fbo
	 * @param context
	 */
	public void validateShowCancelForm(CancelAppointmentFormBackingObject fbo, ValidationContext context) {
		 MessageContext messages = context.getMessageContext();
		 if(!fbo.isConfirmCancel()) {
			 messages.addMessage(new MessageBuilder().error().source("confirmCancel").
		                defaultText("Please check the box to confirm your intent to cancel this appointment.").build());
		 }
	}
}
