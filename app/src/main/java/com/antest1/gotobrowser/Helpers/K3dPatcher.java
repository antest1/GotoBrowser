package com.antest1.gotobrowser.Helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.JavascriptInterface;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.view.Surface.*;

public class K3dPatcher implements SensorEventListener {
    private Activity activity;
    private SensorManager mSensorManager;
    private Sensor mGyroscope;

    private float gyroX = 0f;
    private float gyroY = 0f;

    @JavascriptInterface
    public float getX(){
        double gotX = (Math.sqrt(1f + Math.abs(gyroX)) - 1) * 0.2f * Math.signum(gyroX);
        gyroX *= 0.95f;
        return (float)gotX;
    }

    @JavascriptInterface
    public float getY(){
        double gotY = (Math.sqrt(1f + Math.abs(gyroY)) - 1) * 0.2f * Math.signum(gyroY);
        gyroY *= 0.95f;
        return (float)gotY;
    }

    public void prepare(Activity activity) {
        this.activity = activity;
        mSensorManager = (SensorManager)activity.getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(TYPE_GYROSCOPE);
    }

    public void pause() {
        mSensorManager.unregisterListener(this);
    }

    public void resume() {
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
    }

    public void handleRotation(int rotation) {
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
    }

    public static String patchKantai3d(String main_js){

        main_js = main_js.replaceFirst(
                "(return .{0,99}\\!\\=.{0,99}\\|\\|null\\!\\=\\(.{0,99}\\=.{0,99}\\(.{0,99}\\)\\)&&\\(.{0,99}\\='_'\\+.{0,99}\\),.{0,99}\\+\\(.{0,99}\\+.{0,99}\\+'\\/'\\+\\(.{0,99}\\+.{0,99}\\(.{0,99},.{0,99}\\)\\)\\+'_'\\+.{0,99}\\+.{0,99}\\+.{0,99}\\+.{0,99}\\(0x0,parseInt\\(.{0,99}\\)\\)\\);)",
                "\n return window.displacementPath = (function () {\n$1\n})();\n");

        main_js = main_js.replaceFirst(
                "(new PIXI\\[.{0,99}\\]\\(.{0,99}\\[.{0,99}\\]\\[.{0,99}\\],.{0,99},.{0,99}\\);document)",
                "\n window.pixiApp = $1");

        main_js = main_js.replaceFirst(
                "(\\=[^=]{0,99}\\[[^\\[]{0,99}\\]\\[[^\\[]{0,99}\\]\\([^\\(]{0,99}\\),[^,]{0,99}\\=0x0\\=\\=.{0,99}\\?0x0\\:.{0,99},.{0,99}\\=.{0,99}\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\);)",
                "\n = window.charar $1");

        main_js = main_js.replaceFirst(
                "(var .{0,99}\\=new PIXI\\[\\(.{0,99}\\)\\]\\(.{0,99}\\);this\\[.{0,99}\\]\\=.{0,99}\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\),this\\[.{0,99}\\]\\[.{0,99}]\\[.{0,99}\\]\\(.{0,99},\\-.{0,99}\\);var [^=]{0,99}=)",
                "$1 window.charal = \n");

        main_js = main_js.replaceFirst(
                "(\\=[^=]{0,99}\\[[^=]{0,99}\\]\\[[^=]{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\)\\[.{0,99}\\]\\(.{0,99}\\);this\\[.{0,99}\\]\\[.{0,99}\\]\\[.{0,99}\\]\\(\\-.{0,99}\\+.{0,99}\\['x'\\]\\+.{0,99},\\-.{0,99}\\+.{0,99}\\['y'\\]\\),)",
                "\n = window.charah $1");

        main_js = main_js.replaceFirst(
                "(\\['y'\\]\\),this\\[.{0,99}\\('.{0,99}'\\)\\]\\[.{0,99}\\('.{0,99}'\\)\\]\\(.{0,99}\\+.{0,99},.{0,99}\\-.{0,99}\\);)",
                "$1 " + "\n" +
                        "window.portOffset = -window.charal + window.charah.x;//-l+h.x\n" +
                        "window.portOffsetR = window.charar;//r\n" +
                        "\n" +
                        "window.displacementSprite = PIXI.Sprite.fromImage('https://kantai3d.com/'+ window.displacementPath );\n" +
                        "window.displacementFilter = new PIXI.Filter(`"
                        + vert + "`, `" + frag + "`);\n" +
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
                        "}\n" +
                        "\n" +
                        "window.displacementFilter.uniforms.textureWidth = this._chara.texture.width;\n" +
                        "window.displacementFilter.uniforms.textureHeight = this._chara.texture.height;\n" +
                        "window.displacementFilter.padding = 0;\n" +
                        "\n" +
                        "\n" +
                        "window.displacementSprite.visible = false;\n" +
                        "\n" +
                        "window.displacementFilter.padding = 150;\n" +
                        "\n" +
                        "window.currenctChara = this._chara;\n" +
                        "\n" +
                        "if (window.displacementSprite.width != 1) {\n" +
                        "    // The depth map is already loaded\n" +
                        "    window.displacementFilter.uniforms.displacementMap = window.displacementSprite.texture;\n" +
                        "    window.displacementFilter.uniforms.scale = 1.0;\n" +
                        "    window.displacementFilter.uniforms.focus = 0.5;\n" +
                        "    window.displacementFilter.uniforms.offset = [0.0, 0.0];\n" +
                        "    window.currenctChara.filters = [window.displacementFilter];\n" +
                        "    window.currenctChara.addChild(window.displacementSprite);\n" +
                        "} else {\n" +
                        "    // The depth map is not loaded yet, and may not exist in server at all\n" +
                        "    // Disable the filter first\n" +
                        "    this._chara.filters = [];\n" +
                        "    window.currenctChara.filters = [];\n" +
                        "    window.displacementSprite.texture.baseTexture.on('loaded', function(){\n" +
                        "        // Re-enable the filter when resource loaded\n" +
                        "        window.displacementFilter.uniforms.displacementMap = window.displacementSprite.texture;\n" +
                        "        window.displacementFilter.uniforms.scale = 1.0;\n" +
                        "        window.displacementFilter.uniforms.focus = 0.5;\n" +
                        "        window.displacementFilter.uniforms.offset = [0.0, 0.0];\n" +
                        "        window.currenctChara.filters = [window.displacementFilter];\n" +
                        "        window.currenctChara.addChild(window.displacementSprite);\n" +
                        "    });\n" +
                        "}");



        main_js = main_js.replaceFirst(
                "(\\=Math\\[.{0,99}\\]\\(.{0,99}\\),.{0,99}\\=0x1\\+0\\.012\\*\\(0\\.5\\+0\\.5\\*.{0,99}\\);this\\[.{0,99}\\]\\[.{0,99}\\]\\(.{0,99}\\),)",
                "\n = window.charasin $1");


        main_js = main_js.replaceFirst(
                "(this\\['y'\\]=this\\[.{0,99}\\('.{0,99}'\\)]-1.5\\*.{0,99}\\*1.8;)",
                "$1\n" +
                        "var mousex = (window.pixiApp.renderer.plugins.interaction.mouse.global.x/1200.0-0.5);\n" +
                        "var mousey = (window.pixiApp.renderer.plugins.interaction.mouse.global.y/720.0-0.5);\n" +
                        "\n" +
                        "window.displacementFilter.uniforms.textureScale = this.scale.x;\n" +
                        "\n" +
//                        "var flip = (this.parent._chara.transform.position.x - window.portOffset) / (window.portOffsetR) - 0.5;\n" +
//                        "window.displacementFilter.uniforms.offset = [flip * mousex *1.3\n" +
//                        ",0.02 * window.charasin - 0.01 + mousey * 0.6];\n" +
                        "\n");


        return main_js + ";\n" +

                "setInterval(refreshGyroData, 10)\n" +
                "\n" +
                "function refreshGyroData() {\n" +
                "  if (window.displacementFilter && window.displacementFilter.uniforms && window.displacementFilter.uniforms.offset) {\n" +
                "    window.displacementFilter.uniforms.offset[0] = window.gyroData.getX();\n" +
                "    window.displacementFilter.uniforms.offset[1] = window.gyroData.getY();\n" +
                "  }" +
                "}";

    }




    private static final String frag = "precision mediump float;\n" +
            "uniform vec2 offset;\n" +
            "\n" +
            "\n" +
            "\n" +
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
            "\n" +
            "\n" +
            "\n" +
            "\n" +
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

    private static final String vert = "#ifdef GL_ES\n" +
            "precision highp float;\n" +
            "#endif\n" +
            "\n" +
            "attribute vec2 aVertexPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "\n" +
            "uniform mat3 projectionMatrix;\n" +
            "\n" +
            "void main(void)\n" +
            "{\n" +
            "    vTextureCoord = aTextureCoord;\n" +
            "    gl_Position = vec4((projectionMatrix * vec3(aVertexPosition, 1.0)).xy, 0.0, 1.0);\n" +
            "}";
}
