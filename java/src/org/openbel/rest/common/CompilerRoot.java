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

import static java.lang.String.format;
import static org.openbel.rest.common.Objects.*;
import static org.openbel.rest.Util.*;
import static org.openbel.rest.main.*;
import static org.openbel.framework.common.bel.parser.BELParser.*;

import org.openbel.framework.common.enums.AnnotationType;
import org.openbel.framework.common.model.AnnotationDefinition;
import org.openbel.rest.common.Objects;
import org.jongo.*;
import org.openbel.rest.Path;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.openbel.framework.common.model.*;
import org.openbel.framework.common.bel.parser.BELParseResults;
import org.openbel.bel.model.*;
import org.restlet.representation.Representation;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.data.Status;
import java.util.*;

@Path("/api/v1/compiler")
public class CompilerRoot extends ServerResource {

    @Post("json")
    public Representation _post1(Representation body) {
        if (body == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return null;
        }

        Map<String, Object> map = mapify(body);
        Object stmt_obj = map.get("statement");
        if (stmt_obj == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return null;
        }

        String stmt_str = cast(stmt_obj, String.class);
        if (stmt_str == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return null;
        }
        Statement stmt;
        try {
            stmt = parseStatement(stmt_str);
        } catch (Exception e) {
            stmt = null;
        }

        Validation objv;
        if (stmt == null) objv = new Validation(false);
        else objv = new Validation(true);

        return null;
    }

    @Post("txt")
    public Representation _post2(Representation body) {
        if (body == null) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return null;
        }
        String txt = textify(body);
        return null;
    }

    private static Document document() {
        Find find = $namespaces.find("{}");
        List<Namespace> namespaces = new ArrayList<>();
        for (Map<?, ?> map : find.as(Map.class)) {
            String keyword = (String) map.get("keyword");
            String url = (String) map.get("url");
            namespaces.add(new Namespace(keyword, url));
        }

        AnnotationDefinition ad;
        List<AnnotationDefinitions> annotations = new ArrayList<>();
        find = $annotations.find("{}");
        for (Map<?, ?> map : find.as(Map.class)) {
            String keyword = (String) map.get("keyword");
            String url = (String) map.get("url");
            String desc = (String) map.get("description");
            String usage = (String) map.get("usage");
            AnnotationType at = AnnotationType.ENUMERATION;
            ad = new AnnotationDefinition(keyword, at, desc, usage, url);
            annotations.add(ad);
        }
    }

}
