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
// Generated on: 2010.08.06 at 03:50:33 PM CDT 
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
 *         &lt;element ref="{http://wisccal.wisc.edu/available}AvailableBlockList"/>
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
    "availableBlockList"
})
@XmlRootElement(name = "VisitorConflictsResponse")
public class VisitorConflictsResponse {

    @XmlElement(name = "AvailableBlockList", required = true)
    protected AvailableBlockList availableBlockList;

    /**
     * Gets the value of the availableBlockList property.
     * 
     * @return
     *     possible object is
     *     {@link AvailableBlockList }
     *     
     */
    public AvailableBlockList getAvailableBlockList() {
        return availableBlockList;
    }

    /**
     * Sets the value of the availableBlockList property.
     * 
     * @param value
     *     allowed object is
     *     {@link AvailableBlockList }
     *     
     */
    public void setAvailableBlockList(AvailableBlockList value) {
        this.availableBlockList = value;
    }

}