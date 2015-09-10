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
package org.cerberus.serviceEngine.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.cerberus.entity.MessageEvent;
import org.cerberus.enums.MessageEventEnum;
import org.cerberus.entity.Session;
import org.cerberus.serviceEngine.ISikuliService;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 *
 * @author bcivel
 */
@Service
public class SikuliService implements ISikuliService {

    private JSONObject generatePostParameters(String action, String locator, String text) throws JSONException, IOException, MalformedURLException {
        JSONObject result = new JSONObject();
        String picture = "";
        URL url = new URL(locator);
        InputStream istream = url.openStream();
        byte[] bytes = IOUtils.toByteArray(istream);
        picture = Base64.encodeBase64URLSafeString(bytes);
        result.put("action", action);
        result.put("picture", picture);
        result.put("text", text);
        return result;
    }

    @Override
    public MessageEvent doSikuliAction(Session session, String action, String locator, String text) {
        URL url;
        try {
            String urlToConnect = "http://" + session.getHost() + ":" + session.getPort() + "/extra/ExecuteSikuliAction";
            /**
             * Connect to ExecuteSikuliAction Servlet Through SeleniumServer
             */
            url = new URL(urlToConnect);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

            JSONObject postParameters = generatePostParameters(action, locator, text);
            connection.setDoOutput(true);

            // Send post request
            PrintStream os = new PrintStream(connection.getOutputStream());
            os.println(postParameters.toString());
            os.println("|ENDS|");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return new MessageEvent(MessageEventEnum.ACTION_FAILED_SIKULI_SERVER_NOT_REACHABLE);
            }

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while (!(inputLine = in.readLine()).equals("|ENDR|")) {
                response.append(inputLine);
            }
            in.close();
            os.close();

        } catch (MalformedURLException ex) {
            Logger.getLogger(SikuliService.class.getName()).log(Level.FATAL, ex);
            return new MessageEvent(MessageEventEnum.ACTION_FAILED_SIKULI_SERVER_NOT_REACHABLE);
        } catch (IOException ex) {
            Logger.getLogger(SikuliService.class.getName()).log(Level.FATAL, ex);
            return new MessageEvent(MessageEventEnum.ACTION_FAILED);
        } catch (JSONException ex) {
            Logger.getLogger(SikuliService.class.getName()).log(Level.FATAL, ex);
            return new MessageEvent(MessageEventEnum.ACTION_FAILED);
        }
        return getResultMessage(action, locator, text);
    }

    private MessageEvent getResultMessage(String action, String locator, String text) {
        MessageEvent message = null;
        if (action.equals("click")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_CLICK);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
        } else if (action.equals("rightClick")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_RIGHTCLICK);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
        } else if (action.equals("doubleClick")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_DOUBLECLICK);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
        } else if (action.equals("type")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_TYPE);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
            message.setDescription(message.getDescription().replaceAll("%DATA%", text));
        } else if (action.equals("mouseOver")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_MOUSEOVER);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
        } else if (action.equals("keyPress")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_KEYPRESS);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
        } else if (action.equals("wait")) {
            message = new MessageEvent(MessageEventEnum.ACTION_SUCCESS_WAIT_ELEMENT);
            message.setDescription(message.getDescription().replaceAll("%ELEMENT%", locator));
        } else if (action.equals("verifyElementPresent")) {
            message = new MessageEvent(MessageEventEnum.CONTROL_SUCCESS_PRESENT);
            message.setDescription(message.getDescription().replaceAll("%STRING1%", locator));
        }
        return message;

    }

}