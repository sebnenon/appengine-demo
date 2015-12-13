package fr.ecp.sio.appenginedemo.api;

/**
 * Created by snenon on 13/12/15.
 */


import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.ServingUrlOptions;
import fr.ecp.sio.appenginedemo.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static fr.ecp.sio.appenginedemo.data.UsersRepository.saveUser;


public class ImageServlet extends JsonServlet {


    @Override
    protected String doGet(HttpServletRequest req) throws ServletException, IOException, ApiException {
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        return blobstoreService.createUploadUrl("/image");
    }

    /**
     * This method gets the picture sent to the servlet, stores it in the blobstore and updates the user
     * @param req
     * @return the picture URL
     * @throws ServletException, IOException, ApiException
     */
    @Override
    protected String doPost(HttpServletRequest req) throws ServletException, IOException, ApiException {
        // Initializes the Blobstore and Image services
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();

        ImagesService imagesService = ImagesServiceFactory.getImagesService();

        // TODO: copy athentification wherever needed and add comparison to URL-id if given
        // Authentification
        User currentUser = getAuthenticatedUser(req);
        if (currentUser == null) throw new ApiException(500, "accessDenied", "authorization required");



        // get uploaded picture
        // TODO: redim and convert to jpeg
        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        List<BlobKey> blobKeys = blobs.get("uploadedFile");
        String uploadedFileKey = blobKeys.get(0).getKeyString();

        // get the serving url of the picture
        String urlImage = imagesService.getServingUrl(ServingUrlOptions.Builder.withBlobKey(blobKeys.get(0)));

        // update user
        currentUser.blobkey = uploadedFileKey;
        currentUser.avatar = urlImage;
        saveUser(currentUser);

        // return picture url
        return urlImage;
    }
}