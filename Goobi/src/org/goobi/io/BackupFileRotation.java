/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi
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

package org.goobi.io;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.Logger; import org.apache.logging.log4j.LogManager;

import de.sub.goobi.helper.StorageProvider;

/**
 * Creates backup for files in a given directory that match a regular expression.
 * 
 * All backup files are named by the original file with a number appended. The bigger the number, the older the backup. A specified maximum number of
 * backup files are generated:
 * 
 * <pre>
 * file.xml	// would be the original
 * file.xml.1	// the latest backup
 * file.xml.2	// an older backup
 * ...
 * file.xml.6	// the oldest backup, if maximum number was 6
 * </pre>
 */
public class BackupFileRotation {

    private static final Logger logger = LogManager.getLogger(BackupFileRotation.class);

    private int numberOfBackups;
    private String format;
    private String processDataDirectory;

    /**
     * Start the configured backup.
     * 
     * If the maximum backup count is less then 1, nothing happens.
     */
    public void performBackup() {
        List<Path> metaFiles;

        if (numberOfBackups < 1) {
            return;
        }

        metaFiles = generateBackupBaseNameFileList(format, processDataDirectory);

        if (metaFiles.size() < 1) {
            logger.info("No files matching format '" + format + "' in directory " + processDataDirectory + " found.");
            return;
        }

        for (Path metaFile : metaFiles) {
            createBackupForFile(metaFile.toString());
        }
    }

    /**
     * Set the number of backup files to create for each individual original file.
     * 
     * @param numberOfBackups Maximum number of backup files
     */
    public void setNumberOfBackups(int numberOfBackups) {
        this.numberOfBackups = numberOfBackups;
    }

    /**
     * Set file name matching pattern for original files to create backup files for.
     * 
     * @param format Java regular expression string.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Set the directory to find the original files and to place the backup files.
     * 
     * @param processDataDirectory A platform specfic filesystem path
     */
    public void setProcessDataDirectory(String processDataDirectory) {
        this.processDataDirectory = processDataDirectory;
    }

    private void rename(String oldFileName, String newFileName) {
        try {
            StorageProvider.getInstance().renameTo(Paths.get(oldFileName), newFileName);
        } catch (IOException ioe) {
            logger.trace("Renaming file from " + oldFileName + " to " + newFileName + " failed. Reason: " + ioe.getMessage());
        }

    }

    private void createBackupForFile(String fileName) {
        rotateBackupFilesFor(fileName);

        String newName = fileName + ".1";
        rename(fileName, newName);
    }

    private void rotateBackupFilesFor(String fileName) {
        for (int count = numberOfBackups; count > 1; count--) {
            String oldName = fileName + "." + (count - 1);
            String newName = fileName + "." + count;
            rename(oldName, newName);
        }
    }

    private List<Path> generateBackupBaseNameFileList(String filterFormat, String directoryOfBackupFiles) {
        return StorageProvider.getInstance().listFiles(directoryOfBackupFiles, new FileListFilter(filterFormat));
    }

}