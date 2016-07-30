package com.coderstory.miui_toolkit.tools.Root;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import com.coderstory.miui_toolkit.R;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public abstract class SuHelper {

    /**
     * 执行所提交的命令组
     * @return
     */
    public final boolean execute() {
        boolean retval = false;
        try {
            ArrayList<String> commands = getCommandsToExecute();
            if (null != commands && commands.size() > 0) {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                for (String currCommand : commands) {
                    os.writeBytes(currCommand + "\n");
                    os.flush();
                }
                os.writeBytes("exit\n");
                os.flush();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                int read;
                char[] buffer = new char[4096];
                StringBuilder output = new StringBuilder();
                while ((read = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, read);
                }
                reader.close();
                try {
                    int suProcessRetval = process.waitFor();
                    retval = 255 != suProcessRetval;
                    Log.d("gg", "execute: "+output.toString());
                } catch (Exception ex) {
                    //Log.e("Error executing root action", ex);
                }
            }
        } catch (IOException ex) {
            Log.w("ROOT", "Can't get root access", ex);
        }  catch (Exception ex) {
            Log.w("ROOT", "Error executing internal operation", ex);
        }
        return retval;
    }
    protected abstract ArrayList<String> getCommandsToExecute() throws UnsupportedEncodingException;


    /**
     * 弹窗确认后执行root命令
     * @param commandText
     * @param messageText
     * @param mContext
     */
    public static void showTips(final String commandText, String messageText, final Context mContext) {

        SharedPreferences prefs = mContext.getSharedPreferences("UserSettings", Context.MODE_WORLD_READABLE);
       final    SharedPreferences.Editor  editor = prefs.edit();
        AlertDialog builder = new AlertDialog.Builder(mContext)
                .setTitle( R.string.Tips_Title)
                .setMessage(messageText)
                .setPositiveButton(R.string.Btn_Sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            List<String> strList = new ArrayList();
                            Process process =    Runtime.getRuntime().exec(new String[]{"su", "-c", commandText});
                            InputStream is = process.getInputStream();
                            InputStreamReader ir = new InputStreamReader(process
                                    .getInputStream());
                            LineNumberReader input = new LineNumberReader(ir);
                            String line;
                            process.waitFor();
                            while ((line = input.readLine()) != null){
                                System.out.println(line);
                                strList.add(line);
                            }
                            Log.d("aaaa", "onClick: "+strList.toString());

                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.Btn_Cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        builder.show();
    }

    /**
     * 判断是否已经被授权root
     * @return
     */
    public static boolean canRunRootCommands() {
        boolean retval = false;
        Process suProcess;

        try {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            if (null != os && null != osRes) {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = osRes.readLine();
                boolean exitSu = false;
                if (null == currUid) {
                    retval = false;
                    exitSu = false;
                    Log.d("ROOT", "Can't get root access or denied by user");
                } else if (true == currUid.contains("uid=0")) {
                    retval = true;
                    exitSu = true;
                    Log.d("ROOT", "Root access granted");
                } else {
                    retval = false;
                    exitSu = true;
                    Log.d("ROOT", "Root access rejected: " + currUid);
                }

                if (exitSu) {
                    os.writeBytes("exit\n");
                    os.flush();
                }
            }
        } catch (Exception e) {
            // Can't get root !
            // Probably broken pipe exception on trying to write to output
            // stream after su failed, meaning that the device is not rooted
            retval = false;
            Log.d("ROOT",
                    "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
        }

        return retval;
    }
}