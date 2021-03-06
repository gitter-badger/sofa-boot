/**
 * Copyright Notice: This software is developed by Ant Small and Micro Financial Services Group Co., Ltd. This software and all the relevant information, including but not limited to any signs, images, photographs, animations, text, interface design,
 *  audios and videos, and printed materials, are protected by copyright laws and other intellectual property laws and treaties.
 *  The use of this software shall abide by the laws and regulations as well as Software Installation License Agreement/Software Use Agreement updated from time to time.
 *   Without authorization from Ant Small and Micro Financial Services Group Co., Ltd., no one may conduct the following actions:
 *
 *   1) reproduce, spread, present, set up a mirror of, upload, download this software;
 *
 *   2) reverse engineer, decompile the source code of this software or try to find the source code in any other ways;
 *
 *   3) modify, translate and adapt this software, or develop derivative products, works, and services based on this software;
 *
 *   4) distribute, lease, rent, sub-license, demise or transfer any rights in relation to this software, or authorize the reproduction of this software on other’s computers.
 */
package com.alipay.sofa.runtime.api.component;

import com.alipay.sofa.runtime.model.ComponentType;

import java.io.Serializable;

/**
 * ComponentName used to identify the component uniquely.
 *
 * @author xuanbei 18/2/28
 */
public class ComponentName implements Serializable {
    private static final long   serialVersionUID = -6856142686482394411L;

    /**
     * component type
     */
    private final ComponentType type;
    /**
     * component name
     */
    private final String        name;
    /**
     * raw name
     */
    private final String        rawName;

    /**
     * build ComponentName by component type and component name
     *
     * @param type component type
     * @param name component name
     */
    public ComponentName(ComponentType type, String name) {
        this.type = type;
        this.name = name;
        this.rawName = this.type.getName() + ":" + this.name;
    }

    public final ComponentType getType() {
        return type;
    }

    /**
     * Gets the name part of the component name.
     *
     * @return the name part
     */
    public final String getName() {
        return name;
    }

    public final String getRawName() {
        return rawName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ComponentName) {
            return rawName.equals(((ComponentName) obj).rawName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return rawName.hashCode();
    }

    @Override
    public String toString() {
        return rawName;
    }

}
