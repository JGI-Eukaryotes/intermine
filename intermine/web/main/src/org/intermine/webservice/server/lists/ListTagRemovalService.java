package org.intermine.webservice.server.lists;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.collections.TransformerUtils.invokerTransformer;
import static org.apache.commons.lang.StringUtils.split;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.tag.TagTypes;
import org.intermine.model.userprofile.Tag;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.WebService;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.ResourceNotFoundException;

public class ListTagRemovalService extends ListTagService {

    public ListTagRemovalService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected void execute(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String listName = request.getParameter("name");
        String[] tags = split(request.getParameter("tags"), ';');
        if (tags == null) {
            throw new BadRequestException("No tags supplied");
        }
        Set<String> tagset = new HashSet<String>(Arrays.asList(tags));

        BagManager bagManager = im.getBagManager();
        Profile profile = SessionMethods.getProfile(request.getSession());
        Map<String, InterMineBag> lists = bagManager.getUserAndGlobalBags(profile);
        InterMineBag list = lists.get(listName);
        if (list == null) {
            throw new ResourceNotFoundException("You do not have access to a list called "
                    + listName);
        }
        TagManager tm = im.getTagManager();

        for (Tag tag: bagManager.getTagsForBag(list)) {
            if (tagset.contains(tag.getTagName())) {
                tm.deleteTag(tag.getTagName(), list.getName(),
                        TagTypes.BAG, profile.getUsername());
            }
        }
        List<Tag> allTags = bagManager.getTagsForBag(list);
        List<String> tagNames = (List<String>) collect(allTags, invokerTransformer("getTagName"));
        output.addResultItem(tagNames);
    }

}