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

package org.jasig.schedassist.impl.caldav;

import org.apache.http.client.CredentialsProvider;
import org.jasig.schedassist.model.ICalendarAccount;

/**
 * Factory interface for HTTP Client's {@link CredentialsProvider}s.
 * 
 * @author Nicholas Blair
 * @version $Id: CredentialsProviderProvider.java $
 */
public interface CredentialsProviderFactory {

	/**
	 * Return a {@link CredentialsProvider} that can be used when performing caldav operations against
	 * the provided {@link ICalendarAccount}.
	 * 
	 * Implementations may not all use the {@link ICalendarAccount}; some return the same set of
	 * credentials for all inputs.
	 * 
	 * @param account
	 * @return a never null {@link CredentialsProvider} that the data dao can use to interact with the CalDAV server
	 */
	CredentialsProvider getCredentialsProvider(ICalendarAccount account);
}
