package de.sub.goobi.metadaten;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi-workflow
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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

import de.sub.goobi.config.ConfigurationHelper;

/**
 * Bean für die Sperrung der Metadaten
 */
public class MetadatenSperrung implements Serializable {
    private static final long serialVersionUID = -8248209179063050307L;
    private static HashMap<Integer, HashMap<String, String>> sperrungen = new HashMap<Integer, HashMap<String, String>>();
    /*
     * Zeit, innerhalb der der Benutzer handeln muss, um seine Sperrung zu
     * behalten (30 min)
     */
    private static final long sperrzeit = ConfigurationHelper.getInstance().getMetsEditorLockingTime();

    /* =============================================================== */

    /**
     * Metadaten eines bestimmten Prozesses wieder freigeben
     */
    public void setFree(int prozessID) {
        if (sperrungen.containsKey(prozessID)) {
            sperrungen.remove(prozessID);
        }
    }

    /* =============================================================== */

    /**
     * Metadaten eines bestimmten Prozesses für einen Benutzer sperren
     */
    public void setLocked(int prozessID, String benutzerID) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Benutzer", benutzerID);
        map.put("Lebenszeichen", String.valueOf(System.currentTimeMillis()));
        sperrungen.put(prozessID, map);
    }

    /* =============================================================== */

    /**
     * prüfen, ob bestimmte Metadaten noch durch anderen Benutzer gesperrt sind
     */
    public static boolean isLocked(int prozessID) {
        HashMap<String, String> temp = sperrungen.get(Integer.valueOf(prozessID));
        /* wenn der Prozess nicht in der Hashpmap ist, ist er nicht gesperrt */
        if (temp == null) {
            return false;
        } else {
            /* wenn er in der Hashmap ist, muss die Zeit geprüft werden */
            long lebenszeichen = Long.parseLong(temp.get("Lebenszeichen"));
	        /*
	         * wenn die Zeit (auf der rechten Seite) Größer ist als erlaubt (lebenszeichen), ist Metadatum nicht gesperrt, also "false"
             * wenn Zeit (auf der rechten Seite) nicht Größer ist, ist er noch gesperrt, also "true"
             */
            return lebenszeichen >= System.currentTimeMillis() - sperrzeit;
        }
    }

    /* =============================================================== */

    public void alleBenutzerSperrungenAufheben(Integer inBenutzerID) {
        String inBenutzerString = String.valueOf(inBenutzerID.intValue());
        HashMap<Integer, HashMap<String, String>> temp = new HashMap<Integer, HashMap<String, String>>(sperrungen);
        for (Iterator<Integer> iter = temp.keySet().iterator(); iter.hasNext();) {
            Integer myKey = iter.next();
            HashMap<String, String> intern = sperrungen.get(myKey);
            if (intern.get("Benutzer").equals(inBenutzerString)) {
                sperrungen.remove(myKey);
            }
        }
    }

    /* =============================================================== */

    /**
     * Benutzer zurückgeben, der Metadaten gesperrt hat
     */
    public String getLockBenutzer(int prozessID) {
        String rueckgabe = "-1";
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /* wenn der Prozess nicht in der Hashpmap ist, gibt es keinen Benutzer */
        if (temp != null) {
            rueckgabe = temp.get("Benutzer");
        }
        return rueckgabe;
    }

    /* =============================================================== */

    /**
     * Sperrung fuer Vorgang aufheben
     */
    public static void UnlockProcess(int prozessID) {
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /* wenn der Prozess in der Hashpmap ist, dort rausnehmen */
        if (temp != null) {
            sperrungen.remove(prozessID);
        }
    }

    /* =============================================================== */

    /**
     * Sekunden zurückgeben, seit der letzten Bearbeitung der Metadaten
     */
    public long getLockSekunden(int prozessID) {
        HashMap<String, String> temp = sperrungen.get(prozessID);
        /* wenn der Prozess nicht in der Hashmap ist, gibt es keine Zeit */
        if (temp == null) {
            return 0;
        } else {
            return (System.currentTimeMillis() - Long.parseLong(temp.get("Lebenszeichen"))) / 1000;
        }
    }
}
