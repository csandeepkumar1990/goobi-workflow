package org.goobi.production.enums;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *     		- http://www.goobi.org
 *     		- http://launchpad.net/goobi-production
 * 		    - http://gdz.sub.uni-goettingen.de
 * 			- http://www.intranda.com
 * 			- http://digiverso.com 
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
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IImportPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IServletPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;
import org.goobi.production.plugin.interfaces.IValidatorPlugin;

import de.sub.goobi.importer.IOpacPlugin;

public enum PluginType {

    Import(1, "import", IImportPlugin.class), Step(2, "step", IStepPlugin.class), Validation(3, "validation", IValidatorPlugin.class), Command(4,
            "command", ICommandPlugin.class), Servlet(5, "servlet", IServletPlugin.class), Opac(6, "opac", IOpacPlugin.class);

    private int id;
    private String name;
    private Class<IPlugin> interfaz;

    @SuppressWarnings("unchecked")
    private PluginType(int id, String name, Class<? extends IPlugin> inInterfaz) {
        this.id = id;
        this.name = name;
        this.interfaz = (Class<IPlugin>) inInterfaz;
    }

    public static PluginType getTypeFromValue(String pluginType) {
        if (pluginType != null) {
            for (PluginType type : PluginType.values()) {
                if (type.getName().equals(pluginType)) {
                    return type;
                }
            }
        }
        return null;
    }

    public static PluginType getTypesFromId(int pluginType) {
        for (PluginType type : PluginType.values()) {
            if (type.getId() == pluginType) {
                return type;
            }
        }
        return null;
    }

    public int getId() {
        return this.id;
    }

    public Class<IPlugin> getInterfaz() {
        return this.interfaz;
    }

    public String getName() {
        return this.name;
    }

}
