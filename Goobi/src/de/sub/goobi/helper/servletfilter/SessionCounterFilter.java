package de.sub.goobi.helper.servletfilter;

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
//TODO: Parts of this class seem to be copied from the Internet, what's the licence?

import java.io.IOException;

import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.el.ValueBinding;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import de.sub.goobi.forms.SessionForm;
import de.sub.goobi.helper.FacesContextHelper;

@SuppressWarnings("deprecation")
public class SessionCounterFilter implements Filter {
    ServletContext servletContext;

    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        FacesContext context = getFacesContext(request, response);
        try {
            Application application = context.getApplication();
            if (application != null) {
                ValueBinding binding = application.createValueBinding("#{SessionForm}");
                if (binding != null && context != null) {

                    SessionForm sf = (SessionForm) binding.getValue(context);
                    sf.sessionAktualisieren(httpReq.getSession());
                }
            }
        } catch (Exception e) {
            
        }
        chain.doFilter(request, response);
    }

    // You need an inner class to be able to call FacesContext.setCurrentInstance
    // since it's a protected method
    private abstract static class InnerFacesContext extends FacesContext {
        protected static void setFacesContextAsCurrentInstance(FacesContext facesContext) {
            FacesContext.setCurrentInstance(facesContext);
        }
    }

    private FacesContext getFacesContext(ServletRequest request, ServletResponse response) {
        // Try to get it first
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (facesContext != null)
            return facesContext;

        FacesContextFactory contextFactory = (FacesContextFactory) FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        LifecycleFactory lifecycleFactory = (LifecycleFactory) FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        Lifecycle lifecycle = lifecycleFactory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);

        // Either set a private member servletContext = filterConfig.getServletContext();
        // in you filter init() method or set it here like this:
        // ServletContext servletContext = ((HttpServletRequest) request).getSession().getServletContext();
        // Note that the above line would fail if you are using any other protocol than http

        // Doesn't set this instance as the current instance of FacesContext.getCurrentInstance
        facesContext = contextFactory.getFacesContext(servletContext, request, response, lifecycle);

        // Set using our inner class
        InnerFacesContext.setFacesContextAsCurrentInstance(facesContext);

        // set a new viewRoot, otherwise context.getViewRoot returns null
        UIViewRoot view = facesContext.getApplication().getViewHandler().createView(facesContext, "yourOwnID");
        facesContext.setViewRoot(view);

        return facesContext;
    }

    /**
    */
    public void destroy() {
        // Nothing necessary
    }

}
