package org.goobi.api.display;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. - https://goobi.io - https://www.intranda.com - https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */

@Data
@AllArgsConstructor
public class Item {
    private String label;
    private String value;
    private boolean selected = false;

    private String source;
    private String field;
    //
    //    /**
    //     * Creates a new item with given params
    //     *
    //     * @param label label of the item
    //     * @param value value of the item
    //     * @param selected indicates wether an item is preselected or not
    //     */
    //
    //    public Item(String label, String value, Boolean selected ){
    //        setLabel(label);
    //        setValue(value);
    //        setIsSelected(selected);
    //
    //    }
    //
    //    /**
    //     *
    //     * @param myLabel sets label for the item
    //     */
    //
    //    public void setLabel(String myLabel) {
    //        this.myLabel = myLabel;
    //    }
    //
    //    /**
    //     *
    //     * @return label of the item
    //     */
    //    public String getLabel() {
    //        return myLabel;
    //    }
    //
    //    /**
    //     *
    //     * @param myValue sets value for the item
    //     */
    //
    //    public void setValue(String myValue) {
    //        this.myValue = myValue;
    //    }
    //
    //    /**
    //     *
    //     * @return value of the item
    //     */
    //    public String getValue() {
    //        return myValue;
    //    }
    //
    //    /**
    //     *
    //     * @param isSelected sets Boolean that indicates whether item is preselected or not
    //     */
    //
    //    public void setIsSelected(Boolean isSelected) {
    //        this.isSelected = isSelected;
    //    }
    //
    //    /**
    //     *
    //     * @return Boolean: is preselected or not
    //     */
    //    public Boolean getIsSelected() {
    //        return isSelected;
    //    }
    //
    //
    //    public String getSource() {
    //        return source;
    //    }
    //
    //    public String getField() {
    //        return field;
    //    }
    //
    //    public void setSource(String source) {
    //        this.source = source;
    //    }
    //
    //    public void setField(String field) {
    //        this.field = field;
    //    }
}
