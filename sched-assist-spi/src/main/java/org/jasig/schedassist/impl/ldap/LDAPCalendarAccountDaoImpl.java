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

package org.jasig.schedassist.impl.ldap;

import java.util.Collections;
import java.util.List;

import javax.naming.directory.SearchControls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.schedassist.ICalendarAccountDao;
import org.jasig.schedassist.model.ICalendarAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.ldap.SizeLimitExceededException;
import org.springframework.ldap.TimeLimitExceededException;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;

import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.KeyGenerator;


/**
 * LDAP backed {@link ICalendarAccountDao} implementation.
 * Returns "People" calendar accounts, e.g {@link LDAPPersonCalendarAccountImpl}.
 * 
 * @author Nicholas Blair
 */
public class LDAPCalendarAccountDaoImpl implements ICalendarAccountDao {

	private static final String OBJECTCLASS = "objectclass";
	private static final String WILD = "*";
	private LdapTemplate ldapTemplate;
	private String baseDn = "o=isp";

	private LDAPAttributesKey ldapAttributesKey = new LDAPAttributesKeyImpl();
	private long searchResultsLimit = 25L;
	private int searchTimeLimit = 5000;
	private final Log log = LogFactory.getLog(this.getClass());
	private boolean enforceSpecificObjectClass = false;
	private String requiredObjectClass = "inetorgperson";
	
	/**
	 * @param ldapTemplate the ldapTemplate to set
	 */
	@Autowired
	public void setLdapTemplate(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}
	/**
	 * @param baseDn the baseDn to set
	 */
	@Value("${ldap.userAccountBaseDn:o=isp}")
	public void setBaseDn(String baseDn) {
		this.baseDn = baseDn;
	}
	/**
	 * @param ldapAttributesKey the ldapAttributesKey to set
	 */
	@Autowired(required=false)
	public void setLdapAttributesKey(LDAPAttributesKey ldapAttributesKey) {
		this.ldapAttributesKey = ldapAttributesKey;
	}
	/**
	 * @param searchResultsLimit the searchResultsLimit to set
	 */
	@Value("${ldap.searchResultsLimit:25}")
	public void setSearchResultsLimit(long searchResultsLimit) {
		this.searchResultsLimit = searchResultsLimit;
	}
	/**
	 * @param searchTimeLimit the searchTimeLimit to set
	 */
	@Value("${ldap.searchTimeLimitMillis:5000}")
	public void setSearchTimeLimit(int searchTimeLimit) {
		this.searchTimeLimit = searchTimeLimit;
	}
	/**
	 * @param enforceSpecificObjectClass the enforceSpecificObjectClass to set
	 */
	@Value("${ldap.accounts.enforceSpecificObjectClass:false}")
	public void setEnforceSpecificObjectClass(boolean enforceSpecificObjectClass) {
		this.enforceSpecificObjectClass = enforceSpecificObjectClass;
	}
	/**
	 * @param requiredObjectClass the requiredObjectClass to set
	 */
	@Value("${ldap.accounts.requiredObjectClass:inetorgperson}")
	public void setRequiredObjectClass(String requiredObjectClass) {
		this.requiredObjectClass = requiredObjectClass;
	}
	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarAccountDao#getCalendarAccount(java.lang.String)
	 */
	@Override
	@Cacheable(cacheName="userAccountCache", keyGenerator=@KeyGenerator(name="StringCacheKeyGenerator"))
	public ICalendarAccount getCalendarAccount(String username) {
		Filter filter = new EqualsFilter(ldapAttributesKey.getUsernameAttributeName(), username);
		if(enforceSpecificObjectClass) {
			filter = andObjectClass(filter);
		}
		return executeSearch(filter);
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarAccountDao#getCalendarAccountFromUniqueId(java.lang.String)
	 */
	@Override
	@Cacheable(cacheName="userAccountCache", keyGenerator=@KeyGenerator(name="StringCacheKeyGenerator"))
	public ICalendarAccount getCalendarAccountFromUniqueId(
			String calendarUniqueId) {
		return getCalendarAccount(ldapAttributesKey.getUniqueIdentifierAttributeName(), calendarUniqueId);
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarAccountDao#getCalendarAccount(java.lang.String, java.lang.String)
	 */
	@Override
	@Cacheable(cacheName="userAccountCache", keyGenerator=@KeyGenerator(name="StringCacheKeyGenerator"))
	public ICalendarAccount getCalendarAccount(String attributeName,
			String attributeValue) {
		Filter filter = new EqualsFilter(attributeName, attributeValue);
		if(enforceSpecificObjectClass) {
			filter = andObjectClass(filter);
		}
		return executeSearch(filter);
	}

	/* (non-Javadoc)
	 * @see org.jasig.schedassist.ICalendarAccountDao#searchForCalendarAccounts(java.lang.String)
	 */
	@Override
	@Cacheable(cacheName="userAccountCache", keyGenerator=@KeyGenerator(name="StringCacheKeyGenerator"))
	public List<ICalendarAccount> searchForCalendarAccounts(String searchText) {
		AndFilter filter = new AndFilter();
		
		StringBuilder wildSearchText = new StringBuilder();
		wildSearchText.append(WILD);
		wildSearchText.append(searchText.replace(" ", WILD));
		wildSearchText.append(WILD);
		
		OrFilter orFilter = new OrFilter();
		orFilter.or(new LikeFilter(ldapAttributesKey.getUsernameAttributeName(), wildSearchText.toString()));
		orFilter.or(new LikeFilter(ldapAttributesKey.getDisplayNameAttributeName(), wildSearchText.toString()));
		
		filter.and(orFilter);
		// guarantee search for users with calendar attributes
		filter.and(new LikeFilter(ldapAttributesKey.getUniqueIdentifierAttributeName(), WILD));
		
		if(enforceSpecificObjectClass) {
			filter.and(new EqualsFilter(OBJECTCLASS, requiredObjectClass));
		}
		return executeSearchReturnList(filter);
	}

	/**
	 * Wraps the {@link Filter} argument with an {@link AndFilter} which
	 * includes an objectclass=requiredObjectClass filter.
	 * 
	 * @param filter
	 * @return
	 */
	protected AndFilter andObjectClass(Filter filter) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(filter);
		andFilter.and(new EqualsFilter(OBJECTCLASS, requiredObjectClass));
		return andFilter;
	}
	/**
	 * 
	 * @param searchFilter
	 * @return
	 */
	protected ICalendarAccount executeSearch(final Filter searchFilter) {
		List<ICalendarAccount> results = executeSearchReturnList(searchFilter);
		ICalendarAccount result = DataAccessUtils.singleResult(results);
		return result;
	}
	/**
	 * 
	 * @param searchFilter
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<ICalendarAccount> executeSearchReturnList(final Filter searchFilter) {
		log.debug("executing search filter: " + searchFilter);
		
		SearchControls sc = new SearchControls();
		sc.setCountLimit(searchResultsLimit);
		sc.setTimeLimit(searchTimeLimit);
		sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
		
		List<ICalendarAccount> results = Collections.emptyList();
		try {
			results = ldapTemplate.search(baseDn, searchFilter.toString(), sc, new DefaultContextMapperImpl(ldapAttributesKey));
		} catch (SizeLimitExceededException e) {
			log.debug("search filter exceeded results size limit(" + searchResultsLimit +"): " + searchFilter);
		} catch (TimeLimitExceededException e) {
			log.warn("search filter exceeded time limit (" + searchTimeLimit + " milliseconds): " + searchFilter);
		}
		return results;
	}
}
