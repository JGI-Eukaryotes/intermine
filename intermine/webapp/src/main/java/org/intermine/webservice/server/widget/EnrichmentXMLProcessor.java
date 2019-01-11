package org.intermine.webservice.server.widget;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.model.InterMineId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * A widget result processor for enrichment results that produces output
 * as XML.
 * @author Alex Kalderimis
 *
 */
public final class EnrichmentXMLProcessor implements WidgetResultProcessor
{

    private static final WidgetResultProcessor INSTANCE = new EnrichmentXMLProcessor();

    private EnrichmentXMLProcessor() {
        // Not to be instantiated.
    }

    /** @return a widget result processor of some type **/
    public static WidgetResultProcessor instance() {
        return INSTANCE;
    }

    private static final Map<InterMineId, String> ELEMENTS = new HashMap<InterMineId, String>() {
        private static final long serialVersionUID = 5353373450297092694L;
        {
            put(InterMineId.valueOf(0), "identifier");
            put(InterMineId.valueOf(1), "description");
            put(InterMineId.valueOf(2), "pValue");
            put(InterMineId.valueOf(3), "count");
            put(InterMineId.valueOf(4), "populationAnnotationCount");
        }
    };

    @Override
    public List<String> formatRow(List<Object> row) {
        StringBuffer sb = new StringBuffer("<result>");
        int i = 0;
        for (Object cell: row) {
            sb.append(formatCell(ELEMENTS.get(InterMineId.valueOf(i)), cell));
            i++;
        }
        sb.append("</result>");
        return Arrays.asList(sb.toString());
    }

    private String formatCell(String name, Object contents) {
        StringBuffer sb = new StringBuffer();
        if (contents != null) {
            if (contents instanceof List) {
                for (Object o: (List<?>) contents) {
                    sb.append(formatCell(name, o));
                }
            } else {
                sb.append("<" + name + ">");
                sb.append(StringEscapeUtils.escapeXml(contents.toString()));
                sb.append("</" + name + ">");
            }
        }
        return sb.toString();
    }

}
