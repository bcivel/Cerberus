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
package org.cerberus.servlet.user;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Level;
import org.cerberus.entity.Group;
import org.cerberus.entity.User;
import org.cerberus.entity.UserSystem;
import org.cerberus.exception.CerberusException;
import org.cerberus.log.MyLogger;
import org.cerberus.service.IUserGroupService;
import org.cerberus.service.IUserService;
import org.cerberus.service.IUserSystemService;
import org.cerberus.service.impl.UserGroupService;
import org.cerberus.service.impl.UserService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author bcivel
 */
@WebServlet(name = "ReadUser", urlPatterns = {"/ReadUser"})
public class ReadUser extends HttpServlet {

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
            throws ServletException, IOException {
        ApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        IUserService userService = appContext.getBean(UserService.class);
        IUserSystemService userSystemService = appContext.getBean(IUserSystemService.class);
        IUserGroupService userGroupService = appContext.getBean(UserGroupService.class);

        JSONObject data = new JSONObject();

        try {

            String user = request.getUserPrincipal().getName();

            User myUser = userService.findUserByKey(user);
            data.put("login", myUser.getLogin());
            data.put("name", myUser.getName());
            data.put("team", myUser.getTeam());
            data.put("defaultSystem", myUser.getDefaultSystem());
            data.put("request", myUser.getRequest());
            data.put("email", myUser.getEmail());
            data.put("language", myUser.getLanguage());

            JSONArray groups = new JSONArray();
            for (Group group : userGroupService.findGroupByKey(myUser.getLogin())) {
                groups.put(group.getGroup());
            }
            data.put("group", groups);

            JSONArray systems = new JSONArray();
            for (UserSystem sys : userSystemService.findUserSystemByUser(myUser.getLogin())) {
                systems.put(sys.getSystem());
            }
            data.put("system", systems);
            HttpSession session = request.getSession();
            System.out.print(session.getId());
            session.setAttribute("MySystem", myUser.getDefaultSystem());
            session.setAttribute("MyLang", myUser.getLanguage());

        } catch (CerberusException ex) {
            response.setContentType("text/html");
            response.getWriter().print(ex.getMessageError().getDescription());
        } catch (JSONException ex) {
            Logger.getLogger(ReadUser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (NullPointerException ex) {
            response.sendRedirect("./Login.jsp");
        }
        response.setContentType("application/json");
        response.getWriter().print(data.toString());

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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
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
