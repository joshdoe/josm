package org.openstreetmap.josm.io.remotecontrol.handler;

import java.io.File;
import java.util.Arrays;
import org.openstreetmap.josm.actions.OpenFileAction;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import static org.openstreetmap.josm.tools.I18n.tr;

public class OpenFileHandler extends RequestHandler {

    public static final String command = "open_file";

    @Override
    public String[] getMandatoryParams() {
        return new String[]{"filename"};
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.OPEN_FILES;
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        OpenFileAction.openFiles(Arrays.asList(new File(args.get("filename"))));
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to open a local file.");
    }
}
