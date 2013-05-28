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
import org.openbel.rest.common.Objects;
import org.restlet.representation.Representation;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.data.Status;
import java.util.*;

import org.openbel.framework.compiler.DefaultPhaseOne;
import org.openbel.framework.compiler.PhaseOneImpl;
import org.openbel.framework.core.*;
import org.openbel.framework.core.annotation.*;
import org.openbel.framework.core.compiler.*;
import org.openbel.framework.core.compiler.expansion.*;
import org.openbel.framework.core.df.cache.*;
import org.openbel.framework.core.indexer.IndexingFailure;
import org.openbel.framework.core.namespace.*;
import org.openbel.framework.core.protocol.ResourceDownloadError;
import org.openbel.framework.core.protonetwork.*;


@Path("/api/v1/compiler")
public class CompilerRoot extends ServerResource {
    private static final Document DOCUMENT;
    static {
        DOCUMENT = document();
    }
    private final DefaultPhaseOne p1;
    private final XBELValidatorService validator;
    private final XBELConverterService converter;
    private final BELValidatorService bv;
    private final BELConverterService bc;
    private final NamespaceIndexerService nsi;
    private final CacheableResourceService cache;
    private final CacheLookupService cl;
    private final NamespaceService nss;
    private final ProtoNetworkService pnsvc;
    private final SemanticService semantics;
    private final ExpansionService expansion;
    private final AnnotationService annosvc;
    private final AnnotationDefinitionService ads;

    {
        try {
            validator = new XBELValidatorServiceImpl();
            converter = new XBELConverterServiceImpl();
            bv = new BELValidatorServiceImpl();
            bc = new BELConverterServiceImpl();
            cache = new DefaultCacheableResourceService();
            cl = new DefaultCacheLookupService();
            nsi = new NamespaceIndexerServiceImpl();
            nss = new DefaultNamespaceService(cache, cl, nsi);
            semantics = new SemanticServiceImpl(nss);
            expansion = new ExpansionServiceImpl();
            pnsvc = new ProtoNetworkServiceImpl();
            annosvc = new DefaultAnnotationService();
            ads = new DefaultAnnotationDefinitionService(cache, cl);
            p1 = new PhaseOneImpl(validator, converter, bv, bc, nss, semantics,
                                  expansion, pnsvc, annosvc, ads);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


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

        Objects.Validation objv;
        if (stmt == null) {
            objv = new Objects.Validation(false);
            return objv.json();
        }
        else objv = new Objects.Validation(true);

        Document document = DOCUMENT.clone();

        // Fix all parameter namespaces
        Map<String, Namespace> nsmap = document.getNamespaceMap();
        for (final Term term : stmt) {
            for (final Parameter param : term) {
                Namespace ns = param.getNamespace();
                if (ns == null) continue;
                Namespace known = nsmap.get(ns.getPrefix());
                if (known == null) continue;
                param.setNamespace(known);
            }
        }

        StatementGroup sg = document.getStatementGroups().get(0);
        List<Statement> stmts = new ArrayList<>();
        stmts.add(stmt);
        sg.setStatements(stmts);

        List<String> messages = new ArrayList<>();
        try {
            p1.stage2NamespaceCompilation(document);
        } catch (IndexingFailure f) {
            messages.add(f.getUserFacingMessage());
        } catch (ResourceDownloadError e) {
            messages.add(e.getUserFacingMessage());
        }

        try {
            p1.stage3SymbolVerification(document);
        } catch (SymbolWarning w) {
            messages.add(w.getUserFacingMessage());
        } catch (IndexingFailure f) {
            messages.add(f.getUserFacingMessage());
        } catch (ResourceDownloadError e) {
            messages.add(e.getUserFacingMessage());
        }

        try {
            p1.stage4SemanticVerification(document);
        } catch (SemanticFailure f) {
            messages.add(f.getUserFacingMessage());
        } catch (IndexingFailure f) {
            messages.add(f.getUserFacingMessage());
        }

        objv.put("messages", messages);
        return objv.json();
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
        List<AnnotationDefinition> definitions = new ArrayList<>();
        find = $annotations.find("{}");
        for (Map<?, ?> map : find.as(Map.class)) {
            String keyword = (String) map.get("keyword");
            String url = (String) map.get("url");
            String desc = (String) map.get("description");
            String usage = (String) map.get("usage");
            AnnotationType at = AnnotationType.ENUMERATION;
            ad = new AnnotationDefinition(keyword, at, desc, usage, url);
            definitions.add(ad);
        }

        Header hdr = new Header("", "", "");
        Document ret = new Document(hdr, new StatementGroup());
        ret.setDefinitions(definitions);
        NamespaceGroup nsgroup = new NamespaceGroup();
        nsgroup.setNamespaces(namespaces);
        ret.setNamespaceGroup(nsgroup);
        return ret;
    }

}
