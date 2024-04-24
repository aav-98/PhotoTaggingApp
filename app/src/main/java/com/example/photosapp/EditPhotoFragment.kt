package com.example.photosapp

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImageView
import com.example.photosapp.data.model.Tool
import com.example.photosapp.data.model.ToolType
import com.example.photosapp.databinding.FragmentEditPhotoBinding
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.GPUImageRenderer
import jp.co.cyberagent.android.gpuimage.PixelBuffer
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.util.Rotation
import kotlin.math.roundToInt


class EditPhotoFragment : Fragment(), CropImageView.OnCropImageCompleteListener {

    private var _binding: FragmentEditPhotoBinding? = null
    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }
    private val args: PreviewFragmentArgs by navArgs()
    private val TAG = javaClass.simpleName

    private var imageBitMap: Bitmap? = null

    private lateinit var toolsRecyclerView: RecyclerView
    private lateinit var adjustmentSeekBar: SeekBar
    private lateinit var gpuImage: GPUImage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditPhotoBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolsRecyclerView = binding.toolsRecyclerView
        adjustmentSeekBar = binding.adjustmentSeekBar
        gpuImage = GPUImage(this.context)
        gpuImage.setGLSurfaceView(binding.glSurfaceView)
        binding.cropImageView.setOnCropImageCompleteListener(this)

        val tools = listOf(
            Tool("Brightness", R.drawable.baseline_brightness_6_24, ToolType.BRIGHTNESS),
            Tool("Contrast", R.drawable.outline_contrast_black_24, ToolType.CONTRAST),
            Tool("Saturation", R.drawable.baseline_color_lens_24, ToolType.SATURATION),
            Tool("Crop", R.drawable.baseline_crop_24, ToolType.CROP),
            Tool("Rotate", R.drawable.outline_rotate_right_black_24, ToolType.ROTATE)
        )
        toolsRecyclerView.adapter = ToolsAdapter(tools) { selectedTool ->
            handleToolSelection(selectedTool)
        }

        photoViewModel.editedPhoto.value?.let {bitmap ->
            imageBitMap = bitmap
            binding.image.setImageBitmap(imageBitMap)
            Log.d(TAG, "Image has been set")
        }

        binding.saveButton.setOnClickListener {
            Log.d(TAG, "Save button clicked")
            imageBitMap?.let {photoViewModel.setEditedPhoto(imageBitMap as Bitmap)}
            if (args.mode == "new") findNavController().navigate(EditPhotoFragmentDirections.actionEditPhotoFragmentToPreviewFragment("newEdit"))
            else findNavController().navigate(EditPhotoFragmentDirections.actionEditPhotoFragmentToPreviewFragment("editEdit"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleToolSelection(tool: Tool) {
        toolsRecyclerView.visibility = View.GONE

        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        val topPad = (activity as? AppCompatActivity)?.supportActionBar?.height?.times(0.80)?.roundToInt()
        if (topPad != null) {
            binding.editPhotoLayout.setPadding(0, topPad, 0, 0)
        }

        when (tool.toolType) {
            ToolType.BRIGHTNESS -> adjustFilter(GPUImageBrightnessFilter(), Pair(-1.0f, 1.0f), 50)
            ToolType.CONTRAST -> adjustFilter(GPUImageContrastFilter(), Pair(0.0f, 4.0f), 25)
            ToolType.SATURATION -> adjustFilter(GPUImageSaturationFilter(), Pair(0.0f, 2.0f), 50)
            ToolType.CROP -> cropOrRotateImage(true)
            ToolType.ROTATE -> cropOrRotateImage(false)
        }
    }

    private fun adjustFilter(filter: GPUImageFilter, rangeLimits: Pair<Float, Float>, defaultProgress: Int) {
        binding.image.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE
        binding.gpuImageLayout.visibility = View.VISIBLE
        binding.doneButton.visibility = View.VISIBLE // Using a common 'Done' button

        gpuImage.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        gpuImage.setImage(imageBitMap)
        gpuImage.setFilter(filter)

        adjustmentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val adjustedValue = range(progress, rangeLimits.first, rangeLimits.second)
                when (filter) {
                    is GPUImageBrightnessFilter -> filter.setBrightness(adjustedValue)
                    is GPUImageContrastFilter -> filter.setContrast(adjustedValue)
                    is GPUImageSaturationFilter -> filter.setSaturation(adjustedValue)
                }
                gpuImage.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.adjustmentSeekBar.max = 100
        binding.adjustmentSeekBar.progress = defaultProgress

        val backgroundColorInt = ContextCompat.getColor(requireActivity(), R.color.bgr_color)
        val red = Color.red(backgroundColorInt) / 255f
        val green = Color.green(backgroundColorInt) / 255f
        val blue = Color.blue(backgroundColorInt) / 255f
        gpuImage.setBackgroundColor(red, green, blue)

        binding.doneButton.setOnClickListener {
            finalizeFilterAdjustments(filter)
        }

        binding.cancelButton.setOnClickListener {
            resetUIAfterEditing()
        }
    }

    private fun finalizeFilterAdjustments(filter: GPUImageFilter) {
        val renderer = GPUImageRenderer(filter)
        renderer.setRotation(Rotation.NORMAL, renderer.isFlippedHorizontally(), renderer.isFlippedVertically())
        renderer.setScaleType(GPUImage.ScaleType.CENTER_CROP)
        val buffer = PixelBuffer(imageBitMap!!.width, imageBitMap!!.height)
        buffer.setRenderer(renderer)
        renderer.setImageBitmap(imageBitMap, false)
        renderer.setFilter(filter)

        val result = buffer.bitmap

        if (result != null) {
            Log.d(TAG, "Adjustment completed with result: ${result.toString()}")
            imageBitMap = result
            binding.image.setImageBitmap(imageBitMap)
        }

        renderer.deleteImage()
        buffer.destroy()

        resetUIAfterEditing()
        if(!binding.saveButton.isEnabled) binding.saveButton.isEnabled = true
    }

    private fun cropOrRotateImage(isCrop: Boolean) {
        binding.image.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.cropImageView.visibility = View.VISIBLE
        binding.doneButton.visibility = View.VISIBLE
        binding.cancelButton.visibility = View.VISIBLE

        binding.cropImageView.isShowCropOverlay = isCrop

        if (isCrop) {
            binding.doneButton.text = getString(R.string.crop_button_text)
            binding.cropImageView.scaleType = CropImageView.ScaleType.CENTER_INSIDE
        } else {
            binding.rotateButton.visibility = View.VISIBLE
            binding.cropImageView.setOnCropImageCompleteListener(this)
            binding.rotateButton.setOnClickListener {rotate()}
        }

        binding.doneButton.setOnClickListener {
            binding.cropImageView.croppedImageAsync()
        }

        binding.cancelButton.setOnClickListener {
            resetUIAfterEditing()
        }

        binding.cropImageView.setImageBitmap(imageBitMap)
    }

    private fun rotate() {
        binding.cropImageView.rotateImage(90);
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        Log.d(TAG, "Crop: entered callback function onCropImageComplete")
        if (result.error == null) {
            val editedImage: Bitmap? = result.bitmap
            if (editedImage != null) {
                imageBitMap = editedImage
                Log.d(TAG, "Crop: Success in retrieving cropped photo")
                binding.image.setImageBitmap(imageBitMap)
            } else {
                Log.d(TAG, "Error retrieving edited image")
            }
        } else {
            Log.d(TAG, "Crop: error in retrieving result of edit")
            Log.e(TAG, "Error:", result.error)
        }
        resetUIAfterEditing()
        if(!binding.saveButton.isEnabled) binding.saveButton.isEnabled = true
    }

    private fun resetUIAfterEditing() {
        binding.image.visibility = View.VISIBLE
        binding.toolsRecyclerView.visibility = View.VISIBLE
        binding.saveButton.visibility = View.VISIBLE
        binding.doneButton.visibility = View.GONE
        binding.doneButton.text = getString(R.string.done_button_text)
        binding.cancelButton.visibility = View.GONE

        binding.gpuImageLayout.visibility = View.GONE

        binding.cropImageView.visibility = View.GONE
        binding.rotateButton.visibility = View.GONE


        (activity as? AppCompatActivity)?.supportActionBar?.show()
        binding.editPhotoLayout.setPadding(0, 0, 0, 0)
    }

    private fun range(percentage: Int, start: Float, end: Float): Float {
        return (end - start) * percentage / 100.0f + start
    }

}