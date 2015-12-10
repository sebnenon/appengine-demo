package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A servlet to handle all the requests on a specific user
 * All requests with path matching "/users/*" where * is the id of the user are handled here.
 */
public class UserServlet extends JsonServlet {

    // A GET request should simply return the user
    @Override
    protected User doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // DONE: Extract the id of the user from the last part of the path of the request
        // DONE: Check if this id is syntactically correct
        // both functionalities are externalized in a method getUserIdFromReq() that returns the id
        long id = getUserIdFromReq(req);

        if (id == 0) {
            throw new ApiException(404, "userNotFound", "User not found");
        }
        // Lookup in repository
        User user = UsersRepository.getUser(id);
        // DONE: not found
        if (user == null) {
            throw new ApiException(404, "userNotFound", "User not found");
        }
        // TODO: Add some mechanism to hide private info about a user (email) except if he is the caller
        // create a publicUser??
        return user;
    }

    // A POST request could be used by a user to edit its own account
    // It could also handle relationship parameters like "followed=true"
    @Override
    protected User doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Get the user as below
        // TODO: Apply some changes on the user (after checking for the connected user)
        // TODO: Handle special parameters like "followed=true" to create or destroy relationships
        // TODO: Return the modified user
        return null;
    }

    // A user can DELETE its own account
    @Override
    protected Void doDelete(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // TODO: Security checks
        // TODO: Delete the user, the messages, the relationships
        // A DELETE request shall not have a response body
        return null;
    }

    // This is a method that gets the id or the "me" in the request and returns the id
    //TODO: refactor to externalize and set generic for the followers/following case
    private long getUserIdFromReq(HttpServletRequest req) throws ApiException {
        // Here we define patterns in order to check the format of the id
        final String ID_PATTERN = "^[0-9]*$";
        final String ME_PATTERN = "^[Mm][Ee]$";
        // Extraction of the parameter in the URI and removal of the initial '/'
        String strId = req.getPathInfo().substring(1);
        // test if long int or "me"
        if (strId.matches(ID_PATTERN)){
            return Long.parseLong(strId);
        }else if (strId.matches(ME_PATTERN)){
            return getAuthenticatedUser(req).id;
        }else{
            return 0;
        }
    }

}