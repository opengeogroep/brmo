//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.07 at 02:40:39 PM CET 
//


package nl.b3p.topnl.top100nl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import org.w3c.dom.Element;


/**
 * <p>Java class for InrichtingselementType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InrichtingselementType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://register.geostandaarden.nl/gmlapplicatieschema/top100nl/1.1.0}_Top100nlObjectType">
 *       &lt;sequence>
 *         &lt;element name="geometrie" type="{http://register.geostandaarden.nl/gmlapplicatieschema/top100nl/1.1.0}LijnOfPuntPropertyType"/>
 *         &lt;element name="hoogteNiveau" type="{http://www.w3.org/2001/XMLSchema}integer"/>
 *         &lt;element name="naamFries" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="naamNL" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="nummer" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="status" type="{http://register.geostandaarden.nl/gmlapplicatieschema/top100nl/1.1.0}StatusT100Type"/>
 *         &lt;element name="typeInrichtingsElement" type="{http://register.geostandaarden.nl/gmlapplicatieschema/top100nl/1.1.0}TypeInrichtingselementT100Type"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InrichtingselementType", namespace = "http://register.geostandaarden.nl/gmlapplicatieschema/top100nl/1.1.0", propOrder = {
    "geometrie",
    "hoogteNiveau",
    "naamFries",
    "naamNL",
    "nummer",
    "status",
    "typeInrichtingsElement"
})
public class InrichtingselementType
    extends Top100NlObjectType
{

    @XmlAnyElement
    protected Element geometrie;
    @XmlElement(required = true)
    protected BigInteger hoogteNiveau;
    protected List<String> naamFries;
    protected List<String> naamNL;
    protected List<String> nummer;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected StatusT100Type status;
    @XmlElement(required = true)
    @XmlSchemaType(name = "string")
    protected TypeInrichtingselementT100Type typeInrichtingsElement;

    /**
     * Gets the value of the geometrie property.
     * 
     * @return
     *     possible object is
     *     {@link Element }
     *     
     */
    public Element getGeometrie() {
        return geometrie;
    }

    /**
     * Sets the value of the geometrie property.
     * 
     * @param value
     *     allowed object is
     *     {@link Element }
     *     
     */
    public void setGeometrie(Element value) {
        this.geometrie = value;
    }

    /**
     * Gets the value of the hoogteNiveau property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getHoogteNiveau() {
        return hoogteNiveau;
    }

    /**
     * Sets the value of the hoogteNiveau property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setHoogteNiveau(BigInteger value) {
        this.hoogteNiveau = value;
    }

    /**
     * Gets the value of the naamFries property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the naamFries property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNaamFries().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getNaamFries() {
        if (naamFries == null) {
            naamFries = new ArrayList<String>();
        }
        return this.naamFries;
    }

    /**
     * Gets the value of the naamNL property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the naamNL property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNaamNL().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getNaamNL() {
        if (naamNL == null) {
            naamNL = new ArrayList<String>();
        }
        return this.naamNL;
    }

    /**
     * Gets the value of the nummer property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the nummer property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNummer().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getNummer() {
        if (nummer == null) {
            nummer = new ArrayList<String>();
        }
        return this.nummer;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link StatusT100Type }
     *     
     */
    public StatusT100Type getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link StatusT100Type }
     *     
     */
    public void setStatus(StatusT100Type value) {
        this.status = value;
    }

    /**
     * Gets the value of the typeInrichtingsElement property.
     * 
     * @return
     *     possible object is
     *     {@link TypeInrichtingselementT100Type }
     *     
     */
    public TypeInrichtingselementT100Type getTypeInrichtingsElement() {
        return typeInrichtingsElement;
    }

    /**
     * Sets the value of the typeInrichtingsElement property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeInrichtingselementT100Type }
     *     
     */
    public void setTypeInrichtingsElement(TypeInrichtingselementT100Type value) {
        this.typeInrichtingsElement = value;
    }

}
