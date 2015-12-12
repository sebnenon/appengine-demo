package fr.ecp.sio.appenginedemo.data;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;
import fr.ecp.sio.appenginedemo.model.Follower;
import fr.ecp.sio.appenginedemo.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a repository class for the users.
 * It could be backed by any kind of persistent storage engine.
 * Here we use the Datastore from Google Cloud Platform, and we access it using the high-level Objectify library.
 */
public class UsersRepository {

    // A static initializer to register the model class with the Objectify service.
    // This is required per Objectify documentation.
    static {
        ObjectifyService.register(User.class);
    }

    public static User getUserByLogin(final String login) {
        // We can add filter of a property if this property has the @Index annotation in the model class
        // first() returns only one result
        return ObjectifyService.ofy()
                .load()
                .type(User.class)
                .filter("login", login)
                .first()
                .now();
    }

    public static User getUserByEmail(final String email) {
        return ObjectifyService.ofy()
                .load()
                .type(User.class)
                .filter("email", email)
                .first()
                .now();
    }

    public static User getUser(long id) {
        return ObjectifyService.ofy()
                .load()
                .type(User.class)
                .id(id)
                .now();
    }

    public static UsersList getUsers() {
        return new UsersList(
                ObjectifyService.ofy()
                        .load()
                        .type(User.class)
                        .list(),
                "dummyCursor"
        );
    }

    public static long allocateNewId() {
        // Sometime we need to allocate an id before persisting, the library allows it
        return new ObjectifyFactory().allocateId(User.class).getId();
    }

    public static void saveUser(User user) {
        user.id = ObjectifyService.ofy()
                .save()
                .entity(user)
                .now()
                .getId();
    }

    public static void deleteUser(long id) {
        ObjectifyService.ofy()
                .delete()
                .type(User.class)
                .id(id)
                .now();
    }

    public static class UsersList {

        public final List<User> users;
        public final String cursor;

        private UsersList(List<User> users, String cursor) {
            this.users = users;
            this.cursor = cursor;
        }

    }

    // Here starts my part =============================================================================================




    /**
     * Get the users following current user, calls getFollow
     * @param id id of the followed user
     * @param limit number of results to return
     * @param cursor position to start from in the results
     * @return a UsersList
     */
    public static UsersList getUserFollowers(long id, int limit, String cursor) {
        return getFollow(id,limit,cursor,"followedId");
    }

    /**
     * Get the users followed by current user, calls getFollow
     * @param id id of the followed user
     * @param limit number of results to return
     * @param cursor position to start from in the results
     * @return a UsersList
     */
    public static UsersList getUserFollowed(long id, int limit, String cursor) {
        return getFollow(id,limit,cursor,"followerId");
    }


    /**
     * This method does the job. It fetches a user list and permits the pagination through
     * limit and cursor parameters
     * @param id id of the followed user
     * @param limit number of results to return
     * @param cursor position to start from in the results
     * @param qFilter the string selecting between followers and followed
     * @return a UsersList
     */
    private static UsersList getFollow(long id, int limit, String cursor, String qFilter) {
        // Initializes the list of users that will be returned
        List<User> results = new ArrayList<>();

        // Query parameterization
        Query<Follower> query = ObjectifyService.ofy().load().type(Follower.class).filter(qFilter, id).limit(limit);

        // If the cursor is set, restart from there
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }

        // this permits to test if we reached the end of the results
        boolean genCursor = false;

        // initializes the iterator on query results
        QueryResultIterator<Follower> iterator = query.iterator();

        // loop on results using iterator
        while (iterator.hasNext()) {
            Follower fol = iterator.next();
            results.add(getUser(fol.id));
            genCursor = true;
        }

        // if we didn't reach the end of the results, generate a new cursor
        String curs = null;
        if (genCursor) {
            curs = iterator.getCursor().toWebSafeString();
        }

        return new UsersList(results,curs);
    }


    /**
     * Permits to follow/unfollow
     * @param followerId id of the follower
     * @param followedId id of the followed
     * @param followed boolean. true: follow, false: unfollow
     */
    public static void setUserFollowed(long followerId, long followedId, boolean followed) {
        // try to get this relationship
        Follower fol = ObjectifyService.ofy()
                .load()
                .type(Follower.class)
                .filter("follower", followerId)
                .filter("followed", followedId)
                .first()
                .now();
        // it doesn't exist and we wan't to create it
        if (followed && fol == null) {
            fol.followedId = followedId;
            fol.followerId = followerId;
            ObjectifyService.ofy()
                    .save()
                    .entity(fol)
                    .now();
        // it exists and we wan't to delete it
        } else if (!followed && fol != null){
            ObjectifyService.ofy()
                    .delete()
                    .entity(fol)
                    .now();
        }

    }

}