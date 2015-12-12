package fr.ecp.sio.appenginedemo.api;

import fr.ecp.sio.appenginedemo.data.UsersRepository;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * Created by snenon on 12/12/15.
 */
public class FollowServlet extends JsonServlet {

    private static final int LIST_LIMIT = 50;

    /**
     * This method returns the list of followers/followed users, depending on the arguments of the URI
     * @param req: the request object
     * @return a List<User>
     * @throws ServletException, IOException, ApiException
     */
    @Override
    protected List<User> doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        String followers = req.getParameter("followedBy");
        String followed = req.getParameter("followerOf");
        String cursor = req.getParameter("continuationCursor");
        final String ID_PATTERN = "^[0-9]*$";
        if(followers != null && followers.matches(ID_PATTERN)){
            return UsersRepository.getUserFollowers(Long.parseLong(followers),LIST_LIMIT,cursor).users;
        }
        if(followed != null && followed.matches(ID_PATTERN)){
            return UsersRepository.getUserFollowed(Long.parseLong(followed),LIST_LIMIT,cursor).users;
        }
        return null;
    }

    /**
     * permits to follow/unfollow
     * @param req the request object
     * @return the followed user or the current user
     * @throws ServletException, IOException, ApiException
     */
    // TODO:SN: authentification and unexisting user
    @Override
    protected User doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        Long currentUser = getAuthenticatedUser(req).id;
        String follow = req.getParameter("follow");
        final String T_PATTERN = "^[tT]rue$";
        final String F_PATTERN = "^[fF]alse$";
        Long followed =  UserServlet.getUserIdFromReq(req);

        if (follow.matches(T_PATTERN)){
            UsersRepository.setUserFollowed(currentUser,followed,true);
            return UsersRepository.getUser(followed);
        }
        if (follow.matches(F_PATTERN)){
            UsersRepository.setUserFollowed(currentUser,followed,false);
            return UsersRepository.getUser(currentUser);
        }


        return null;
    }
}
