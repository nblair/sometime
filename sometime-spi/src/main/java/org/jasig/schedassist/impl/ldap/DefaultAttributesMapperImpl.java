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

/**
 * 
 */
package org.jasig.schedassist.impl.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.springframework.ldap.core.AttributesMapper;

/**
 * Default {@link AttributesMapper} implementation.
 * 
 * @author Nicholas Blair
 * @version $Id: DefaultAttributesMapperImpl.java $
 */
public class DefaultAttributesMapperImpl implements AttributesMapper {

	protected final LDAPAttributesKey ldapAttributesKey;
	/**
	 * 
	 * @param ldapAttributesKey
	 */
	public DefaultAttributesMapperImpl(LDAPAttributesKey ldapAttributesKey) {
		this.ldapAttributesKey = ldapAttributesKey;
	}
	/* (non-Javadoc)
	 * @see org.springframework.ldap.core.AttributesMapper#mapFromAttributes(javax.naming.directory.Attributes)
	 */
	@Override
	public Object mapFromAttributes(Attributes attributes) throws NamingException {
		Map<String, List<String>> attributesMap = convertToStringAttributesMap(attributes);
		
		LDAPPersonCalendarAccountImpl account = new LDAPPersonCalendarAccountImpl(attributesMap, ldapAttributesKey);
		return account;
	}
	
	/**
	 * 
	 * @param attributes
	 * @return
	 * @throws NamingException
	 */
	protected final Map<String, List<String>> convertToStringAttributesMap(Attributes attributes) throws NamingException {
		Map<String, List<String>> attributesMap = new HashMap<String, List<String>>();
		
		NamingEnumeration<String> attributeNames = attributes.getIDs();
		while(attributeNames.hasMore()) {
			String attributeName = attributeNames.next();
			if(ldapAttributesKey.getPasswordAttributeName().equalsIgnoreCase(attributeName)) {
				// skip
				continue;
			}
			Attribute attribute = attributes.get(attributeName);
			int numberOfValues = attribute.size();
			for(int i = 0; i < numberOfValues; i++) {
				String value = (String) attribute.get(i);
				if(null != value) {
					value = value.trim();
				}
			
				List<String> list = safeGetAttributeList(attributesMap, attributeName.toLowerCase());
				list.add(value);
			}
		}
		return attributesMap;
	}
	/**
	 * 
	 * @param attributesMap
	 * @param attributeName
	 * @return
	 */
	protected List<String> safeGetAttributeList(Map<String, List<String>> attributesMap, String attributeName) {
		List<String> list = attributesMap.get(attributeName);
		if(list == null) {
			list = new ArrayList<String>();
			attributesMap.put(attributeName, list);
		}
		
		return list;
	}

}
