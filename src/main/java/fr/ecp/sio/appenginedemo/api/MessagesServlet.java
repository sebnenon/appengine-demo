package fr.ecp.sio.appenginedemo.api;

import com.googlecode.objectify.Ref;
import fr.ecp.sio.appenginedemo.data.MessagesRepository;
import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.Message;
import fr.ecp.sio.appenginedemo.model.User;
import fr.ecp.sio.appenginedemo.utils.ValidationUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A servlet to handle all the requests on a list of messages
 * All requests on the exact path "/messages" are handled here.
 */
public class MessagesServlet extends JsonServlet {

    // A GET request should return a list of messages

    /**
     * This permits to get the messages of an author, of the followed people etc.
     *
     * @param req a request
     * @return me List of messages
     * @throws ServletException, IOException, ApiException
     */
    @Override
    protected List<Message> doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // Authentification
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) throw new ApiException(500, "accessDenied", "authorization required");
        // DONE: e.g. add a parameter to get the messages of a user given its id (i.e. /messages?author=256439)
        String urlid = req.getParameter("author");
        long authorid = 0;
        if (ValidationUtils.validateId(urlid)) {
            authorid = Long.valueOf(urlid);
        }
        if (authorid != 0 && UsersRepository.isFollowerOf(currentUser.id, authorid)) {
            return MessagesRepository.getMessagesFrom(authorid);
        }
        // Default: get the messages of the followed people
        //TODO: manage cursor
        List<Message> returnlist = null;
        for (User usr : UsersRepository.getFollowers(currentUser.id, 50, null).users) {
            returnlist.addAll(MessagesRepository.getMessagesFrom(usr.id));
        }
        return returnlist;

        // TODO: filter the list based on some parameters (order, limit, scope...)

    }


    /**
     * Permits to post and verify a message
     *
     * @param req a request
     * @return the created message
     * @throws ServletException, IOException, ApiException
     */
    @Override
    protected Message doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // Authentification
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) throw new ApiException(500, "accessDenied", "authorization required");
        // The request should be a JSON object describing a new message
        Message message = getJsonRequestBody(req, Message.class);
        if (message == null) {
            throw new ApiException(400, "invalidRequest", "Invalid JSON body");
        }

        // DONE: validate the message here (minimum length, etc.)
        if (!ValidationUtils.validateMessage(message.text)) {
            throw new ApiException(400, "invalidText", "Text did not match the specs");
        }

        // Some values of the Message should not be sent from the client app
        // Instead, we give them here explicit value
        message.user = Ref.create(currentUser);
        message.date = new Date();
        message.id = null;

        // Our message is now ready to be persisted into our repository
        // After this call, our repository should have given it a non-null id
        MessagesRepository.saveMessage(message);

        return message;
    }

}
