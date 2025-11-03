package com.mundosvirtuales.telpoapp

/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 * may have a different license, see the respective files.
 */


import android.animation.Animator
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Toast
import android.widget.ToggleButton
import com.serenegiant.common.BaseActivity
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.CameraDialog.CameraDialogParent
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.USBMonitor.UsbControlBlock
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usbcameracommon.UVCCameraHandler
import com.serenegiant.utils.ViewAnimationHelper
import com.serenegiant.widget.CameraViewInterface

class CameraActivity : BaseActivity(), CameraDialogParent {

    companion object {
        private const val DEBUG = true // TODO set false on release
        private const val TAG = "MainActivity"

        /**
         * set true if you want to record movie using MediaSurfaceEncoder
         * (writing frame data into Surface camera from MediaCodec
         * by almost same way as USBCameratest2)
         * set false if you want to record movie using MediaVideoEncoder
         */
        private const val USE_SURFACE_ENCODER = false

        /**
         * preview resolution(width)
         * if your camera does not support specific resolution and mode,
         * [UVCCamera.setPreviewSize] throw exception
         */
        private const val PREVIEW_WIDTH = 640

        /**
         * preview resolution(height)
         * if your camera does not support specific resolution and mode,
         * [UVCCamera.setPreviewSize] throw exception
         */
        private const val PREVIEW_HEIGHT = 480

        /**
         * preview mode
         * if your camera does not support specific resolution and mode,
         * [UVCCamera.setPreviewSize] throw exception
         * 0:YUYV, other:MJPEG
         */
        private const val PREVIEW_MODE = 1

        const val SETTINGS_HIDE_DELAY_MS = 2500
    }

    /**
     * for accessing USB
     */
    private var mUSBMonitor: USBMonitor? = null

    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private var mCameraHandler: UVCCameraHandler? = null

    /**
     * for camera preview display
     */
    private var mUVCCameraView: CameraViewInterface? = null

    /**
     * for open&start / stop&close camera preview
     */
    private var mCameraButton: ToggleButton? = null

    /**
     * button for start/stop recording
     */
    private var mCaptureButton: ImageButton? = null

    private var mBrightnessButton: View? = null
    private var mContrastButton: View? = null
    private var mResetButton: View? = null
    private var mToolsLayout: View? = null
    private var mValueLayout: View? = null
    private var mSettingSeekbar: SeekBar? = null

