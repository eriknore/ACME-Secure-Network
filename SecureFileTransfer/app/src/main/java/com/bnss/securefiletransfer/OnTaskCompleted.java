package com.bnss.securefiletransfer;

import java.util.ArrayList;

/**
 * Created by luceat on 3/3/15.
 */
public interface OnTaskCompleted{
    void onGetUsersCompleted(ArrayList<User> u);

    void onGetFilesCompleted();

    void onSendFileCompleted(String message);

    void onGetFileCompleted(String fileName);
}
