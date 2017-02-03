package fr.gaulupeau.apps.Poche.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;

import fr.gaulupeau.apps.Poche.data.Settings;
import fr.gaulupeau.apps.Poche.data.dao.entities.QueueItem;
import fr.gaulupeau.apps.Poche.network.Updater;

public class ServiceHelper {

    private static final String TAG = ServiceHelper.class.getSimpleName();

    public static void addLink(Context context, String link) {
        addLink(context, link, null);
    }

    private static void addLink(Context context, String link, Long operationID) {
        Log.d(TAG, "addLink() started");

        ActionRequest request = new ActionRequest(ActionRequest.Action.ADD_LINK);
        request.setExtra(link);
        request.setOperationID(operationID);

        startService(context, request);
    }

    public static void archiveArticle(Context context, int articleID) {
        changeArticle(context, articleID, QueueItem.ArticleChangeType.ARCHIVE);
    }

    public static void favoriteArticle(Context context, int articleID) {
        changeArticle(context, articleID, QueueItem.ArticleChangeType.FAVORITE);
    }

    public static void changeArticleTitle(Context context, int articleID) {
        changeArticle(context, articleID, QueueItem.ArticleChangeType.TITLE);
    }

    public static void changeArticleTags(Context context, int articleID) {
        changeArticle(context, articleID, QueueItem.ArticleChangeType.TAGS);
    }

    public static void deleteTagsFromArticle(Context context, int articleID,
                                             Collection<String> tags) {
        Log.d(TAG, "deleteTagsFromArticle() started");

        ActionRequest request = new ActionRequest(ActionRequest.Action.ARTICLE_TAGS_DELETE);
        request.setArticleID(articleID);
        request.setExtra(TextUtils.join(QueueItem.DELETED_TAGS_DELIMITER, tags));

        startService(context, request);
    }

    public static void deleteArticle(Context context, int articleID) {
        Log.d(TAG, "deleteArticle() started");

        ActionRequest request = new ActionRequest(ActionRequest.Action.ARTICLE_DELETE);
        request.setArticleID(articleID);

        startService(context, request);
    }

    public static void syncAndUpdate(Context context, Updater.UpdateType updateType,
                                     boolean auto, Settings settings) {
        syncAndUpdate(context, updateType, auto, settings, null);
    }

    private static void syncAndUpdate(Context context, Updater.UpdateType updateType,
                                      boolean auto, Settings settings, Long operationID) {
        Log.d(TAG, "syncAndUpdate(conditional) started");

        if(settings != null && settings.isOfflineQueuePending()) {
            syncAndUpdate(context, updateType, auto, operationID);
        } else {
            updateArticles(context, updateType, auto, operationID);
        }
    }

    private static void syncAndUpdate(Context context, Updater.UpdateType updateType,
                                      boolean auto, Long operationID) {
        Log.d(TAG, "syncAndUpdate() started");

        ActionRequest syncRequest = getSyncQueueRequest(auto, false);
        syncRequest.setNextRequest(getUpdateArticlesRequest(updateType, auto, operationID));

        startService(context, syncRequest);
    }

    public static void syncQueue(Context context) {
        syncQueue(context, false, false);
    }

    public static void syncQueue(Context context, boolean auto) {
        syncQueue(context, auto, false);
    }

    public static void syncQueue(Context context, boolean auto, boolean byOperation) {
        Log.d(TAG, "syncQueue() started");

        startService(context, getSyncQueueRequest(auto, byOperation));
    }

    private static ActionRequest getSyncQueueRequest(boolean auto, boolean byOperation) {
        ActionRequest request = new ActionRequest(ActionRequest.Action.SYNC_QUEUE);
        if(auto) request.setRequestType(ActionRequest.RequestType.AUTO);
        else if(byOperation) request.setRequestType(ActionRequest.RequestType.MANUAL_BY_OPERATION);

        return request;
    }

    public static void updateArticles(Context context,
                                      Updater.UpdateType updateType,
                                      boolean auto, Long operationID) {
        Log.d(TAG, "updateArticles() started");

        startService(context, getUpdateArticlesRequest(updateType, auto, operationID));
    }

    private static ActionRequest getUpdateArticlesRequest(Updater.UpdateType updateType,
                                                          boolean auto, Long operationID) {
        ActionRequest request = new ActionRequest(ActionRequest.Action.UPDATE_ARTICLES);
        request.setUpdateType(updateType);
        request.setOperationID(operationID);
        if(auto) request.setRequestType(ActionRequest.RequestType.AUTO);

        return request;
    }

    public static void downloadArticleAsPDF(Context context, int articleID, Long operationID) {
        downloadArticleAsFile(context, articleID, ActionRequest.DownloadFormat.PDF, operationID);
    }

    private static void downloadArticleAsFile(Context context, int articleID,
                                             ActionRequest.DownloadFormat downloadFormat,
                                             Long operationID) {
        Log.d(TAG, "downloadArticleAsFile() started");

        ActionRequest request = new ActionRequest(ActionRequest.Action.DOWNLOAD_AS_FILE);
        request.setArticleID(articleID);
        request.setDownloadFormat(downloadFormat);
        request.setOperationID(operationID);

        startService(context, request);
    }

    public static void fetchImages(Context context) {
        Log.d(TAG, "fetchImages() started");

        startService(context, new ActionRequest(ActionRequest.Action.FETCH_IMAGES));
    }

    private static void changeArticle(Context context, int articleID,
                                      QueueItem.ArticleChangeType articleChangeType) {
        Log.d(TAG, "changeArticle() started; articleChangeType: " + articleChangeType);

        ActionRequest request = new ActionRequest(ActionRequest.Action.ARTICLE_CHANGE);
        request.setArticleID(articleID);
        request.setArticleChangeType(articleChangeType);

        startService(context, request);
    }

    public static void startService(Context context, ActionRequest request) {
        switch(request.getAction()) {
            case ADD_LINK:
            case ARTICLE_CHANGE:
            case ARTICLE_TAGS_DELETE:
            case ARTICLE_DELETE:
            case SYNC_QUEUE:
            case UPDATE_ARTICLES:
                startService(context, request, true);
                break;

            case FETCH_IMAGES:
            case DOWNLOAD_AS_FILE:
                startService(context, request, false);
                break;

            default:
                throw new IllegalStateException("Action is not implemented: " + request.getAction());
        }
    }

    private static void startService(Context context, ActionRequest request, boolean mainService) {
        Intent intent = new Intent(context, mainService ? MainService.class : SecondaryService.class);
        intent.putExtra(ActionRequest.ACTION_REQUEST, request);

        context.startService(intent);
    }

}
