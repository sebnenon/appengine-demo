package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A servlet to handle all the requests on a specific message
 * All requests with path matching "/messages/*" where * is the id of the message are handled here.
 */
public class MessageServlet extends JsonServlet {

    /**
     * Verifies and cast to long the id obtained from the request url
     *
     * @param req a request
     * @return long id: the id of the message
     * @throws ApiException
     */
    protected static long getMessageIdFromReq(HttpServletRequest req) throws ApiException {
        // Here we define patterns in order to check the format of the id
        final String ID_PATTERN = "^[0-9]*$";
        // Extraction of the parameter in the URI and removal of the initial '/'
        String strId = req.getPathInfo().substring(1);
        // test if long int
        if (strId.matches(ID_PATTERN)) {
            return Long.parseLong(strId);
        } else {
            return 0;
        }
    }

    /**
     * Returns a message if the user is allowed to get it
     *
     * @param req request
     * @return a message
     * @throws ServletException, IOException, ApiException
     */
    @Override
    protected Message doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // DONE: Extract the id of the message from the last part of the path of the request
        // DONE: Check if this id is syntactically correct
        long id = getMessageIdFromReq(req);
        User currentUser = getAuthenticatedUser(req);
        // Lookup in repository
        Message message = MessagesRepository.getMessage(id);
        // Verify if message exists and user is allowed to see it
        if (message == null) {
            throw new ApiException(404, "messageNotFound", "message not found");
        } else if (currentUser != null && UsersRepository.isFollowerOf(currentUser.id, message.user.get().id)) {
            return message;
        } else {
            throw new ApiException(500, "accessDenied", "not allowed too see this message");
        }
    }

    /**
     * Permits to edit a message
     *
     * @param req a request
     * @return the edited message
     * @throws ServletException
     * @throws IOException
     * @throws ApiException
     */
    @Override
    protected Message doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // Authentification
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) throw new ApiException(500, "accessDenied", "authorization required");
        // DONE: Get the message as below
        long id = getMessageIdFromReq(req);
        Message message = MessagesRepository.getMessage(id);
        // DONE: verify if user is the author
        if (currentUser.id != message.user.get().id)
            throw new ApiException(500, "accessDenied", "not allowed to midify this message");
        Message messageData = getJsonRequestBody(req, Message.class);
        if (messageData != null) {
            if (messageData.text != null) {
                //the text only is modified, the date is kept the same
                message.text = messageData.text;
                MessagesRepository.saveMessage(message);
                // DONE: Return the modified message
                return message;
            }
        }
        return null;
    }

    /**
     * Deletes a message if current user is the author
     *
     * @param req a request
     * @return null
     * @throws ServletException
     * @throws IOException
     * @throws ApiException
     */
    @Override
    protected Void doDelete(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // Authentification
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) throw new ApiException(500, "accessDenied", "authorization required");
        Message messageToDelete = MessagesRepository.getMessage(getMessageIdFromReq(req));
        if (currentUser.id != messageToDelete.id) throw new ApiException(500, "accessDenied", "authorization required");

        // deletion
        MessagesRepository.deleteMessage(messageToDelete.id);
        return null;
    }

}
