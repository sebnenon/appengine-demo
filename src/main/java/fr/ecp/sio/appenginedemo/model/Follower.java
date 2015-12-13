package fr.ecp.sio.appenginedemo.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * This class stores the followers such that the latter are not stored in the User class
 * the members are self explaining
 */
@Entity
public class Follower {
    @Id
    public long id;

    @Index
    public long followerId;

    @Index
    public long followedId;
}
