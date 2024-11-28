package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.antest1.gotobrowser.R;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
import static com.antest1.gotobrowser.Constants.PREF_LEGACY_RENDERER;
import static com.antest1.gotobrowser.Constants.PREF_MOD_KANTAI3D;

public class K3dPatcher implements SensorEventListener {
    private Activity activity;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;

    private float gyroX = 0f;
    private float gyroY = 0f;

    private static boolean isPatcherEnabled = false;
    private boolean isEffectEnabled = true; // for user to temporarily disable the effect in-game

    private long oldTime = 0;

    public boolean isPatcherEnabled() {
        return isPatcherEnabled;
    }

    public boolean isEffectEnabled() {
        return isEffectEnabled;
    }

    public void setEffectEnabled(boolean effectEnabled) {
        isEffectEnabled = effectEnabled;
    }

    @JavascriptInterface
    public float getX(){
        if (!isEffectEnabled) {
            return 0;
        }
        decayTiltAngle();
        float sign = Math.signum(gyroX);
        double num = Math.abs(gyroX) * 0.000002;
        double gotX = Math.sqrt(1.0 + num) - 1.0;
        return (float)gotX * sign ;
    }

    @JavascriptInterface
    public float getY(){
        if (!isEffectEnabled) {
            return 0;
        }
        float sign = Math.signum(gyroY);
        double num = Math.abs(gyroY) * 0.000002;
        double gotY = Math.sqrt(1.0 + num) - 1.0;
        return (float)gotY * sign ;
    }

    private String imageUrl = null;
    private boolean depthMapLoaded = false;

    @JavascriptInterface
    public void notifyError(String newImageUrl){
        imageUrl = newImageUrl;
        depthMapLoaded = false;
    }

    @JavascriptInterface
    public void notifyLoaded(String newImageUrl){
        imageUrl = newImageUrl;
        depthMapLoaded = true;
    }

    private void decayTiltAngle() {
        // Slowly rebound the tile angle until it becomes centre
        long newTime = System.currentTimeMillis();
        if (oldTime != 0) {
            // The angle becomes 95% after every 10ms
            double decay = Math.pow(0.994359f, (newTime - oldTime) / 1.0);
            gyroX *= decay;
            gyroY *= decay;
        }
        oldTime = newTime;
    }

    public void prepare(Activity activity) {
        // Only update the enable status when opening the browser view
        // Require reopening the browser after switching the MOD on or off
        SharedPreferences sharedPref = activity.getSharedPreferences(
                activity.getString(R.string.preference_key), Context.MODE_PRIVATE);

        // Kantai3D is disabled if using a legacy renderer
        isPatcherEnabled = sharedPref.getBoolean(PREF_MOD_KANTAI3D, false) &&
                !sharedPref.getBoolean(PREF_LEGACY_RENDERER, false);

        if (isPatcherEnabled) {
            this.activity = activity;
            mSensorManager = (SensorManager)activity.getSystemService(SENSOR_SERVICE);
            if (mSensorManager != null) {
                mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
            }
        }
    }

