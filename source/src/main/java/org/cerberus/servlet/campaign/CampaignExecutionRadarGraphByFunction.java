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
package org.cerberus.servlet.campaign;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.cerberus.dto.TestCaseWithExecution;
import org.cerberus.exception.CerberusException;
import org.cerberus.service.ICampaignService;
import org.cerberus.service.ITestCaseService;
import org.cerberus.util.GraphicHelper;
import org.json.JSONObject;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class CampaignExecutionReport.
 *
 * @author memiks
 */
public class CampaignExecutionRadarGraphByFunction extends HttpServlet {

    private final static Logger LOGGER = Logger.getLogger(CampaignExecutionRadarGraphByFunction.class);
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            ApplicationContext appContext = WebApplicationContextUtils
                    .getWebApplicationContext(this.getServletContext());

            ICampaignService campaignService = appContext
                    .getBean(ICampaignService.class);

            ITestCaseService testCaseService = appContext
                    .getBean(ITestCaseService.class);

            PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);

            String campaignName = policy.sanitize(request
                    .getParameter("CampaignName"));
            String tag = policy.sanitize(request.getParameter("Tag"));
            String[] env = request.getParameterValues("Environment");
            String[] country = request.getParameterValues("Country");
            String[] browser = request.getParameterValues("Browser");

            
            List<TestCaseWithExecution> testCaseWithExecutions = campaignService.getCampaignTestCaseExecutionForEnvCountriesBrowserTag(campaignName, tag, env, country, browser);
            
            HashMap<String, TestCaseWithExecution> testCaseWithExecutionsList = TestCaseWithExecution.generateEmptyResultOfExecutions(testCaseService.findTestCaseByCampaignName(campaignName), env, country, browser);

            for (TestCaseWithExecution testCaseWithExecution : testCaseWithExecutions) {
                String key = testCaseWithExecution.getBrowser() + "_" 
                        + testCaseWithExecution.getCountry() + "_" 
                        + testCaseWithExecution.getEnvironment() + "_" 
                        + testCaseWithExecution.getTest() + "_" 
                        + testCaseWithExecution.getTestCase();
                testCaseWithExecutionsList.put(key, testCaseWithExecution);
            }

            testCaseWithExecutions = new ArrayList<TestCaseWithExecution>(testCaseWithExecutionsList.values());
            
            HashMap<String, List<String>> datas = generateMultiBarAxisFromStatus(testCaseWithExecutions);

            List<JSONObject> axis = new ArrayList<JSONObject>();
            
            axis.add(GraphicHelper.generateAxisForMultiBar("total", 
                    datas.get("total").toArray(new String[1]),"rgba(0,0,255,0.2)","rgba(0,0,255,1)","rgba(0,0,255,0.4)"));

            JSONObject jSONResult = GraphicHelper.generateChart(
                    GraphicHelper.ChartType.Radar, 
                    axis.toArray(new JSONObject[1]), 
                    datas.get("labels").toArray(new String[1]));

            response.setContentType("application/json");
            response.getWriter().print(jSONResult);
        } catch (CerberusException ex) {
            LOGGER.error(ex);

        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed"
    // desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
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
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#getServletInfo()
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private HashMap<String, List<String>> generateMultiBarAxisFromStatus(List<TestCaseWithExecution> testCaseWithExecutions) {
        HashMap<String, List<String>> results = new HashMap<String, List<String>>();

        results.put("total", new ArrayList<String>());
        results.put("labels", new ArrayList<String>());
        
        int index;
        String key;
        for (TestCaseWithExecution testCaseWithExecution : testCaseWithExecutions) {
            if(testCaseWithExecution.getFunction() != null && !"".equals(testCaseWithExecution.getFunction())) {
                key = testCaseWithExecution.getFunction();
            } else {
                key = testCaseWithExecution.getTest();
            }
            
            LOGGER.error("Key=" + key);

            
            if(!results.get("labels").contains(key)) {
                results.get("labels").add(key);

                results.get("total").add("0");
            }
            
            index = results.get("labels").indexOf(key);

            results.get("total").set(index, String.valueOf(Integer.parseInt(results.get("total").get(index)) + 1));
        }

        return results;
    }

}