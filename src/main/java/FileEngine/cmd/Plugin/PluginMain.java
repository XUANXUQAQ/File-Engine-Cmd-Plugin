package FileEngine.cmd.Plugin;

import FileEngine.cmd.Plugin.ThreadPool.CachedThreadPool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.*;

import FileEngine.cmd.Plugin.checkupdate.UpdateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PluginMain extends Plugin {
    private volatile long startTime;
    private volatile boolean timer = false;
    private volatile boolean isNotExit = true;
    private volatile String command;
    private static final Pattern colon = Pattern.compile(":");
    private final String configFile = "plugins/Plugin configuration files/Cmd/settings.json";
    private final String tmpDir = "plugins/Plugin configuration files/Cmd/tmp";
    private Color backgroundColor;
    private Color labelColor;

    private void initSettings() {
        initFile();
        StringBuilder strBuilder = new StringBuilder();
        String eachLine;
         try (BufferedReader buffR = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
             while ((eachLine = buffR.readLine()) != null) {
                 strBuilder.append(eachLine);
             }
             JSONObject json = JSONObject.parseObject(strBuilder.toString());
             backgroundColor = new Color(json.getInteger("backgroundColor"));
             labelColor = new Color(json.getInteger("labelColor"));
         } catch (Exception e) {
             e.printStackTrace();
         } finally {
             if (backgroundColor == null) {
                 backgroundColor = new Color(0xffffff);
             }
             if (labelColor == null) {
                 labelColor = new Color(0xff9868);
             }
             //保存配置文件
             saveConfigs();
         }
    }

    private void generateBatFile(String command, String filePath) {
        try (BufferedWriter buffW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            buffW.write(command);
            buffW.newLine();
            buffW.write("pause");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initFile() {
        try {
            File settings = new File(configFile);
            File parent = settings.getParentFile();
            File tmp = new File(tmpDir);
            if (!parent.exists()) {
                parent.mkdirs();
            }
            if (!tmp.exists()) {
                tmp.mkdirs();
            }
            if (!settings.exists()) {
                settings.createNewFile();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfigs() {
        try (BufferedWriter buffW = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            JSONObject json = new JSONObject();
            json.put("backgroundColor", backgroundColor.getRGB());
            json.put("labelColor", labelColor.getRGB());
            String format = JSON.toJSONString(json, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
            buffW.write(format);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() {
        startTime = System.currentTimeMillis();
        timer = true;
    }

    private void reset() {
        startTime = System.currentTimeMillis();
        timer = false;
    }

    /**
     * Do Not Remove, this is used for File-Engine to get message from the plugin.
     * You can show message using "displayMessage(String caption, String message)"
     * @return String[2], the first string is caption, the second string is message.
     * @see #displayMessage(String, String)
     */
    public String[] getMessage() {
        return _getMessage();
    }

    /**
     * Do Not Remove, this is used for File-Engine to get results from the plugin
     * You can add result using "addToResultQueue(String result)".
     * @see #addToResultQueue(String)
     * @return result
     */
    public String pollFromResultQueue() {
        return _pollFromResultQueue();
    }

    /**
     * Do Not Remove, this is used for File-Engine to check the API version.
     * @return Api version
     */
    public int getApiVersion() {
        return _getApiVersion();
    }

    /**
     * When the search bar textChanged, this function will be called.
     * @param text
     * Example : When you input "&gt;examplePlugin TEST" to the search bar, the param will be "TEST"
     */
    @Override
    public void textChanged(String text) {
        if (!text.isEmpty()) {
            command = text;
            start();
        } else {
            reset();
        }
    }

    /**
     * When File-Engine is starting, the function will be called.
     * You can initialize your plugin here
     */
    @Override
    public void loadPlugin() {
        initSettings();
        CachedThreadPool.getInstance().executeTask(() -> {
            long endTime;
            while (isNotExit) {
                endTime = System.currentTimeMillis();
                if ((endTime - startTime > 300) && timer) {
                    timer = false;
                    //开始显示
                    if (!command.isEmpty()) {
                        addToResultQueue("运行命令:" + command);
                    }
                }
            }
        });
    }

    /**
     * When File-Engine is closing, the function will be called.
     */
    @Override
    public void unloadPlugin() {
        isNotExit = false;
        CachedThreadPool.getInstance().shutdown();
    }

    /**
     * Invoked when a key has been released.See the class description for the swing KeyEvent for a definition of a key released event.
     * Notice : Up and down keys will not be included (key code 38 and 40 will not be included).
     * @param e KeyEvent, Which key on the keyboard is released.
     * @param result Currently selected content.
     */
    @Override
    public void keyReleased(KeyEvent e, String result) {
    }

    /**
     * Invoked when a key has been pressed. See the class description for the swing KeyEvent for a definition of a key pressed event.
     * Notice : Up and down keys will not be included (key code 38 and 40 will not be included).
     * @param e KeyEvent, Which key on the keyboard is pressed.
     * @param result Currently selected content.
     */
    @Override
    public void keyPressed(KeyEvent e, String result) {
        if (e.getKeyCode() == 10) {
            //enter
            String[] strings = colon.split(result);
            if (strings.length == 2) {
                String command = strings[1];
                String batFile = tmpDir + File.separator + "$$bat.bat";
                generateBatFile(command, batFile);
                String start = batFile.substring(0,2);
                String end = batFile.substring(2);
                try {
                    Runtime.getRuntime().exec("cmd.exe /k start " + start + "\"" + end + "\"");
                }catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Invoked when a key has been typed.See the class description for the swing KeyEvent for a definition of a key typed event.
     * Notice : Up and down keys will not be included (key code 38 and 40 will not be included).
     * @param e KeyEvent, Which key on the keyboard is pressed.
     * @param result Currently selected content.
     */
    @Override
    public void keyTyped(KeyEvent e, String result) {
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param e Mouse event
     * @param result Currently selected content.
     */
    @Override
    public void mousePressed(MouseEvent e, String result) {
        if (e.getClickCount() == 2) {
            String[] strings = colon.split(result);
            if (strings.length == 2) {
                String command = strings[1];
                String batFile = tmpDir + File.separator + "$$bat.bat";
                generateBatFile(command, batFile);
                String start = batFile.substring(0,2);
                String end = batFile.substring(2);
                try {
                    Runtime.getRuntime().exec("cmd.exe /k start " + start + "\"" + end + "\"");
                }catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param e Mouse event
     * @param result Currently selected content
     */
    @Override
    public void mouseReleased(MouseEvent e, String result) {
    }

    /**
     * Get the plugin Icon. It can be the png, jpg.
     * Make the icon small, or it will occupy too much memory.
     * @return icon
     */
    @Override
    public ImageIcon getPluginIcon() {
        return new ImageIcon(PluginMain.class.getResource("/cmd.png"));
    }

    /**
     * Get the official site of the plugin.
     * @return official site
     */
    @Override
    public String getOfficialSite() {
        return "https://github.com/XUANXUQAQ/File-Engine-Cmd-Plugin";
    }

    /**
     * Get the plugin version.
     * @return version
     */
    @Override
    public String getVersion() {
        return UpdateUtil._getPluginVersion();
    }

    /**
     * Get the description of the plugin.
     * Just write the description outside, and paste it to the return value.
     * @return description
     */
    @Override
    public String getDescription() {
        return "A plugin to make File-Engine run cmd commands quickly.";
    }

    /**
     * Check if the current version is the latest.
     * @return true or false
     * @see #getUpdateURL()
     */
    @Override
    public boolean isLatest() {
        return UpdateUtil._isLatest();
    }

    /**
     * Get the plugin download url.
     * Invoke when the isLatest() returns false;
     * @see #isLatest()
     * @return download url
     */
    @Override
    public String getUpdateURL() {
        return UpdateUtil._getUpdateURL();
    }

    /**
     * Show the content to the GUI.
     * @param result current selected content.
     * @param label The label to be displayed.
     * @param isChosen If the label is being selected.
     *                 If so, you are supposed to set the label at a different background.
     */
    @Override
    public void showResultOnLabel(String result, JLabel label, boolean isChosen) {
        label.setText(result);
        if (isChosen) {
            label.setBackground(labelColor);
        } else {
            label.setBackground(backgroundColor);
        }
    }

    @Override
    public String getAuthor() {
        return "XUANXU";
    }
}
