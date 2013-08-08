/**
 * Copyright (C) 2013 Selventa, Inc.
 *
 * This file is part of the BEL Framework REST API.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The BEL Framework REST API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the BEL Framework REST API. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional Terms under LGPL v3:
 *
 * This license does not authorize you and you are prohibited from using the
 * name, trademarks, service marks, logos or similar indicia of Selventa, Inc.,
 * or, in the discretion of other licensors or authors of the program, the
 * name, trademarks, service marks, logos or similar indicia of such authors or
 * licensors, in any marketing or advertising materials relating to your
 * distribution of the program or any covered product. This restriction does
 * not waive or limit your obligation to keep intact all copyright notices set
 * forth in the program as delivered to you.
 *
 * If you distribute the program in whole or in part, or any modified version
 * of the program, and you assume contractual liability to the recipient with
 * respect to the program or modified version, then you will indemnify the
 * authors and licensors of the program for any liabilities that these
 * contractual assumptions directly impose on those licensors and authors.
 */
package org.openbel.rest.common;

import static org.openbel.rest.common.Objects.*;
import static org.openbel.rest.main.*;
import static org.openbel.rest.Util.*;
import static java.util.regex.Pattern.*;
import static java.lang.String.format;
import org.jongo.*;
import java.util.*;
import java.util.regex.*;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.representation.Representation;
import org.restlet.data.Status;
import org.openbel.rest.Path;

@Path("/api/v1/completion/annotation/{keyword}")
public class AnnotationKeywordCompletion extends ServerResource {
    private static final String ALT_URL = "/api/v1/completion/annotation/";
    private static final String FIND;
    static {
        FIND = "{norm:#}";
    }

    @Get("json")
    public Representation _get() {
        String keyword = getAttribute("keyword");
    	String input = format("^%s", escapeRE(keyword));
    	Pattern ptrn = compile(input.toLowerCase());
        Find find = $annotations.find(FIND, ptrn);

        List<String> rslts = new ArrayList<>();
        for (Map<?, ?> map : find.as(Map.class)) {
            rslts.add((String) map.get("keyword"));
        }

        if (rslts.size() == 1) {
            AOKeywordCompletion ret = new AOKeywordCompletion();
            ret.addValue(rslts.get(0));
            ret.addLink("self", ALT_URL + keyword);
            return ret.json();
        }

        AOKeywordCompletion ret = new AOKeywordCompletion();
        for (String rslt : rslts) {
            ret.addLink("result", ALT_URL + rslt);
            ret.addValue(rslt);
        }
        ret.addLink("self", ALT_URL + keyword);
        setStatus(Status.REDIRECTION_MULTIPLE_CHOICES);
        return ret.json();
    }

}