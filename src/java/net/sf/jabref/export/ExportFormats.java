package net.sf.jabref.export;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.MnemonicAwareAction;
import net.sf.jabref.BibtexEntry;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.util.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 18, 2006
 * Time: 9:35:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExportFormats {

    private static Map exportFormats = new TreeMap();

    public static void initAllExports() {
        exportFormats.clear();
        initBuiltinExports();
        TreeMap customExports = Globals.prefs.customExports.getCustomExportFormats();
        for (Iterator i=customExports.keySet().iterator(); i.hasNext();) {
            putFormat((ExportFormat)customExports.get(i.next()));
        }
    }

    public static void initBuiltinExports() {
        putFormat(new ExportFormat(
                Globals.lang("HTML"), "html", "html", null, ".html"));
        putFormat(new ExportFormat(
                Globals.lang("Simple HTML"), "simplehtml", "simplehtml", null, ".html"));
        putFormat(new ExportFormat(Globals.lang("Docbook"), "docbook", "docbook", null, ".xml"));
        putFormat(new ExportFormat(Globals.lang("BibTeXML"), "bibtexml", "bibtexml", null, ".xml"));
        putFormat(new ModsExportFormat());
        putFormat(new ExportFormat(Globals.lang("HTML table"),
                "tablerefs", "tablerefs", "tablerefs", ".html"));
        putFormat(new ExportFormat(Globals.lang("HTML table (with Abstract & BibTeX)"),
                "tablerefsabsbib", "tablerefsabsbib", "tablerefsabsbib", ".html"));
        putFormat(new ExportFormat(Globals.lang("Harvard RTF"), "harvard", "harvard",
                "harvard", ".rtf"));
        putFormat(new ExportFormat(Globals.lang("Endnote"), "endnote", "EndNote",
                "endnote", ".txt"));
        putFormat(new OpenOfficeDocumentCreator());
        putFormat(new OpenDocumentSpreadsheetCreator());

        //openofficeItem = new JMenuItem("OpenOffice Calc"),
        //odsItem = new JMenuItem("OpenDocument Spreadsheet");

    }

    /**
     * Build a string listing all available export formats.
     * @param maxLineLength The max line length before a line break must be added.
     * @param linePrefix If a line break is added, this prefix will be inserted
     *  at the beginning of the next line.
     * @return The string describing available formats.
     */
    public static String getConsoleExportList(int maxLineLength, int firstLineSubtr,
                                              String linePrefix) {
        StringBuffer sb = new StringBuffer();
        int lastBreak = -firstLineSubtr;

        for (Iterator i = exportFormats.keySet().iterator(); i.hasNext();) {
            String name = (String)i.next();
            if (sb.length() + 2 + name.length() - lastBreak > maxLineLength) {
                sb.append(",\n");
                lastBreak = sb.length();
                sb.append(linePrefix);
            } else if (sb.length() > 0)
                sb.append(", ");
            sb.append(name);
        }

        return sb.toString();
    }

    /**
     * Look up the named export format.
     * @param consoleName The export name given in the JabRef console help information.
     * @return The ExportFormat, or null if no exportformat with that name is registered.
     */
    public static ExportFormat getExportFormat(String consoleName) {
        return (ExportFormat)exportFormats.get(consoleName);
    }

    /**
     * Create an AbstractAction for performing an export operation.
     * @param frame The JabRefFrame of this JabRef instance.
     * @param selectedOnly true indicates that only selected entries should be exported,
     *  false indicates that all entries should be exported.
     * @return The action.
     */
    public static AbstractAction getExportAction(JabRefFrame frame, boolean selectedOnly) {

        class ExportAction extends MnemonicAwareAction {
            private JabRefFrame frame;
            private boolean selectedOnly;

            public ExportAction(JabRefFrame frame, boolean selectedOnly) {
                this.frame = frame;
                this.selectedOnly = selectedOnly;
                putValue(NAME, selectedOnly ? "Export selected entries" : "Export");
            }

            public void actionPerformed(ActionEvent e) {
                ExportFormats.initAllExports();
                JFileChooser fc = ExportFormats.createExportFileChooser("/home/alver/Documents");
                fc.showSaveDialog(frame);
                File file = fc.getSelectedFile();
                if (file == null)
                    return;
                FileFilter ff = fc.getFileFilter();
                if (ff instanceof ExportFileFilter) {
                    try {
                        ExportFormat format = ((ExportFileFilter) ff).getExportFormat();
                        Set entryIds = null;
                        if (selectedOnly) {
                            BibtexEntry[] selected = frame.basePanel().getSelectedEntries();
                            entryIds = new HashSet();
                            for (int i = 0; i < selected.length; i++) {
                                BibtexEntry bibtexEntry = selected[i];
                                entryIds.add(bibtexEntry.getId());
                            }
                        }
                        format.performExport(frame.basePanel().database(),
                                file.getPath(), "UTF8", entryIds);
                        // Make sure we remember which filter was used, to set the default
                        // for next time:
                        Globals.prefs.put("lastUsedExport", format.getConsoleName());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        return new ExportAction(frame, selectedOnly);
    }

    public static JFileChooser createExportFileChooser(String currentDir) {
        String lastUsedFormat = Globals.prefs.get("lastUsedExport");
        FileFilter defaultFilter = null;
        JFileChooser fc = new JFileChooser(currentDir);
        for (Iterator i = exportFormats.keySet().iterator(); i.hasNext();) {
            String formatName = (String)i.next();
            ExportFormat format = (ExportFormat)exportFormats.get(formatName);
            fc.addChoosableFileFilter(format.getFileFilter());
            if (formatName.equals(lastUsedFormat))
                defaultFilter = format.getFileFilter();

        }
        fc.setAcceptAllFileFilterUsed(false);
        if (defaultFilter != null)
            fc.setFileFilter(defaultFilter);
        return fc;
    }

    private static void putFormat(ExportFormat format) {
        exportFormats.put(format.getConsoleName(), format);
    }



}