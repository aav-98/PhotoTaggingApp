package com.example.photosapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.canhub.cropper.CropImageOptions
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
import jp.co.cyberagent.android.gpuimage.util.Rotation


class EditPhotoFragment : Fragment(), CropImageView.OnCropImageCompleteListener {

    private var _binding: FragmentEditPhotoBinding? = null
    private val binding get() = _binding!!

    private val photoViewModel: PhotoViewModel by activityViewModels {
        PhotoViewModelFactory(requireActivity().applicationContext)
    }

    private val args: PreviewFragmentArgs by navArgs()

    private val TAG = javaClass.simpleName
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEditPhotoBinding.inflate(inflater, container, false)
        return binding.root

    }

    private var imageBitMap: Bitmap? = null

    private lateinit var toolsRecyclerView: RecyclerView
    private lateinit var adjustmentSeekBar: SeekBar
    private lateinit var gpuImage: GPUImage

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

        when (tool.toolType) {
            ToolType.BRIGHTNESS -> adjustBrightness()
            ToolType.CONTRAST -> adjustContrast()
            ToolType.SATURATION -> adjustSaturation()
            ToolType.CROP -> cropImage()
            ToolType.ROTATE -> rotateImage()
        }
    }

    private fun adjustBrightness() {
        binding.image.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.gpuImageLayout.visibility = View.VISIBLE
        binding.brightnessDoneButton.visibility = View.VISIBLE

        val brightnessFilter = GPUImageBrightnessFilter()
        gpuImage.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        gpuImage.setImage(imageBitMap)
        gpuImage.setFilter(brightnessFilter)

        adjustmentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = range(progress, -1.0f, 1.0f)
                brightnessFilter.setBrightness(brightness)
                gpuImage.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.adjustmentSeekBar.setMax(100)
        binding.adjustmentSeekBar.setProgress(50)

        val backgroundColorInt = ContextCompat.getColor(requireActivity(), R.color.bgr_color)
        val red = Color.red(backgroundColorInt) / 255f
        val green = Color.green(backgroundColorInt) / 255f
        val blue = Color.blue(backgroundColorInt) / 255f

        //Log.d(TAG, "Setting background color - R: $red, G: $green, B: $blue")
        gpuImage.setBackgroundColor(red, green, blue)

        binding.brightnessDoneButton.setOnClickListener {
            val renderer = GPUImageRenderer(brightnessFilter)
            renderer.setRotation(
                Rotation.NORMAL,
                renderer.isFlippedHorizontally(), renderer.isFlippedVertically()
            )
            renderer.setScaleType(GPUImage.ScaleType.CENTER_CROP)
            val buffer = PixelBuffer(imageBitMap!!.getWidth(), imageBitMap!!.getHeight())
            buffer.setRenderer(renderer)
            renderer.setImageBitmap(imageBitMap, false)

            renderer.setFilter(brightnessFilter)

            val result = buffer.bitmap

            if (result != null) {
                Log.d(TAG, result.toString())
                imageBitMap = result
                binding.image.setImageBitmap(imageBitMap)
            }

            renderer.deleteImage()
            buffer.destroy()

            binding.brightnessDoneButton.visibility = View.GONE
            binding.gpuImageLayout.visibility = View.GONE
            binding.image.visibility = View.VISIBLE
            binding.toolsRecyclerView.visibility = View.VISIBLE
            binding.saveButton.visibility = View.VISIBLE
        }
    }

    private fun adjustContrast() {
        binding.image.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.gpuImageLayout.visibility = View.VISIBLE
        binding.contrastDoneButton.visibility = View.VISIBLE

        val contrastFilter = GPUImageContrastFilter()
        gpuImage.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        gpuImage.setImage(imageBitMap)
        gpuImage.setFilter(contrastFilter)

        adjustmentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val contrast = range(progress, 0.0F, 4.0f)
                contrastFilter.setContrast(contrast)
                gpuImage.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.adjustmentSeekBar.setMax(100)
        binding.adjustmentSeekBar.setProgress(25)

        val backgroundColorInt = ContextCompat.getColor(requireActivity(), R.color.bgr_color)
        val red = Color.red(backgroundColorInt) / 255f
        val green = Color.green(backgroundColorInt) / 255f
        val blue = Color.blue(backgroundColorInt) / 255f

        //Log.d(TAG, "Setting background color - R: $red, G: $green, B: $blue")
        gpuImage.setBackgroundColor(red, green, blue)

        binding.contrastDoneButton.setOnClickListener {
            val renderer = GPUImageRenderer(contrastFilter)
            renderer.setRotation(
                Rotation.NORMAL,
                renderer.isFlippedHorizontally(), renderer.isFlippedVertically()
            )
            renderer.setScaleType(GPUImage.ScaleType.CENTER_CROP)
            val buffer = PixelBuffer(imageBitMap!!.getWidth(), imageBitMap!!.getHeight())
            buffer.setRenderer(renderer)
            renderer.setImageBitmap(imageBitMap, false)

            renderer.setFilter(contrastFilter)

            val result = buffer.bitmap

            if (result != null) {
                Log.d(TAG, result.toString())
                imageBitMap = result
                binding.image.setImageBitmap(imageBitMap)
            }

            renderer.deleteImage()
            buffer.destroy()

            binding.contrastDoneButton.visibility = View.GONE
            binding.gpuImageLayout.visibility = View.GONE
            binding.image.visibility = View.VISIBLE
            binding.toolsRecyclerView.visibility = View.VISIBLE
            binding.saveButton.visibility = View.VISIBLE
        }
    }

    private fun adjustSaturation() {
        binding.image.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.gpuImageLayout.visibility = View.VISIBLE
        binding.saturationDoneButton.visibility = View.VISIBLE

        val contrastFilter = GPUImageSaturationFilter()
        gpuImage.setScaleType(GPUImage.ScaleType.CENTER_INSIDE)
        gpuImage.setImage(imageBitMap)
        gpuImage.setFilter(contrastFilter)

        adjustmentSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val contrast = range(progress, 0.0F, 2.0f)
                contrastFilter.setSaturation(contrast)
                gpuImage.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.adjustmentSeekBar.setMax(100)
        binding.adjustmentSeekBar.setProgress(50)

        val backgroundColorInt = ContextCompat.getColor(requireActivity(), R.color.bgr_color)
        val red = Color.red(backgroundColorInt) / 255f
        val green = Color.green(backgroundColorInt) / 255f
        val blue = Color.blue(backgroundColorInt) / 255f

        //Log.d(TAG, "Setting background color - R: $red, G: $green, B: $blue")
        gpuImage.setBackgroundColor(red, green, blue)

        binding.saturationDoneButton.setOnClickListener {
            val renderer = GPUImageRenderer(contrastFilter)
            renderer.setRotation(
                Rotation.NORMAL,
                renderer.isFlippedHorizontally(), renderer.isFlippedVertically()
            )
            renderer.setScaleType(GPUImage.ScaleType.CENTER_CROP)
            val buffer = PixelBuffer(imageBitMap!!.getWidth(), imageBitMap!!.getHeight())
            buffer.setRenderer(renderer)
            renderer.setImageBitmap(imageBitMap, false)

            renderer.setFilter(contrastFilter)

            val result = buffer.bitmap

            if (result != null) {
                Log.d(TAG, result.toString())
                imageBitMap = result
                binding.image.setImageBitmap(imageBitMap)
            }

            renderer.deleteImage()
            buffer.destroy()

            binding.saturationDoneButton.visibility = View.GONE
            binding.gpuImageLayout.visibility = View.GONE
            binding.image.visibility = View.VISIBLE
            binding.toolsRecyclerView.visibility = View.VISIBLE
            binding.saveButton.visibility = View.VISIBLE
        }
    }

    private fun cropImage() {
        binding.image.visibility = View.GONE
        binding.saveButton.visibility = View.GONE
        binding.cropImageView.visibility = View.VISIBLE
        binding.cropButton.visibility = View.VISIBLE

        binding.cropImageView.scaleType = CropImageView.ScaleType.CENTER_INSIDE
        binding.cropImageView.isShowCropOverlay = true

        binding.cropButton.setOnClickListener {
            binding.cropImageView.croppedImageAsync()
        }

        binding.cropImageView.setImageBitmap(imageBitMap)
    }

    private fun rotateImage() {
        Log.d(TAG, "entered crop image function")
        binding.image.visibility = View.GONE
        binding.cropImageView.visibility = View.VISIBLE
        binding.saveButton.visibility = View.GONE
        binding.rotateButton.visibility = View.VISIBLE
        binding.doneButton.visibility = View.VISIBLE

        binding.cropImageView.isShowCropOverlay = false

        binding.cropImageView.setOnCropImageCompleteListener(this)

        binding.rotateButton.setOnClickListener {
            rotate()
        }

        binding.doneButton.setOnClickListener {
            binding.cropImageView.croppedImageAsync()
            Log.d(TAG, "CroppedImage: crop button clicked")
        }
        binding.cropImageView.setImageBitmap(imageBitMap)
        Log.d(TAG, "finished executing rotate image function")
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
        binding.image.visibility = View.VISIBLE
        binding.cropImageView.visibility = View.GONE
        binding.toolsRecyclerView.visibility = View.VISIBLE
        binding.cropButton.visibility = View.GONE
        binding.saveButton.visibility = View.VISIBLE
        binding.rotateButton.visibility = View.GONE
        binding.doneButton.visibility = View.GONE
    }

    private fun range(percentage: Int, start: Float, end: Float): Float {
        return (end - start) * percentage / 100.0f + start
    }

}