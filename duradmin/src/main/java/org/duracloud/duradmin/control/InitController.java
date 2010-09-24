/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duradmin.control;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.duracloud.appconfig.xml.DuradminInitDocumentBinding.createDuradminConfigFrom;
import static org.duracloud.common.util.ExceptionUtil.getStackTraceAsString;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.duracloud.common.error.DuraCloudRuntimeException;
import org.duracloud.duradmin.config.DuradminConfig;
import org.duracloud.duradmin.contentstore.ContentStoreProvider;
import org.duracloud.duradmin.domain.AdminInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * This class initializes the application based on the xml body of the
 * servlet request.
 *
 * @author: Andrew Woods
 * Date: Apr 30, 2010
 */
public class InitController extends BaseCommandController {

    private final Logger log = LoggerFactory.getLogger(InitController.class);

    public InitController() {
    	setCommandClass(AdminInit.class);
    	setCommandName("adminInit");
    }
    
    protected ModelAndView handle(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Object o,
                                  BindException be) throws Exception {
        String method = request.getMethod();
        if (!method.equalsIgnoreCase("POST")) {
            return respond(response, "unsupported: " + method, SC_METHOD_NOT_ALLOWED);
        }

        ServletInputStream xml = request.getInputStream();
        if (xml != null) {
            try {
                updateInit(createDuradminConfigFrom(xml));
                return respond(response, "Initialization Successful\n", SC_OK);

            } catch (Exception e) {
                return respond(response,
                        getStackTraceAsString(e),
                        SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            return respond(response, "no duradminConfig in request\n", SC_BAD_REQUEST);
        }
    }

    private void updateInit(org.duracloud.appconfig.domain.DuradminConfig config)
        throws Exception {
        AdminInit init = new AdminInit();
        init.setDuraServiceHost(config.getDuraserviceHost());
        init.setDuraServicePort(config.getDurastorePort());
        init.setDuraServiceContext(config.getDuraserviceContext());
        init.setDuraStoreHost(config.getDurastoreHost());
        init.setDuraStorePort(config.getDurastorePort());
        init.setDuraStoreContext(config.getDurastoreContext());

        DuradminConfig.setConfig(init);

        ContentStoreProvider contentStoreProvider = getContentStoreProvider();
        contentStoreProvider.reinitializeContentStoreManager();
    }

    private ModelAndView respond(HttpServletResponse response, String msg, int status) {
        response.setStatus(status);
        log.info("writing response: status = " + status + "; msg = " + msg);
        return new ModelAndView("jsonView", "response", msg);
    }
}
