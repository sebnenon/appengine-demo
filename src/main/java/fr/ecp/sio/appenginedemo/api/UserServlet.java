package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.User;
import fr.ecp.sio.appenginedemo.utils.ValidationUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A servlet to handle all the requests on a specific user
 * All requests with path matching "/users/*" where * is the id of the user are handled here.
 */
public class UserServlet extends JsonServlet {

    /**
     * Gets the id or the "me" in the request URL and returns the id
     *
     * @param req, the request
     * @return a long id or 0 if the user doesn't exist
     * @throws ApiException
     */
    protected static long getUserIdFromReq(HttpServletRequest req) throws ApiException {
        // Here we define patterns in order to check the format of the id
        final String ID_PATTERN = "^[0-9]*$";
        final String ME_PATTERN = "^[Mm][Ee]$";
        // Extraction of the parameter in the URI and removal of the initial '/'
        String strId = req.getPathInfo().substring(1);
        // test if long int or "me"
        if (strId.matches(ID_PATTERN)) {
            return Long.parseLong(strId);
        } else if (strId.matches(ME_PATTERN)) {
            if (getAuthenticatedUser(req) != null) {
                return getAuthenticatedUser(req).id;
            } else {
                throw new ApiException(500, "accessDenied", "invalid token");
            }
        } else {
            return 0;
        }
    }

    /**
     * Returns the user specified by the id in the URL (with sensitive data obfuscated), else, returns the current user.
     *
     * @param req: HTTP request
     * @return a User
     * @throws ServletException, IOException, ApiException
     */
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
        // DONE: Add some mechanism to hide private info about a user (email) except if he is the caller
        User currentUser = getAuthenticatedUser(req);
        if (currentUser != null && currentUser.id != user.id) {
            return UsersRepository.obfuscatedUser(user);
        }
        return user;
    }

    /**
     * Permits to follow/unfollow if current user is connected, using the boolean parameter "follow"
     * If "follow" is not specified, the method looks for a JSON containing User attributes to edit
     * user account
     * @param req the request object
     * @return the followed user or the current user
     * @throws ServletException, IOException, ApiException
     */
    @Override
    protected User doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // Authentification
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) throw new ApiException(500, "accessDenied", "authorization required");
        // DONE: Apply some changes on the user (after checking for the connected user)
        // DONE: Return the modified user
        if (currentUser.id == 0) {
            throw new ApiException(404, "userNotFound", "User not found");
        }
        // first step: manage followings if follow is defined
        String follow = req.getParameter("followed");
        if (follow != null) {
            // true = follow, false = unfollow
            final String T_PATTERN = "^[tT]rue$";
            final String F_PATTERN = "^[fF]alse$";
            User followedUser = UsersRepository.getUser(UserServlet.getUserIdFromReq(req));
            if (followedUser == null || followedUser.id == 0) {
                throw new ApiException(404, "userNotFound", "User to follow not found");
            }
            if (follow.matches(T_PATTERN)) {
                UsersRepository.setFollowRelationship(currentUser.id, followedUser.id, true);
                return followedUser;
            }
            if (follow.matches(F_PATTERN)) {
                UsersRepository.setFollowRelationship(currentUser.id, followedUser.id, false);
                return currentUser;
            }
            // second step: edit user account using a JSON. It is possible to specify only one parameter to edit
        } else {
            // First: collect the data from the JSON
            User userData = getJsonRequestBody(req, User.class);
            if (userData == null) {
                throw new ApiException(400, "invalidRequest", "Invalid JSON body");
            }
            // Performs the same checkings as for a user creation
            // The modification of the mail and password only is allowed
            if (userData.password == null || !ValidationUtils.validatePassword(userData.password)) {
                throw new ApiException(400, "invalidPassword", "Password did not match the specs");
            } else {
                currentUser.password = userData.password;
            }
            if (!ValidationUtils.validateEmail(userData.email)) {
                throw new ApiException(400, "invalidEmail", "Invalid email");
            } else {
                if (UsersRepository.getUserByEmail(userData.email) != null) {
                    throw new ApiException(400, "duplicateEmail", "Duplicate email");
                }
                currentUser.email = userData.email;
            }
            UsersRepository.saveUser(currentUser);
            return currentUser;
        }
        return null;
    }

    /**
     * A user can DELETE its own account. This method call UserRepository.deleteUser() which
     * also deletes relationships (following/follower)
     *
     * @param req: the request, including URL and parameters
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
        User userToDelete = UsersRepository.getUser(UserServlet.getUserIdFromReq(req));
        if (currentUser.id != userToDelete.id) throw new ApiException(500, "accessDenied", "authorization required");

        // deletion
        UsersRepository.deleteUser(userToDelete.id);
        return null;
    }

}