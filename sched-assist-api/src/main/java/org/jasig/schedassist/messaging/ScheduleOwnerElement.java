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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.04.29 at 01:07:29 PM CDT 
//


package org.jasig.schedassist.messaging;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="fullName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="netid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element ref="{http://wisccal.wisc.edu/available}PreferencesSet"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "id",
    "fullName",
    "netid",
    "preferencesSet"
})
@XmlRootElement(name = "ScheduleOwnerElement")
public class ScheduleOwnerElement {

    protected long id;
    @XmlElement(required = true)
    protected String fullName;
    @XmlElement(required = true)
    protected String netid;
    @XmlElement(name = "PreferencesSet", required = true)
    protected PreferencesSet preferencesSet;

    /**
     * Gets the value of the id property.
     * 
     */
    public long getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     */
    public void setId(long value) {
        this.id = value;
    }

    /**
     * Gets the value of the fullName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the value of the fullName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFullName(String value) {
        this.fullName = value;
    }

    /**
     * Gets the value of the netid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNetid() {
        return netid;
    }

    /**
     * Sets the value of the netid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNetid(String value) {
        this.netid = value;
    }

    /**
     * Gets the value of the preferencesSet property.
     * 
     * @return
     *     possible object is
     *     {@link PreferencesSet }
     *     
     */
    public PreferencesSet getPreferencesSet() {
        return preferencesSet;
    }

    /**
     * Sets the value of the preferencesSet property.
     * 
     * @param value
     *     allowed object is
     *     {@link PreferencesSet }
     *     
     */
    public void setPreferencesSet(PreferencesSet value) {
        this.preferencesSet = value;
    }

}