    public void pause() {
        if (isPatcherEnabled && mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    public void resume() {
        if (isPatcherEnabled && mSensorManager != null) {
            mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    long lastEventTimestamp = 0L;

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (lastEventTimestamp != 0 && sensorEvent.timestamp != lastEventTimestamp) {
            int rotation = 0;
            if (activity != null) {
                rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            }
            switch (rotation) {
                default:
                case ROTATION_0:
                    gyroX -= sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY += sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    break;
                case ROTATION_90:
                    gyroX -= sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY -= sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    break;
                case ROTATION_180:
                    gyroX += sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY -= sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    break;
                case ROTATION_270:
                    gyroX += sensorEvent.values[0] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
                    gyroY += sensorEvent.values[1] * (sensorEvent.timestamp - lastEventTimestamp) / 1000;
            }
        }

        lastEventTimestamp = sensorEvent.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.k3d_form, null);

        if (imageUrl != null) {
            final TextView textView = dialogView.findViewById(R.id.kantai3d_msg_text);
            textView.setText(String.format(Locale.US, activity.getString(depthMapLoaded ?
                    R.string.msg_kantai3d_loaded : R.string.msg_kantai3d_error), imageUrl));
        }

        final SwitchCompat switchCompat = dialogView.findViewById(R.id.switch_3d);
        switchCompat.setChecked(isEffectEnabled());

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.text_save, (dialog, which) -> {
            // Make the change effective
            setEffectEnabled(switchCompat.isChecked());
            dialog.dismiss();
        });

        builder.setNegativeButton(R.string.text_cancel, (dialog, which) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static String patchKantai3d(String main_js){
        if (!isPatcherEnabled) {
            return main_js;
        }

        Map<String, String> stringsToReplace = new LinkedHashMap<>();



        stringsToReplace.put("(=[^,;=]{0,50}\\(this\\)\\|\\|this;return ([^,;=]{0,50}=new PIXI.{0,80},){6}[^,;=]{0,50}=new [^;=]{0,80}\\((0x4f|79),(0xa4|164)\\),)",
                "= window.sound_options $1");

        stringsToReplace.put("(=new )([^,;=]{0,20})(,[^,;=]{0,70}\\((0x5a|90),(0x18e|398)\\),)",
                "$1$2$3\n" +
                        "    window.sound_options._toggle3d = new $2,\n" +
                        "    window.sound_options._toggle3d.position.set(-120, 150),\n" +
                        "    window.sound_options._toggle3d_ai = new $2,\n" +
                        "    window.sound_options._toggle3d_ai.position.set(-120, 215),\n" +
                        "    window.sound_options._toggle3d_text = new PIXI.Text('Kantai3D v3.7\\\\n\\\\nExtra Depth Maps', new PIXI.TextStyle({fontFamily: \"Georgia\", fontSize: 28, fill: '#ffffff'})),\n" +
                        "    window.sound_options._toggle3d_text.position.set(-360, 150),\n" +
                        "    window.sound_options._toggle3d_ai_text = new PIXI.Text('Extra depth maps are generated by AI \\\\nfor all CGs without a painted depth map.\\\\n3D Effects are usually rougher.\\\\n\\\\nChanges will be effective on next return \\\\nto Home Port.', new PIXI.TextStyle({fontFamily: \"Georgia\", fontSize: 18, fill: '#ffffff'})),\n" +
                        "    window.sound_options._toggle3d_ai_text.position.set(-360, 290),\n" +
                        "    window.sound_options._toggle3d_bg = new PIXI.Graphics(),\n" +
                        "    window.sound_options._toggle3d_bg.beginFill(0x202020, 0.8).lineStyle(2, 0xd1b44b, 0.8).drawRect(7-373-15, 56, 373, 413),\n" +
                        "    window.sound_options.addChild(window.sound_options._toggle3d_bg),\n" +
                        "    window.sound_options.addChild(window.sound_options._toggle3d),\n" +
                        "    window.sound_options.addChild(window.sound_options._toggle3d_ai),\n" +
                        "    window.sound_options.addChild(window.sound_options._toggle3d_text),\n" +
                        "    window.sound_options.addChild(window.sound_options._toggle3d_ai_text),");

        stringsToReplace.put("(\\((0x26|38),(0x18e|398)\\),this[^,;=]{0,60}\\((0xc6|198),(0x18e|398)\\),)",
                "$1\n" +
                        "    this._toggle3d.initialize(),\n" +
                        "    this._toggle3d_ai.initialize(),");

        stringsToReplace.put("(function\\(\\)\\{[^{]{0,90},([^;=]{0,90}\\(\\),this[^=]{0,90}=null){11})([^,])",
                "$1\n ;" +
                        "    this._toggle3d.dispose();\n" +
                        "    this._toggle3d = null;\n" +
                        "    this._toggle3d_ai.dispose();\n" +
                        "    this._toggle3d_ai = null; $3");

        stringsToReplace.put("(,'skin_id':this\\[[^{]{0,90}\\]\\};)",
                "$1\nwindow.isDepthEnabled = this._sound._toggle3d.value; \n" +
                        "    window.isDepthAiEnabled = this._sound._toggle3d_ai.value;\n" +
                        "    ");

        stringsToReplace.put("((null==this[^,;=]{0,40}&&\\(this[^,;=]{0,40}=[^,;=]{0,70}\\),this[^,;=]{0,40}=[^,;=]{0,70},){2})",
                "$1\n" +
                        "    this._toggle3d.value = window.isDepthEnabled != false,\n" +
                        "    this._toggle3d_ai.value = window.isDepthAiEnabled != false,");



        stringsToReplace.put("\\}(return.{0,599}\\(0(x0)?,parseInt\\([^,;=]{0,20}\\)\\)\\))",
                "\n } return window.displacementPath = (function () {\n$1\n})();\n");

        stringsToReplace.put("(new PIXI[^,;=]{0,20}\\([^,;=]{0,70},[^,;=]{0,60},[^,;=]{0,20}\\);document)",
                "\n window.pixiApp = $1");

        stringsToReplace.put("(var[^=]{0,19}=[^=]{0,29},[^=]{0,39}=[^=]{0,99},[^=]{0,99})(=.{0,99}0x0.{0,99}0x0.{0,99}PIXI.{0,999}\\((0x1eb,-0x58|491,-88)\\);var [^=]{0,99}=)",
                "\n $1  = window.charar $2 window.charal = \n");

        stringsToReplace.put("(\\/0x2.{0,99}\\/0x2,[^,;=]{0,99})(\\=.{0,999}-(0x58|88)[^;]{0,200};?)(\\})",
                "\n $1 = window.charah $2" + "\n" +
                "window.portOffset = -window.charal + window.charah.x;//-l+h.x\n" +
                "window.portOffsetR = window.charar;//r\n" +
                "\n" +
                "\n" +
                "var url1 = window.displacementPath.replace(/resources\\\\/ship\\\\/full[_dmg]*\\\\/([0-9]*)_([0-9_a-z]*).png(\\\\?version=)?([0-9]*)/g, \\\"https://cdn.jsdelivr.net/gh/laplamgor/kantai3d-depth-maps@master/source/\\$1/\\$1_\\$2_v\\$4.png\\\");\n" +
                "var url2 = window.displacementPath.replace(/resources\\\\/ship\\\\/full[_dmg]*\\\\/([0-9]*)_([0-9_a-z]*).png(\\\\?version=)?([0-9]*)/g, \\\"https://kantai3d.com/midas/\\$1_\\$2_v\\$4.png\\\");\n" +
                "var triedUrl2 = false;\n" +
                "\n" +
                "const loadDepthUrl = function(url, chara) {\n" +
                "window.displacementSprite = PIXI.Sprite.fromImage(url);\n" +
                "window.displacementFilter = PIXI.DepthPerspectiveFilter;\n" +
                "\n" +
                "window.displacementFilter.uniforms.textureSize = {\n" +
                "    0: chara.texture.width,\n" +
                "    1: chara.texture.height\n" +
                "};\n" +
                "\n" +
                "window.displacementSprite.visible = false;\n" +
                "\n" +
                "window.displacementFilter.padding = 150;\n" +
                "\n" +
                "window.currentChara = chara;\n" +
                "\n" +
                "if (window.displacementSprite.width != 1) {\n" +
                "    console.log('The depth map for this secretary is already loaded.');\n" +
                "    window.kantai3dInterface.notifyLoaded(window.displacementPath.replace(/resources\\\\/ship\\\\/full[_dmg]*\\\\/([0-9]*)_([0-9_a-z]*).png(\\\\?version=)?([0-9]*)/g, \"\\$1_\\$2_v\\$4\\.png\"));\n" +

                "    // The depth map is already loaded\n" +
                "    window.displacementFilter.uniforms.displacementMap = window.displacementSprite.texture;\n" +
                "    window.displacementFilter.uniforms.scale = 1.0;\n" +
                "    window.displacementFilter.uniforms.focus = 0.5;\n" +
                "    window.displacementFilter.uniforms.offset = [0.0, 0.0];\n" +
                "    window.currentChara.filters = [window.displacementFilter];\n" +
                "    window.currentChara.addChild(window.displacementSprite);\n" +
                "\n" +
                "    window.mousex1 = null;\n" +
                "    window.mousey1 = null;\n" +
                "    prepareJiggle(window.currentChara.texture, window.displacementSprite.texture);\n" +
                "    window.displacementFilter.uniforms.displacementMap = window.jiggledDepthMapRT.texture;\n" +
                "} else {\n" +
                "    // The depth map is not loaded yet, and may not exist in server at all\n" +
                "    // Disable the filter first\n" +
                "    chara.filters = [];\n" +
                "    window.currentChara.filters = [];\n" +
                "    window.displacementSprite.texture.baseTexture.on('loaded', function() {\n" +
                "\n" +
                "        console.log('The depth map for this secretary is now loaded.');\n" +
                "        window.kantai3dInterface.notifyLoaded(window.displacementPath.replace(/resources\\\\/ship\\\\/full[_dmg]*\\\\/([0-9]*)_([0-9_a-z]*).png(\\\\?version=)?([0-9]*)/g, \"\\$1_\\$2_v\\$4\\.png\"));\n" +

                "        // Re-enable the filter when resource loaded\n" +
                "        window.displacementFilter.uniforms.displacementMap = window.displacementSprite.texture;\n" +
                "        window.displacementFilter.uniforms.scale = 1.0;\n" +
                "        window.displacementFilter.uniforms.focus = 0.5;\n" +
                "        window.displacementFilter.uniforms.offset = [0.0, 0.0];\n" +
                "        window.currentChara.filters = [window.displacementFilter];\n" +
                "        window.currentChara.addChild(window.displacementSprite);\n" +
                "\n" +
                "        window.mousex1 = null;\n" +
                "        window.mousey1 = null;\n" +
                "        prepareJiggle(window.currentChara.texture, window.displacementSprite.texture);\n" +
                "        window.displacementFilter.uniforms.displacementMap = window.jiggledDepthMapRT.texture;\n" +
                "    });\n" +
                "    window.displacementSprite.texture.baseTexture.on('error', function() {\n" +
                "        if (!triedUrl2 && window.isDepthAiEnabled != false) {\n" +
                "            triedUrl2 = true;\n" +
                "            loadDepthUrl(url2, chara);\n" +
                "        }\n" +
                "        console.log('The depth map for this secretary is not available. Please visit https://github.com/laplamgor/kantai3d to check the supported CG list and consider to contribute your own depth map.');\n" +
                "        window.kantai3dInterface.notifyError(window.displacementPath.replace(/resources\\\\/ship\\\\/full[_dmg]*\\\\/([0-9]*)_([0-9_a-z]*).png(\\\\?version=)?([0-9]*)/g, \"\\$1_\\$2_v\\$4\\.png\"));\n" +
                "    });\n" +
                "\n" +
                "    if (!window.displacementSprite.texture.baseTexture.isLoading) {\n" +
                "        if (!triedUrl2 && window.isDepthAiEnabled != false) {\n" +
                "            triedUrl2 = true;\n" +
                "            loadDepthUrl(url2, chara);\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "}\n" +
                "if (window.isDepthEnabled != false) {\n" +
                "    loadDepthUrl(url1, this._chara);\n" +
                "} else {\n" +
                "    this._chara.filters = [];\n" +
                "    window.currentChara.filters = [];\n" +
                "}" +

                "///////////////////////////////////\n" +
                "function prepareJiggle(baseMap, depthMap) {\n" +

                "    window.jigglePositions = [];\n" +
                "    window.jiggleVelocities = [];\n" +
                "    window.jiggleForces = [];\n" +

                "    window.jiggleStaticFlags = [];\n" +
                "    window.jiggleMovement = [];\n" +

                "    window.damping = [];\n" +
                "    window.springiness = [];\n" +
                "    \n" +

                "    var depthImg = depthMap.baseTexture.source;\n" +
                "    var tempCanvas = document.createElement('canvas');\n" +
                "    tempCanvas.width = depthImg.width;\n" +
                "    tempCanvas.height = depthImg.height;\n" +
                "    let tmCtx = tempCanvas.getContext('2d');\n" +
                "    tmCtx.drawImage(depthImg, 0, 0);\n" +
                "    var dmData = tmCtx.getImageData(0, 0, depthImg.width, depthImg.height).data;\n" +


                "    window.jiggleMeshW = Math.ceil(baseMap.width / 10.0);\n" +
                "    window.jiggleMeshH = Math.ceil(baseMap.height / 10.0);\n" +

                "    // This is the jiggled mseh\n" +
                "    window.jiggledDepthMapMesh = new PIXI.mesh.Plane(window.displacementSprite.texture, window.jiggleMeshW, window.jiggleMeshH);\n" +
                "    window.jiggledDepthMapMesh.visible = false;\n" +

                "    // This is the render texture of the jiggled mseh\n" +
                "    window.jiggledDepthMapRT = new PIXI.Sprite(PIXI.RenderTexture.create(baseMap.width, baseMap.height));\n" +
                "    window.jiggledDepthMapRT.visible = false;\n" +

                "    window.jiggledBaseMapMesh = new PIXI.mesh.Plane(baseMap, window.jiggleMeshW, window.jiggleMeshH);\n" +

                "    window.pixiApp.stage.addChild(window.jiggledDepthMapMesh);\n" +
                "    window.pixiApp.stage.addChild(window.jiggledDepthMapRT);\n" +
                "    window.currentChara.addChild(window.jiggledBaseMapMesh);\n" +

                "    window.gridW = baseMap.width / (window.jiggleMeshW - 1.0);\n" +
                "    window.gridH = baseMap.height / (window.jiggleMeshH - 1.0);\n" +
                "    for (var y = 0; y < window.jiggleMeshH; y++) {\n" +
                "        for (var x = 0; x < window.jiggleMeshW; x++) {\n" +
                "            window.jigglePositions.push({ x: window.gridW * x, y: y * window.gridH });\n" +
                "            window.jiggleVelocities.push({ x: 0, y: 0 });\n" +
                "            window.jiggleForces.push({ x: 0, y: 0 });\n" +
                "\n" +
                "            var r = dmData[(Math.floor(y * window.gridH) * baseMap.width + Math.floor(x * window.gridW)) * 4 + 0];\n" +
                "            var b = dmData[(Math.floor(y * window.gridH) * baseMap.width + Math.floor(x * window.gridW)) * 4 + 2];\n" +
                "\n" +
                "            window.damping.push(1.0 / (b / 255.0 * 16.0 + 1));\n" +
                "            window.springiness.push(1.0 / ( b / 255.0 * 32.0 + 1));\n" +
                "        \n" +
                "\n" +
                "            window.jiggleStaticFlags.push(b == 0);\n" +
                "            window.jiggleMovement.push((r - 127.0) / 128.0);\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    window.Mx = null;\n" +
                "    window.My = null;\n" +
                "    window.Mx2 = null;\n" +
                "    window.My2 = null;\n" +
                "    \n" +
                "    // start animating\n" +
                "    window.pixiApp.ticker.add(function (t) {\n" +
                "\n" +
                "    });\n" +
                "} $4");


        stringsToReplace.put("(\\=Math[^,;=]{0,40},[^,;=]{0,30}\\=(0x)?1\\+(0)?\\.012[^,;=]{0,10}\\*\\(.{0,29}\\);this[^,;=]{0,90}\\([^,;=]{0,90}\\),this(\\.y|\\['y'\\])=this[^,;=]{0,20}-1.5\\*[^,;=]{0,40}\\*1.8;?)",
                "\n = window.charasin $1 ;\n" +
                "window.displacementFilter.uniforms.textureScale = this.scale.x;\n" +
                "\n");

        stringsToReplace.put("$",
                ";\n" +
                "\n" +
                "function refreshGyroData() {\n" +
                "  if (window.displacementFilter && window.displacementFilter.uniforms && window.displacementFilter.uniforms.offset) {\n" +
                "    window.displacementFilter.uniforms.offset[0] = window.kantai3dInterface.getX();\n" +
                "    window.displacementFilter.uniforms.offset[1] = window.kantai3dInterface.getY();\n" +
                "  }" +
                "}\n" +
                "'use strict';\n" +
                "PIXI.DepthPerspectiveFilter = new PIXI.Filter(null, `" + frag + "`);\n" +

                "\n" +
                "PIXI.DepthPerspectiveFilter.apply = function(filterManager, input, output)\n" +
                "{\n" +
                "  refreshGyroData();\n" +
                "  this.uniforms.dimensions = {\n" +
                "        0: input.sourceFrame.width,\n" +
                "        1: input.sourceFrame.height\n" +
                "      };\n" +
                "\n" +
                "    this.uniforms.padding = this.padding;\n" +
                "    this.uniforms.frameSize = { \n" +
                "        0: input.size.width, \n" +
                "        1: input.size.height\n" +
                "    };\n" +
                "    this.uniforms.filterAreaOffset = { \n" +
                "        0: Math.min(window.currentChara.worldTransform.tx, 0.0), \n" +
                "        1: Math.min(window.currentChara.worldTransform.ty, 0.0)\n" +
                "    };\n" +

                        
                "    ////////\n" +
                "    \n" +
                "    var vertices = window.jiggledBaseMapMesh.vertices;\n" +
                "    var vertices2 = window.jiggledDepthMapMesh.vertices;\n" +
                "\n" +
                "    var newMx = window.displacementFilter.uniforms.offset[0];\n" +
                "    var newMy = window.displacementFilter.uniforms.offset[1];\n" +
                "    \n" +
                "    var baseMap = window.currentChara.texture;\n" +
                "    var depthMap = window.displacementSprite.texture;\n" +
                "    if (baseMap && baseMap.baseTexture && depthMap && depthMap.baseTexture) {\n" +
                "\n" +
                "        window.My2 = window.My;\n" +
                "        window.Mx2 = window.Mx;\n" +
                "        window.My = newMy;\n" +
                "        window.Mx = newMx;\n" +
                "        for (var y = 0; y < window.jiggleMeshH; y++) {\n" +
                "            for (var x = 0; x < window.jiggleMeshW; x++) {\n" +
                "                resetForce(x, y);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        if (window.Mx && window.My && window.Mx2 && window.My2 && newMx != -999999 && window.Mx != -999999 && window.Mx2 != -999999) {\n" +
                "    \n" +
                "            var aX = (window.Mx2 - window.Mx) - (window.Mx - newMx);\n" +
                "            var aY = (window.My2 - window.My) - (window.My - newMy);\n" +
                "    \n" +
                "            for (var y = 0; y < window.jiggleMeshH; y++) {\n" +
                "                for (var x = 0; x < window.jiggleMeshW; x++) {\n" +
                "                    var m = window.jiggleMovement[y * window.jiggleMeshW + x];\n" +
                "                    window.jiggleForces[y * window.jiggleMeshW + x].x += aX * m * -50;\n" +
                "                    window.jiggleForces[y * window.jiggleMeshW + x].y += aY * m * 50;\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    \n" +
                "\n" +
                "        for (var y = 0; y < window.jiggleMeshH; y++) {\n" +
                "            for (var x = 0; x < window.jiggleMeshW; x++) {\n" +
                "                if (x != 0) {\n" +
                "                    springUpdate(x - 1, y, x, y);\n" +
                "                }\n" +
                "                if (y != 0) {\n" +
                "                    springUpdate(x, y - 1, x, y);\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    \n" +
                "    \n" +
                "        for (var y = 0; y < window.jiggleMeshH; y++) {\n" +
                "            for (var x = 0; x < window.jiggleMeshW; x++) {\n" +
                "                addDampingForce(x, y);\n" +
                "                update(x, y);\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "    \n" +
                "        for (var i = 0; i < window.jigglePositions.length; i++) {\n" +
                "            var pos = window.jigglePositions[i];\n" +
                "            vertices[i * 2] = Math.min(Math.max(pos.x, 0), baseMap.width);\n" +
                "            vertices[i * 2 + 1] = Math.min(Math.max(pos.y, 0), baseMap.height);\n" +
                "    \n" +
                "            vertices2[i * 2] = vertices[i * 2];\n" +
                "            vertices2[i * 2 + 1] = vertices[i * 2 + 1];\n" +
                "        }\n" +
                "    }\n" +
                "    ////////\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "    window.jiggledDepthMapMesh.visible = true;\n" +
                "    window.pixiApp.renderer.render(window.jiggledDepthMapMesh, window.jiggledDepthMapRT.texture);\n" +
                "    window.jiggledDepthMapMesh.visible = false;" +
                "  // draw the filter...\n" +
                "  filterManager.applyFilter(this, input, output);\n" +
                "}\n" +
                "" +
                "\n" +
                "function resetForce(x, y) {\n" +
                "    window.jiggleForces[y * window.jiggleMeshW + x] = { x: 0, y: 0 };\n" +
                "}\n" +
                "\n" +
                "function addForce(x, y, addX, addY) {\n" +
                "    var f = window.jiggleForces[y * window.jiggleMeshW + x];\n" +
                "    f.x += addX;\n" +
                "    f.y += addY;\n" +
                "}\n" +
                "\n" +
                "function addDampingForce(x, y) {\n" +
                "    var v = jiggleVelocities[y * window.jiggleMeshW + x];\n" +
                "    var f = window.jiggleForces[y * window.jiggleMeshW + x];\n" +
                "    f.x -= v.x * window.damping[y * window.jiggleMeshW + x];\n" +
                "    f.y -= v.y * window.damping[y * window.jiggleMeshW + x];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "function update(x, y) {\n" +
                "    var p = window.jigglePositions[y * window.jiggleMeshW + x];\n" +
                "    var v = window.jiggleVelocities[y * window.jiggleMeshW + x];\n" +
                "    var f = window.jiggleForces[y * window.jiggleMeshW + x];\n" +
                "\n" +
                "    if (window.jiggleStaticFlags[y * window.jiggleMeshW + x] == false) {\n" +
                "        v.x += f.x;\n" +
                "        v.y += f.y;\n" +
                "        p.x += v.x;\n" +
                "        p.y += v.y;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n" +
                "\n" +
                "function springUpdate(x1, y1, x2, y2) {\n" +
                "    if (window.jiggleStaticFlags[x1 + y1 * window.jiggleMeshW.w] && !window.jiggleStaticFlags[x2 + y2 * window.jiggleMeshW.w]) \n" +
                "        return;\n" +
                "\n" +
                "    var distanceOrigin = (x2 - x1) * window.gridW + (y2 - y1) * window.gridH;\n" +
                "    \n" +
                "    \n" +
                "\n" +
                "    var p1 = window.jigglePositions[y1 * window.jiggleMeshW + x1];\n" +
                "    var p2 = window.jigglePositions[y2 * window.jiggleMeshW + x2];\n" +
                "\n" +
                "    var distance = len(sub(p1, p2));\n" +
                "\n" +
                "    var springiness = (window.springiness[y1 * window.jiggleMeshW + x1] + window.springiness[y2 * window.jiggleMeshW + x2]) / 2;\n" +
                "\n" +
                "    var springForce = springiness * (distanceOrigin - distance);\n" +
                "    var frcToAdd = tim(normalize(sub(p1, p2)), springForce);\n" +
                "\n" +
                "    addForce(x1, y1, frcToAdd.x, frcToAdd.y);\n" +
                "    addForce(x2, y2, -frcToAdd.x, -frcToAdd.y);\n" +
                "}\n" +
                "\n" +
                "\n" +
                "function len(v) {\n" +
                "    return Math.sqrt(v.x * v.x + v.y * v.y);\n" +
                "}\n" +
                "\n" +
                "function normalize(v) {\n" +
                "    var l = len(v);\n" +
                "    return { x: v.x / l, y: v.y / l };\n" +
                "}\n" +
                "\n" +
                "function sub(v1, v2) {\n" +
                "    return { x: v1.x - v2.x, y: v1.y - v2.y }\n" +
                "}\n" +
                "\n" +
                "function tim(v1, s) {\n" +
                "    return { x: v1.x * s, y: v1.y * s }\n" +
                "}");


        String replaced = main_js;
        for (Map.Entry<String, String> stringToReplace : stringsToReplace.entrySet()) {
            Pattern pattern = Pattern.compile(stringToReplace.getKey());
            Matcher matcher = pattern.matcher(replaced);
            if (matcher.find() && !matcher.find()) {
                // Find one and only one match
                matcher.reset();
                replaced = matcher.replaceFirst(stringToReplace.getValue());
            } else {
                // The main.js is probably updated and no longer support the 3D patch currently
                // Immediately return the unpatched main.js
                return main_js;
            }
        }
        return replaced;
    }




    private static final String frag = "precision mediump float;\n" +
            "uniform vec2 offset;\n" +
            "\n" +
            "uniform sampler2D uSampler;\n" +
            "uniform sampler2D displacementMap;\n" +
            "\n" +
            "uniform float textureScale;\n" +
            "\n" +
            "uniform vec2 textureSize;\n" +
            "uniform vec2 frameSize;\n" +
            "uniform vec2 filterAreaOffset;\n" +
            "\n" +
            "uniform float padding;\n" +
            "uniform vec4 filterArea;\n" +
            "uniform vec4 filterClamp;\n" +
            "\n" +
            "\n" +
            "varying vec2 vTextureCoord;\n" +
            "varying vec4 vColor;\n" +
            "uniform vec4 dimensions;\n" +
            "uniform vec2 mapDimensions;\n" +
            "uniform float scale;\n" +
            "uniform float focus;\n" +
            "\n" +
            "\n" +
            "vec2 mapCoordDepth(vec2 coord)\n" +
            "{\n" +
            "    return (coord * (frameSize) - filterAreaOffset - padding) / textureSize / textureScale;\n" +
            "}\n" +
            "\n" +
            "const float compression = 1.0;\n" +
            "const float dmin = 0.0;\n" +
            "const float dmax = 1.0;\n" +
            "\n" +
            "#define MAXSTEPS 600.0\n" +
            "float steps = max(MAXSTEPS *length(offset), 30.0);\n" +
            "\n" +
            "\n" +
            "\n" +
            "void main(void)\n" +
            "{\n" +
            "\n" +
            "    float aspect = dimensions.x / dimensions.y;\n" +
            "    vec2 scale2 =\n" +
            "        vec2(scale * min(1.0, 1.0 / aspect), scale * min(1.0, aspect)) * vec2(1, -1);\n" +
            "    mat2 baseVector =\n" +
            "        mat2(vec2((0.5 - focus) * (offset) - (offset) / 2.0) * scale2,\n" +
            "             vec2((0.5 - focus) * (offset) + (offset) / 2.0) * scale2);\n" +
            "\n" +
            "    vec2 pos = (vTextureCoord);\n" +
            "    mat2 vector = baseVector;\n" +
            "\n" +
            "    float dstep = compression / (steps - 1.0);\n" +
            "    vec2 vstep = (vector[1] - vector[0]) / vec2((steps - 1.0));\n" +
            "\n" +
            "    vec2 posSumLast = vec2(0.0);\n" +
            "    vec2 posSum = vec2(0.0);\n" +
            "\n" +
            "    float weigth = 1.0;\n" +
            "    float dpos;\n" +
            "    float dposLast;\n" +
            "\n" +
            "    for (float i = 0.0; i < MAXSTEPS; ++i)\n" +
            "    {\n" +
            "        vec2 vpos = pos + vector[1] - i * vstep;\n" +
            "        dpos = 1.0 - i * dstep;\n" +
            "        float depth = texture2D(displacementMap, mapCoordDepth(vpos)).r;\n" +
            "\n" +
            "\n" +
            "        if (texture2D(uSampler, vpos)[3] == 0.0)\n" +
            "        {\n" +
            "            depth = 0.0;\n" +
            "        }\n" +
            "\n" +
            "        depth = clamp(depth, dmin, dmax);\n" +
            "\n" +
            "        if (dpos > depth)\n" +
            "        {\n" +
            "            posSumLast = vpos;\n" +
            "            dposLast = dpos;\n" +
            "        }\n" +
            "        else\n" +
            "        {\n" +
            "            posSum = vpos;\n" +
            "            weigth = (depth - dposLast) / dstep;\n" +
            "            break;\n" +
            "        }\n" +
            "    };\n" +
            "\n" +
            "    gl_FragColor = texture2D(\n" +
            "                       uSampler,\n" +
            "                       (posSum - posSumLast) * -clamp(weigth * 0.5 + 0.5, 0.0, 1.5) + posSum);\n" +
            "\n" +
            "}";
}
