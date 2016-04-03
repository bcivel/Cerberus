/*
 * Cerberus  Copyright (C) 2013  vertigo17
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.servlet.crud.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.cerberus.crud.entity.Application;
import org.cerberus.crud.entity.CountryEnvironmentParameters;
import org.cerberus.crud.entity.MessageEvent;
import org.cerberus.crud.factory.IFactoryCountryEnvironmentParameters;
import org.cerberus.enums.MessageEventEnum;
import org.cerberus.exception.CerberusException;
import org.cerberus.crud.service.IApplicationService;
import org.cerberus.crud.service.ICountryEnvironmentParametersService;
import org.cerberus.crud.service.ILogEventService;
import org.cerberus.crud.service.impl.LogEventService;
import org.cerberus.util.ParameterParserUtil;
import org.cerberus.util.StringUtil;
import org.cerberus.util.answer.Answer;
import org.cerberus.util.answer.AnswerItem;
import org.cerberus.util.answer.AnswerUtil;
import org.cerberus.util.servlet.ServletUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author bcivel
 */
@WebServlet(name = "UpdateApplication", urlPatterns = {"/UpdateApplication"})
public class UpdateApplication extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, CerberusException, JSONException {
        JSONObject jsonResponse = new JSONObject();
        ApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        Answer ans = new Answer();
        MessageEvent msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_UNEXPECTED);
        msg.setDescription(msg.getDescription().replace("%DESCRIPTION%", ""));
        ans.setResultMessage(msg);
        PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
        String charset = request.getCharacterEncoding();

        ICountryEnvironmentParametersService ceaService = appContext.getBean(ICountryEnvironmentParametersService.class);

        response.setContentType("application/json");

        // Calling Servlet Transversal Util.
        ServletUtil.servletStart(request);

        /**
         * Parsing and securing all required parameters.
         */
        String application = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("application"), null, charset);
        String system = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("system"), null, charset);
        String subSystem = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("subsystem"), null, charset);
        String type = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("type"), null, charset);
        String mavenGpID = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("mavengroupid"), null, charset);
        String deployType = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("deploytype"), null, charset);
        String svnURL = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("svnurl"), null, charset);
        String bugTrackerURL = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("bugtrackerurl"), null, charset);
        String newBugURL = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("bugtrackernewurl"), null, charset);
        String description = ParameterParserUtil.parseStringParamAndDecode(request.getParameter("description"), null, charset);
        Integer sort = 10;
        boolean sort_error = false;
        try {
            if (request.getParameter("sort") != null && !request.getParameter("sort").equals("")) {
                sort = Integer.valueOf(policy.sanitize(request.getParameter("sort")));
            }
        } catch (Exception ex) {
            sort_error = true;
        }

        // Getting list of application from JSON Call
        JSONArray objApplicationArray = new JSONArray(request.getParameter("environmentList"));
        List<CountryEnvironmentParameters> ceaList = new ArrayList();
        ceaList = getCountryEnvironmentApplicationFromParameter(request, appContext, system, application, objApplicationArray);

        // Prepare the final answer.
        MessageEvent msg1 = new MessageEvent(MessageEventEnum.GENERIC_OK);
        Answer finalAnswer = new Answer(msg1);

        /**
         * Checking all constrains before calling the services.
         */
        if (StringUtil.isNullOrEmpty(application)) {
            msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_EXPECTED);
            msg.setDescription(msg.getDescription().replace("%ITEM%", "Application")
                    .replace("%OPERATION%", "Update")
                    .replace("%REASON%", "Application ID (application) is missing."));
            ans.setResultMessage(msg);
        } else if (sort_error) {
            msg = new MessageEvent(MessageEventEnum.DATA_OPERATION_ERROR_EXPECTED);
            msg.setDescription(msg.getDescription().replace("%ITEM%", "Application")
                    .replace("%OPERATION%", "Update")
                    .replace("%REASON%", "Could not manage to convert sort to an integer value."));
            ans.setResultMessage(msg);
        } else {
            /**
             * All data seems cleans so we can call the services.
             */
            IApplicationService applicationService = appContext.getBean(IApplicationService.class);

            AnswerItem resp = applicationService.readByKey(application);
            if (!(resp.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode()) && resp.getItem()!=null)) {
                /**
                 * Object could not be found. We stop here and report the error.
                 */
                finalAnswer = AnswerUtil.agregateAnswer(finalAnswer, (Answer) resp);

            } else {
                /**
                 * The service was able to perform the query and confirm the
                 * object exist, then we can update it.
                 */
                Application applicationData = (Application) resp.getItem();
                applicationData.setSystem(system);
                applicationData.setSubsystem(subSystem);
                applicationData.setType(type);
                applicationData.setMavengroupid(mavenGpID);
                applicationData.setDeploytype(deployType);
                applicationData.setSvnurl(svnURL);
                applicationData.setBugTrackerUrl(bugTrackerURL);
                applicationData.setBugTrackerNewUrl(newBugURL);
                applicationData.setDescription(description);
                applicationData.setSort(sort);
                ans = applicationService.update(applicationData);
                finalAnswer = AnswerUtil.agregateAnswer(finalAnswer, (Answer) ans);

                if (ans.isCodeEquals(MessageEventEnum.DATA_OPERATION_OK.getCode())) {
                    /**
                     * Update was succesfull. Adding Log entry.
                     */
                    ILogEventService logEventService = appContext.getBean(LogEventService.class);
                    logEventService.createPrivateCalls("/UpdateApplication", "UPDATE", "Updated Application : ['" + application + "']", request);
                }

                // Update the Database with the new list.
                ans = ceaService.compareListAndUpdateInsertDeleteElements(system, application, ceaList);
                finalAnswer = AnswerUtil.agregateAnswer(finalAnswer, (Answer) ans);

            }
        }

        /**
         * Formating and returning the json result.
         */
        jsonResponse.put("messageType", finalAnswer.getResultMessage().getMessage().getCodeString());
        jsonResponse.put("message", finalAnswer.getResultMessage().getDescription());

        response.getWriter().print(jsonResponse);
        response.getWriter().flush();
    }

    private List<CountryEnvironmentParameters> getCountryEnvironmentApplicationFromParameter(HttpServletRequest request, ApplicationContext appContext, String system, String application, JSONArray json) throws JSONException {
        List<CountryEnvironmentParameters> cedList = new ArrayList();
        IFactoryCountryEnvironmentParameters cedFactory = appContext.getBean(IFactoryCountryEnvironmentParameters.class);

        for (int i = 0; i < json.length(); i++) {
            JSONObject tcsaJson = json.getJSONObject(i);

            boolean delete = tcsaJson.getBoolean("toDelete");
            String country = tcsaJson.getString("country");
            String environment = tcsaJson.getString("environment");
            String ip = tcsaJson.getString("ip");
            String domain = tcsaJson.getString("domain");
            String url = tcsaJson.getString("url");
            String urlLogin = tcsaJson.getString("urlLogin");

            if (!delete) {
                CountryEnvironmentParameters ced = cedFactory.create(system, country, environment, application, ip, domain, url, urlLogin);
                cedList.add(ced);
            }
        }
        return cedList;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);

        } catch (CerberusException ex) {
            Logger.getLogger(UpdateApplication.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(UpdateApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);

        } catch (CerberusException ex) {
            Logger.getLogger(UpdateApplication.class
                    .getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(UpdateApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
