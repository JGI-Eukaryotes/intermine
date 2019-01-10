package org.intermine.api.tracker;

/*
 * Copyright (C) 2002-2018 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.HashMap;
import java.util.Map;

import org.intermine.model.InterMineId;
import org.intermine.api.tracker.track.TemplateTrack;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;

/**
 * Class for saving the template executions into the memory. The template executions are saved into
 * an Map containing as a value an hashmap having as a key the user's name (or the session
 * identifier) and as a value the number of executions for that user's name (or session identifier)
 * @author dbutano
 */
public class TemplatesExecutionMap
{
    protected Map<String, Map<String, InterMineId>> templateExecutions =
                                              new HashMap<String, Map<String, InterMineId>>();

    /**
     * Add a new template track into the map
     * @param templateTrack the template track to add
     */
    public void addExecution(TemplateTrack templateTrack) {
        String executionKey = (templateTrack.getUserName() != null
                              && !"".equals(templateTrack.getUserName()))
                              ? templateTrack.getUserName()
                              : templateTrack.getSessionIdentifier();
        String templateName = templateTrack.getTemplateName();
        Map<String, InterMineId> execution;
        if (!templateExecutions.containsKey(templateName)) {
            execution = new HashMap<String, InterMineId>();
            execution.put(executionKey, InterMineId.valueOf(1));
            templateExecutions.put(templateName, execution);
        }
        else {
            execution = templateExecutions.get(templateName);
            if (!execution.containsKey(executionKey)) {
                execution.put(executionKey, InterMineId.valueOf(1));
            } else {
                execution.put(executionKey, InterMineId.valueOf(execution.get(executionKey).intValue() + 1));
            }
        }
    }

    /**
     * Return a map containing the logarithm's sum of the templates executions launched by
     * the same users or during the same sessions. If the user name is specified, we only
     * consider the templates executed by the user specified.
     * @param executionKey the user's name or the session identifier
     * @param templateManager the template manager used to retrieve the global templates
     * @return map having as key the template's name and as value the logarithm sum
     */
    public Map<String, Double> getLogarithmMap(String executionKey,
                                               TemplateManager templateManager) {
        Map<String, Double> logarithmMap = new HashMap<String, Double>();
        if (executionKey == null) {
            if (templateManager != null) {
                Map<String, ApiTemplate> publicTemplates =
                    templateManager.getValidGlobalTemplates();
                for (String templateName : templateExecutions.keySet()) {
                    if (publicTemplates.containsKey(templateName)) {
                        Map<String, InterMineId> execution = templateExecutions.get(templateName);
                        double accessLn = 0;
                        for (String key : execution.keySet()) {
                            accessLn = accessLn + Math.log(execution.get(key).intValue() + 1);
                        }
                        logarithmMap.put(templateName, Double.valueOf(accessLn));
                    }
                }
            }
        } else {
            for (String templateName : templateExecutions.keySet()) {
                Map<String, InterMineId> execution = templateExecutions.get(templateName);
                if (execution.containsKey(executionKey)) {
                    double accessLn = Math.log(execution.get(executionKey).intValue() + 1);
                    logarithmMap.put(templateName, Double.valueOf(accessLn));
                }
            }
        }
        return logarithmMap;
    }
}