    private var mSettingMode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DEBUG) Log.v(TAG, "onCreate:")
        setContentView(R.layout.activity_main)

        mCameraButton = findViewById<ToggleButton>(R.id.camera_button)
        mCameraButton?.setOnCheckedChangeListener(mOnCheckedChangeListener)

        mCaptureButton = findViewById<ImageButton>(R.id.capture_button)
        mCaptureButton?.setOnClickListener(mOnClickListener)
        mCaptureButton?.visibility = View.INVISIBLE

        val view = findViewById<View>(R.id.camera_view)
        view.setOnLongClickListener(mOnLongClickListener)
        mUVCCameraView = view as CameraViewInterface
        mUVCCameraView?.setAspectRatio((PREVIEW_WIDTH / PREVIEW_HEIGHT.toFloat()).toDouble())

        mBrightnessButton = findViewById(R.id.brightness_button)
        mBrightnessButton?.setOnClickListener(mOnClickListener)
        mContrastButton = findViewById(R.id.contrast_button)
        mContrastButton?.setOnClickListener(mOnClickListener)
        mResetButton = findViewById(R.id.reset_button)
        mResetButton?.setOnClickListener(mOnClickListener)
        mSettingSeekbar = findViewById<SeekBar>(R.id.setting_seekbar)
        mSettingSeekbar?.setOnSeekBarChangeListener(mOnSeekBarChangeListener)

        mToolsLayout = findViewById(R.id.tools_layout)
        mToolsLayout?.visibility = View.INVISIBLE
        mValueLayout = findViewById(R.id.value_layout)
        mValueLayout?.visibility = View.INVISIBLE

        mUSBMonitor = USBMonitor(this, mOnDeviceConnectListener)
        mCameraHandler = UVCCameraHandler.createHandler(
            this, mUVCCameraView,
            if (USE_SURFACE_ENCODER) 0 else 1,
            PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE
        )
    }

    override fun onStart() {
        super.onStart()
        if (DEBUG) Log.v(TAG, "onStart:")
        mUSBMonitor?.register()
        mUVCCameraView?.onResume()
    }

    override fun onStop() {
        if (DEBUG) Log.v(TAG, "onStop:")
        mCameraHandler?.close()
        mUVCCameraView?.onPause()
        setCameraButton(false)
        super.onStop()
    }

    override fun onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:")
        mCameraHandler?.release()
        mCameraHandler = null
        mUSBMonitor?.destroy()
        mUSBMonitor = null
        mUVCCameraView = null
        mCameraButton = null
        mCaptureButton = null
        super.onDestroy()
    }

    /**
     * event handler when click camera / capture button
     */
    private val mOnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.capture_button -> {
                if (mCameraHandler?.isOpened == true) {
                    if (checkPermissionWriteExternalStorage() && checkPermissionAudio()) {
                        if (mCameraHandler?.isRecording == false) {
                            mCaptureButton?.setColorFilter(0xffff0000.toInt()) // turn red
                            mCameraHandler?.startRecording()
                        } else {
                            mCaptureButton?.setColorFilter(0) // return to default color
                            mCameraHandler?.stopRecording()
                        }
                    }
                }
            }
            R.id.brightness_button -> {
                showSettings(UVCCamera.PU_BRIGHTNESS)
            }
            R.id.contrast_button -> {
                showSettings(UVCCamera.PU_CONTRAST)
            }
            R.id.reset_button -> {
                resetSettings()
            }
        }
    }

    private val mOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { compoundButton, isChecked ->
        if (compoundButton.id != R.id.camera_button) {
            return@OnCheckedChangeListener
        }

        // --- LOG AÑADIDO ---
        if (DEBUG) Log.v(TAG, "mCameraButton onCheckedChanged, isChecked=$isChecked")

        if (isChecked && mCameraHandler?.isOpened == false) {
            // --- LOG AÑADIDO ---
            if (DEBUG) Log.v(TAG, "mCameraHandler not open, showing CameraDialog.")
            CameraDialog.showDialog(this@CameraActivity)
        } else {
            // --- LOG AÑADIDO ---
            if (DEBUG) Log.v(TAG, "Closing camera handler.")
            mCameraHandler?.close()
            setCameraButton(false)
        }
    }

    /**
     * capture still image when you long click on preview image(not on buttons)
     */
    private val mOnLongClickListener = View.OnLongClickListener { view ->
        if (view.id != R.id.camera_view) {
            return@OnLongClickListener false
        }

        if (mCameraHandler?.isOpened == true) {
            if (checkPermissionWriteExternalStorage()) {
                mCameraHandler?.captureStill()
            }
            return@OnLongClickListener true
        }
        false
    }

    private fun setCameraButton(isOn: Boolean) {
        runOnUiThread({
            mCameraButton?.let {
                try {
                    it.setOnCheckedChangeListener(null)
                    it.isChecked = isOn
                } finally {
                    it.setOnCheckedChangeListener(mOnCheckedChangeListener)
                }
            }
            if (!isOn) {
                mCaptureButton?.visibility = View.INVISIBLE
            }
        }, 0)
        updateItems()
    }

    private fun startPreview() {
        // --- LOG AÑADIDO ---
        if (DEBUG) Log.v(TAG, "Inside startPreview()...")
        val st = mUVCCameraView?.surfaceTexture

        // --- LOG AÑADIDO ---
        if (st == null) {
            if (DEBUG) Log.e(TAG, "startPreview: SurfaceTexture es NULL! No se puede iniciar el preview.")
            return
        }

        // --- LOG AÑADIDO ---
        if (DEBUG) Log.v(TAG, "SurfaceTexture es VÁLIDO. Llamando a mCameraHandler.startPreview().")

        // --- BLOQUE TRY-CATCH AÑADIDO ---
        try {
            mCameraHandler?.startPreview(Surface(st))
        } catch (e: Exception) {
            if (DEBUG) Log.e(TAG, "mCameraHandler.startPreview() FALLÓ:", e)
            setCameraButton(false) // Apagar el botón si falla
            return // No continuar si el preview falló
        }

        runOnUiThread {
            // --- LOG AÑADIDO ---
            if (DEBUG) Log.v(TAG, "Setting mCaptureButton to VISIBLE.")
            mCaptureButton?.visibility = View.VISIBLE
        }
        updateItems()
    }

    private val mOnDeviceConnectListener = object : OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            // --- LOG AÑADIDO ---
            if (DEBUG) Log.v(TAG, "onAttach: ${device.deviceName}")
            Toast.makeText(this@CameraActivity, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show()
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: UsbControlBlock, createNew: Boolean) {
            if (DEBUG) Log.v(TAG, "onConnect: device=${device.deviceName}")

            // --- LOG AÑADIDO ---
            if (DEBUG) Log.v(TAG, "Llamando a mCameraHandler.open()...")

            // --- BLOQUE TRY-CATCH AÑADIDO ---
            try {
                mCameraHandler?.open(ctrlBlock)
                // --- LOG AÑADIDO ---
                if (DEBUG) Log.v(TAG, "mCameraHandler.open() EXITOSO.")
                // --- LOG AÑADIDO ---
                if (DEBUG) Log.v(TAG, "Llamando a startPreview()...")
                startPreview()
                updateItems()
            } catch (e: Exception) {
                // --- LOG AÑADIDO ---
                if (DEBUG) Log.e(TAG, "mCameraHandler.open() FALLÓ:", e)
                setCameraButton(false)
            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: UsbControlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect: ${device.deviceName}")
            mCameraHandler?.let {
                queueEvent({
                    // --- LOG AÑADIDO ---
                    if (DEBUG) Log.v(TAG, "onDisconnect: Cerrando camera handler en queueEvent.")
                    mCameraHandler?.close()
                }, 0)
                setCameraButton(false)
                updateItems()
            }
        }

        override fun onDettach(device: UsbDevice) {
            // --- LOG AÑADIDO ---
            if (DEBUG) Log.v(TAG, "onDettach: ${device.deviceName}")
            Toast.makeText(this@CameraActivity, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show()
        }

        override fun onCancel(device: UsbDevice) {
            // --- LOG AÑADIDO ---
            if (DEBUG) Log.w(TAG, "onCancel: Permiso denegado para el dispositivo ${device.deviceName}")
            setCameraButton(false)
        }
    }

    /**
     * to access from CameraDialog
     */
    override fun getUSBMonitor(): USBMonitor? {
        return mUSBMonitor
    }

    override fun onDialogResult(canceled: Boolean) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=$canceled")
        if (canceled) {
            setCameraButton(false)
        }
        // --- LOG AÑADIDO ---
        if (!canceled) {
            if (DEBUG) Log.i(TAG, "onDialogResult: NO cancelado. Esperando onConnect... (Asegúrate de aceptar el permiso USB)")
        }
    }

    //================================================================================
    private fun isActive(): Boolean {
        return mCameraHandler?.isOpened == true
    }

    private fun checkSupportFlag(flag: Int): Boolean {
        return mCameraHandler?.checkSupportFlag(flag.toLong()) == true
    }

    private fun getValue(flag: Int): Int {
        return mCameraHandler?.getValue(flag) ?: 0
    }

    private fun setValue(flag: Int, value: Int): Int {
        return mCameraHandler?.setValue(flag, value) ?: 0
    }

    private fun resetValue(flag: Int): Int {
        return mCameraHandler?.resetValue(flag) ?: 0
    }

    private fun updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100)
    }

    private val mUpdateItemsOnUITask = Runnable {
        if (isFinishing) return@Runnable
        val visibleActive = if (isActive()) View.VISIBLE else View.INVISIBLE
        mToolsLayout?.visibility = visibleActive
        mBrightnessButton?.visibility =
            if (checkSupportFlag(UVCCamera.PU_BRIGHTNESS)) visibleActive else View.INVISIBLE
        mContrastButton?.visibility =
            if (checkSupportFlag(UVCCamera.PU_CONTRAST)) visibleActive else View.INVISIBLE
    }

    /**
     * 設定画面を表示
     */
    private fun showSettings(mode: Int) {
        if (DEBUG) Log.v(TAG, String.format("showSettings:%08x", mode))
        hideSetting(false)
        if (isActive()) {
            when (mode) {
                UVCCamera.PU_BRIGHTNESS, UVCCamera.PU_CONTRAST -> {
                    mSettingMode = mode
                    mSettingSeekbar?.progress = getValue(mode)
                    ViewAnimationHelper.fadeIn(mValueLayout, -1, 0, mViewAnimationListener)
                }
            }
        }
    }

    private fun resetSettings() {
        if (isActive()) {
            when (mSettingMode) {
                UVCCamera.PU_BRIGHTNESS, UVCCamera.PU_CONTRAST -> {
                    mSettingSeekbar?.progress = resetValue(mSettingMode)
                }
            }
        }
        mSettingMode = -1
        ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener)
    }

    /**
     * 設定画面を非表示にする
     * @param fadeOut trueならばフェードアウトさせる, falseなら即座に非表示にする
     */
    protected fun hideSetting(fadeOut: Boolean) {
        removeFromUiThread(mSettingHideTask)
        if (fadeOut) {
            runOnUiThread({
                ViewAnimationHelper.fadeOut(mValueLayout, -1, 0, mViewAnimationListener)
            }, 0)
        } else {
            try {
                mValueLayout?.visibility = View.GONE
            } catch (e: Exception) {
                // ignore
            }
            mSettingMode = -1
        }
    }

    protected val mSettingHideTask = Runnable {
        hideSetting(true)
    }

    /**
     * 設定値変更用のシークバーのコールバックリスナー
     */
    private val mOnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            // 設定が変更された時はシークバーの非表示までの時間を延長する
            if (fromUser) {
                runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS.toLong())
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            // シークバーにタッチして値を変更した時はonProgressChangedへ
            // 行かないみたいなのでここでも非表示までの時間を延長する
            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS.toLong())
            if (isActive() && checkSupportFlag(mSettingMode)) {
                when (mSettingMode) {
                    UVCCamera.PU_BRIGHTNESS, UVCCamera.PU_CONTRAST -> {
                        setValue(mSettingMode, seekBar.progress)
                    }
                }
            }
        }
    }

    private val mViewAnimationListener = object : ViewAnimationHelper.ViewAnimationListener {
        override fun onAnimationStart(animator: Animator, target: View, animationType: Int) {
//            if (DEBUG) Log.v(TAG, "onAnimationStart:")
        }

        override fun onAnimationEnd(animator: Animator, target: View, animationType: Int) {
            val id = target.id
            when (animationType) {
                ViewAnimationHelper.ANIMATION_FADE_IN,
                ViewAnimationHelper.ANIMATION_FADE_OUT -> {
                    val fadeIn = animationType == ViewAnimationHelper.ANIMATION_FADE_IN
                    if (id == R.id.value_layout) {
                        if (fadeIn) {
                            runOnUiThread(mSettingHideTask, SETTINGS_HIDE_DELAY_MS.toLong())
                        } else {
                            mValueLayout?.visibility = View.GONE
                            mSettingMode = -1
                        }
                    } else if (!fadeIn) {
//                        target.visibility = View.GONE
                    }
                }
            }
        }

        override fun onAnimationCancel(animator: Animator, target: View, animationType: Int) {
//            if (DEBUG) Log.v(TAG, "onAnimationStart:")
        }
    }
}
