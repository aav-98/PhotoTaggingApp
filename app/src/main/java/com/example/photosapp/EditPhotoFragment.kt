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
import com.example.photosapp.data.model.Modes

/**
 * Fragment for editing photos with functionalities including brightness, contrast, saturation adjustments,
 * as well as cropping and rotating images. This fragment leverages GPUImage to apply real-time filters
 * and effects to images. The user interactions, like selecting different tools and applying adjustments,
 * are managed through a dedicated UI consisting of sliders and tool selection options. Adjustments can be finalized
 * or reverted, and the edited image can be navigated back to the previous fragment with changes applied.
 */
class EditPhotoFragment : Fragment(), CropImageView.OnCropImageCompleteListener {

    private val TAG = javaClass.simpleName

    private var _binding: FragmentEditPhotoBinding? = null

    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }
    private val args: PreviewFragmentArgs by navArgs()

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
            Tool(getString(R.string.brightness), R.drawable.baseline_brightness_6_24, ToolType.BRIGHTNESS),
            Tool(getString(R.string.contrast), R.drawable.outline_contrast_black_24, ToolType.CONTRAST),
            Tool(getString(R.string.saturation), R.drawable.baseline_color_lens_24, ToolType.SATURATION),
            Tool(getString(R.string.crop), R.drawable.baseline_crop_24, ToolType.CROP),
            Tool(getString(R.string.rotate), R.drawable.outline_rotate_right_black_24, ToolType.ROTATE)
        )
        toolsRecyclerView.adapter = ToolsAdapter(tools) { selectedTool ->
            handleToolSelection(selectedTool)
        }

        photoViewModel.editedPhoto.value?.let {bitmap ->
            imageBitMap = bitmap
            binding.image.setImageBitmap(imageBitMap)
        }

        binding.saveButton.setOnClickListener {
            imageBitMap?.let {photoViewModel.setEditedPhoto(imageBitMap as Bitmap)}
            if (args.mode == Modes.NEW) findNavController().navigate(EditPhotoFragmentDirections.actionEditPhotoFragmentToPreviewFragment(Modes.NEW_WITH_EDITED_PHOTO))
            else findNavController().navigate(EditPhotoFragmentDirections.actionEditPhotoFragmentToPreviewFragment(Modes.EDIT_WITH_EDITED_PHOTO))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Handles tool selection for photo editing by hiding the tools' RecyclerView and adjusting the photo layout.
     * Depending on the selected tool type, this function applies specific filters or performs image operations
     * like cropping or rotating.
     *
     * @param tool The editing tool selected by the user, defined by its type (e.g., brightness, contrast, saturation, crop, rotate).
     */
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

    /**
     * Configures and applies a specific GPUImageFilter to the image, adjusting its visual properties based on user input.
     *
     * @param filter The GPUImageFilter to be applied to the image.
     * @param rangeLimits A pair defining the minimum and maximum values for the filter adjustment.
     * @param defaultProgress The initial position of the seek bar representing the filter's starting adjustment level.
     */
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

    /**
     * Applies the finalized filter adjustments to the image and updates the UI accordingly.
     * This method renders the image with the specified filter adjustments and displays the result,
     * updating the UI to reflect the changes made by the user during the editing process.
     *
     * @param filter The GPUImageFilter representing the finalized adjustments to be applied to the image.
     */
    private fun finalizeFilterAdjustments(filter: GPUImageFilter) {
        val renderer = GPUImageRenderer(filter)
        renderer.setRotation(Rotation.NORMAL, renderer.isFlippedHorizontally, renderer.isFlippedVertically)
        renderer.setScaleType(GPUImage.ScaleType.CENTER_CROP)
        val buffer = PixelBuffer(imageBitMap!!.width, imageBitMap!!.height)
        buffer.setRenderer(renderer)
        renderer.setImageBitmap(imageBitMap, false)
        renderer.setFilter(filter)

        val result = buffer.bitmap

        if (result != null) {
            imageBitMap = result
            binding.image.setImageBitmap(imageBitMap)
        }

        renderer.deleteImage()
        buffer.destroy()

        resetUIAfterEditing()
        if(!binding.saveButton.isEnabled) binding.saveButton.isEnabled = true
    }

    /**
    * Displays the crop or rotate options for the image editing interface.
    * This method shows the CropImageView with crop overlay if specified,
    * or displays the rotate button if rotation is selected,
    * along with the necessary buttons for finalizing or cancelling the operation.
    *
    * @param isCrop Boolean value indicating whether to show crop overlay or rotate option.
    */
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
        binding.cropImageView.rotateImage(90)
    }

    /**
     * Callback method invoked when the image cropping operation is completed.
     * Handles the result of the crop operation, updating the image bitmap if successful,
     * and resetting the UI after editing.
     *
     * @param view The CropImageView instance triggering the callback.
     * @param result The CropResult containing the result of the cropping operation.
     */
    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            val editedImage: Bitmap? = result.bitmap
            if (editedImage != null) {
                imageBitMap = editedImage
                binding.image.setImageBitmap(imageBitMap)
            } else {
                Log.e(TAG, "Error retrieving edited image")
            }
        } else {
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