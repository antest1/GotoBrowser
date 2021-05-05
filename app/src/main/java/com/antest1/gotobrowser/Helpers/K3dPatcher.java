package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.antest1.gotobrowser.R;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;
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
    private final float decayRate = 0.95f; // The angle becomes 95% after every 10ms

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
        double gotX = (Math.sqrt(1f + Math.abs(gyroX)) - 1) * 0.2f * Math.signum(gyroX);
        return (float)gotX;
    }

    @JavascriptInterface
    public float getY(){
        if (!isEffectEnabled) {
            return 0;
        }
        double gotY = (Math.sqrt(1f + Math.abs(gyroY)) - 1) * 0.2f * Math.signum(gyroY);
        return (float)gotY;
    }

    private void decayTiltAngle() {
        // Slowly rebound the tile angle until it becomes centre
        long newTime = System.currentTimeMillis();
        if (oldTime != 0) {
            double decay = Math.pow(decayRate, (newTime - oldTime) / 10.0);
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
        isPatcherEnabled = sharedPref.getBoolean(PREF_MOD_KANTAI3D, false);

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

    public void onSensorChanged(SensorEvent sensorEvent) {
        int rotation = 0;
        if (activity != null) {
            rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        }
        switch (rotation) {
            default:
            case ROTATION_0:
                gyroX -= sensorEvent.values[1] * 0.5;
                gyroY += sensorEvent.values[0] * 0.5;
                break;
            case ROTATION_90:
                gyroX -= sensorEvent.values[0] * 0.5;
                gyroY -= sensorEvent.values[1] * 0.5;
                break;
            case ROTATION_180:
                gyroX += sensorEvent.values[1] * 0.5;
                gyroY -= sensorEvent.values[0] * 0.5;
                break;
            case ROTATION_270:
                gyroX += sensorEvent.values[0] * 0.5;
                gyroY += sensorEvent.values[1] * 0.5;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("onAccuracyChanged", "onAccuracyChanged: "  + accuracy);
    }

    public static String patchKantai3d(String main_js){
        if (!isPatcherEnabled) {
            return main_js;
        }

        Map<String, String> stringsToReplace = new LinkedHashMap<>();

        stringsToReplace.put("(return .{0,99}\\!\\=.{0,99}\\|\\|null\\!\\=\\(.{0,99}\\=.{0,99}\\(.{0,99}\\)\\)&&\\(.{0,99}\\='_'\\+.{0,99}\\),.{0,99}\\+\\(.{0,99}\\+.{0,99}\\+'\\/'\\+\\(.{0,99}\\+.{0,99}\\(.{0,99},.{0,99}\\)\\)\\+'_'\\+.{0,99}\\+.{0,99}\\+.{0,99}\\+.{0,99}\\(0x0,parseInt\\(.{0,99}\\)\\)\\);)",
                "\n return window.displacementPath = (function () {\n$1\n})();\n");

        stringsToReplace.put("(new PIXI\\[.{0,99}\\]\\(.{0,99}\\[.{0,99}\\]\\[.{0,99}\\],.{0,99},.{0,99}\\);document)",
                "\n window.pixiApp = $1");

        stringsToReplace.put("(\\=[^=]{0,99}\\[[^\\[]{0,99}\\]\\[[^\\[]{0,99}\\]\\([^\\(]{0,99}\\),[^,]{0,99}\\=0x0\\=\\=.{0,99}\\?0x0\\:.{0,99},.{0,99}\\=.{0,99}\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\);)",
                "\n = window.charar $1");

        stringsToReplace.put("(var .{0,99}\\=new PIXI\\[\\(.{0,99}\\)\\]\\(.{0,99}\\);this\\[.{0,99}\\]\\=.{0,99}\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\),this\\[.{0,99}\\]\\[.{0,99}]\\[.{0,99}\\]\\(.{0,99},\\-.{0,99}\\);var [^=]{0,99}=)",
                "$1 window.charal = \n");

        stringsToReplace.put("(\\=[^=]{0,99}\\[[^=]{0,99}\\]\\[[^=]{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\)\\[.{0,99}\\]\\(.{0,99}\\);this\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(\\-.{0,99}\\+.{0,99}\\['x'\\]\\+.{0,99},\\-.{0,99}\\+.{0,99}\\['y'\\]\\),)",
                "\n = window.charah $1");

        stringsToReplace.put("(\\['y'\\]\\),this\\[.{0,99}\\('.{0,99}'\\)\\]\\[.{0,99}\\('.{0,99}'\\)\\]\\(.{0,99}\\+.{0,99},.{0,99}\\-.{0,99}\\);)",
                "$1 " + "\n" +
                "window.portOffset = -window.charal + window.charah.x;//-l+h.x\n" +
                "window.portOffsetR = window.charar;//r\n" +
                "\n" +
                "window.displacementSprite = PIXI.Sprite.fromImage('https://kantai3d.com/'+ window.displacementPath );\n" +
                "\n" +
                "window.displacementFilter.uniforms.textureWidth = this._chara.texture.width;\n" +
                "window.displacementFilter.uniforms.textureHeight = this._chara.texture.height;\n" +
                "\n" +
                "\n" +
                "window.displacementSprite.visible = false;\n" +
                "\n" +
                "window.displacementFilter.padding = 150;\n" +
                "\n" +
                "window.currentChara = this._chara;\n" +
                "\n" +
                "if (window.displacementSprite.width != 1) {\n" +
                "    // The depth map is already loaded\n" +
                "    window.displacementFilter.uniforms.displacementMap = window.displacementSprite.texture;\n" +
                "    window.displacementFilter.uniforms.scale = 1.0;\n" +
                "    window.displacementFilter.uniforms.focus = 0.5;\n" +
                "    window.displacementFilter.uniforms.offset = [0.0, 0.0];\n" +
                "    window.currentChara.filters = [window.displacementFilter];\n" +
                "    window.currentChara.addChild(window.displacementSprite);\n" +
                "} else {\n" +
                "    // The depth map is not loaded yet, and may not exist in server at all\n" +
                "    // Disable the filter first\n" +
                "    window.currentChara.filters = [];\n" +
                "    window.displacementSprite.texture.baseTexture.on('loaded', function(){\n" +
                "        // Re-enable the filter when resource loaded\n" +
                "        window.displacementFilter.uniforms.displacementMap = window.displacementSprite.texture;\n" +
                "        window.displacementFilter.uniforms.scale = 1.0;\n" +
                "        window.displacementFilter.uniforms.focus = 0.5;\n" +
                "        window.displacementFilter.uniforms.offset = [0.0, 0.0];\n" +
                "        window.currentChara.filters = [window.displacementFilter];\n" +
                "        window.currentChara.addChild(window.displacementSprite);\n" +
                "    });\n" +
                "}");


        stringsToReplace.put("(\\=Math\\[.{0,99}\\]\\(.{0,99}\\),.{0,99}\\=0x1\\+0\\.012\\*\\(0\\.5\\+0\\.5\\*.{0,99}\\);this\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\),)",
                "\n = window.charasin $1");


        stringsToReplace.put(
                "(this\\['y'\\]=this\\[.{0,99}\\('.{0,99}'\\)]-1.5\\*.{0,99}\\*1.8;)",
                "$1\n" +
                "window.displacementFilter.uniforms.textureScale = this.scale.x;\n" +
                "\n");

        stringsToReplace.put("$",
                ";\n" +
                "setInterval(refreshGyroData, 10)\n" +
                "\n" +
                "function refreshGyroData() {\n" +
                "  if (window.displacementFilter && window.displacementFilter.uniforms && window.displacementFilter.uniforms.offset) {\n" +
                "    window.displacementFilter.uniforms.offset[0] = window.gyroData.getX();\n" +
                "    window.displacementFilter.uniforms.offset[1] = window.gyroData.getY();\n" +
                "  }" +
                "}" +
                "window.displacementFilter = new PIXI.Filter(null, `" + frag + "`);\n" +
                "\n" +
                "window.displacementFilter.apply = function(filterManager, input, output)\n" +
                "{\n" +
                "  this.uniforms.dimensions = {};\n" +
                "  this.uniforms.dimensions[0] = input.sourceFrame.width;\n" +
                "  this.uniforms.dimensions[1] = input.sourceFrame.height;\n" +
                "\n" +
                "  this.uniforms.padding = this.padding;\n" +
                "  \n" +
                "  this.uniforms.frameWidth = input.size.width;\n" +
                "  this.uniforms.frameHeight = input.size.height;\n" +
                "\n" +
                "  // draw the filter...\n" +
                "  filterManager.applyFilter(this, input, output);\n" +
                "}\n");


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
            "uniform float textureWidth;\n" +
            "uniform float textureHeight;\n" +
            "uniform float frameWidth;\n" +
            "uniform float frameHeight;\n" +
            "\n" +
            "uniform float padding;\n" +
            "uniform vec4 filterArea;\n" +
            "uniform vec4 filterClamp;\n" +
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
            "    return vec2((coord[0] * frameWidth - padding) / textureWidth / textureScale,\n" +
            "                (coord[1] * frameHeight - padding) / textureHeight / textureScale);\n" +
            "}\n" +
            "\n" +
            "vec2 mapCoord2(vec2 coord)\n" +
            "{\n" +
            "    return vec2(coord[0] * frameWidth / textureWidth / textureScale,\n" +
            "                coord[1] * frameHeight / textureHeight / textureScale);\n" +
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
