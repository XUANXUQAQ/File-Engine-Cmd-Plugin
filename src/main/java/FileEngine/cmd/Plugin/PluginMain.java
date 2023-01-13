package FileEngine.cmd.Plugin;

import FileEngine.cmd.Plugin.ThreadPool.CachedThreadPool;
import FileEngine.cmd.Plugin.checkupdate.UpdateUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PluginMain extends Plugin {
    private volatile long startTime;
    private volatile boolean timer = false;
    private volatile boolean isNotExit = true;
    private volatile String command;
    private static final Pattern colon = Pattern.compile(":");
    private final String configurationPath = "plugins/Plugin configuration files/Cmd";
    private final String tmpDir = configurationPath + "/tmp";
    private Color backgroundColor = new Color(0xffffff);
    private Color labelColor = new Color(0xcccccc);
    private ImageIcon cmdIcon;

    private void generateBatFile(String command, String filePath) {
        try (BufferedWriter buffW = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {
            buffW.write(command);
        } catch (IOException e) {
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

    public void clearResultQueue() {
        _clearResultQueue();
    }

    /**
     * Do Not Remove, this is used for File-Engine to tell the plugin the current Theme settings.
     * You can use them on method showResultOnLabel(String, JLabel, boolean).
     * When the label is chosen by user, you could set the label background as chosenLabelColor.
     * When the label isn't chosen by user, you could set the label background as defaultColor.
     * @see #showResultOnLabel(String, JLabel, boolean)
     * @param defaultColor When the label isn't chosen, it will be shown as this color.
     * @param choseLabelColor When the label is chosen, it will be shown as this color.
     */
    @Override
    public void setCurrentTheme(int defaultColor, int choseLabelColor, int borderColor) {
        backgroundColor = new Color(defaultColor);
        labelColor = new Color(choseLabelColor);
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
        cmdIcon = new ImageIcon(Objects.requireNonNull(PluginMain.class.getResource("/cmd.png")));
        File pluginFolder = new File(configurationPath);
        boolean ret;
        if (!pluginFolder.exists()) {
            ret = pluginFolder.mkdirs();
        } else {
            ret = true;
        }
        if (!ret) {
            throw new RuntimeException("初始化文件夹失败");
        }
        File tmpD = new File(tmpDir);
        if (!tmpD.exists()) {
            ret = tmpD.mkdirs();
        }
        if (!ret) {
            throw new RuntimeException("初始化文件夹失败");
        }
        CachedThreadPool.getInstance().executeTask(() -> {
            long endTime;
            try {
                while (isNotExit) {
                    endTime = System.currentTimeMillis();
                    if ((endTime - startTime > 300) && timer) {
                        timer = false;
                        //开始显示
                        if (!command.isEmpty()) {
                            if (!"open".equals(command)) {
                                addToResultQueue("运行命令:" + command);
                            } else {
                                addToResultQueue("打开CMD窗口");
                            }
                        }
                    }
                    TimeUnit.MILLISECONDS.sleep(50);
                }
            }catch (InterruptedException ignored) {
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
            try {
                if ("打开CMD窗口".equals(result)) {
                    Runtime.getRuntime().exec("cmd.exe /k start");
                } else {
                    String[] strings = colon.split(result);
                    if (strings.length == 2) {
                        String command = strings[1];
                        String batFile = tmpDir + File.separator + "$$bat.bat";
                        generateBatFile(command, batFile);
                        String start = batFile.substring(0, 2);
                        String end = batFile.substring(2);
                        Runtime.getRuntime().exec("cmd.exe /k start " + start + "\"" + end + "\"");
                    }
                }
            }catch (IOException e1) {
                e1.printStackTrace();
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
        return new ImageIcon(Objects.requireNonNull(PluginMain.class.getResource("/cmd.png")));
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
        return """
                A plugin to make File-Engine run cmd commands quickly.
                Usage:  input ">cmd ipconfig" ---> run command "ipconfig" in cmd.
                快速运行cmd命令插件
                使用方法： 输入 “>cmd ipconfig” ---> 在cmd中运行命令“ipconfig”.图标来自： https://icons8.com/icon/90807/cmd icon by https://icons8.com""";
    }

    /**
     * Check if the current version is the latest.
     * @return true or false
     * @see #getUpdateURL()
     */
    @Override
    public boolean isLatest() throws IOException {
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
        label.setIcon(cmdIcon);
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
