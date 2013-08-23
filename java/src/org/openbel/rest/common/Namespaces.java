/**
 *  Copyright 2013 OpenBEL Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.openbel.rest.common;

import static java.lang.String.format;
import static org.openbel.rest.common.Objects.*;
import static org.openbel.rest.Util.*;
import static org.openbel.rest.main.*;
import org.openbel.rest.common.Objects;
import org.jongo.*;
import org.openbel.rest.Path;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.representation.Representation;
import org.restlet.data.Status;
import java.util.*;

@Path("/api/v1/namespaces/{keyword}")
public class Namespaces extends ServerResource {
    private static final String RSRC_FIND;
    static {
        RSRC_FIND = "{keyword: '%s'}";
    }

    @Get("json")
    public Representation _get() {
        String keyword = getAttribute("keyword");
        String query = format(RSRC_FIND, keyword);
        Map<?, ?> ns = $namespaces.findOne(query).as(Map.class);
        if (ns == null) {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return null;
        }

        String name = (String) ns.get("name");
        String kword = (String) ns.get("keyword");
        String desc = (String) ns.get("description");
        Objects.Namespace objn = new Objects.Namespace(name, kword, desc);

        // links
        String path = declaredPath(Objects.Namespaces.class);
        objn.addLink("self", urlify(path, keyword));
        objn.addLink("related", urlify(path, keyword, "values"));
        NamespacesRoot.linkResource(objn);
        return objn.json();
    }

}
