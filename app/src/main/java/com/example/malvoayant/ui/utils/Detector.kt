package com.example.malvoayant.ui.utils
import android.content.Context
import android.graphics.Bitmap
import android.media.FaceDetector.Face
import android.media.FaceDetector.Face.CONFIDENCE_THRESHOLD
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.pow

private val recentDetections = mutableMapOf<String, Detector.DetectionMeta>()
private val dangerousObjects = setOf("fire", "person", "car", "truck", "knife", "scissors")
private const val CACHE_TIMEOUT_SECONDS = 5
private const val MIN_OBJECT_AREA = 0.005f
private const val DISTANCE_THRESHOLD_POI = 0.3f
private const val DISTANCE_MARGIN = 0.2f
private const val SCORE_THRESHOLD = 0.25f

class Detector(
    private val context: Context,
    private val modelPath: String,
    private val labelPath: String,
    private val detectorListener: DetectorListener,
    private val speechHelper: SpeechHelper
) {
    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    // Processing state
    private var isProcessing = false

    fun isReady(): Boolean {
        return interpreter != null &&
                tensorWidth > 0 &&
                tensorHeight > 0 &&
                numChannel > 0 &&
                numElements > 0
    }

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    fun setup() {
        try {
            val model = FileUtil.loadMappedFile(context, modelPath)
            val options = Interpreter.Options()

            // Use 2 threads for stability
            options.numThreads = 2

            interpreter = Interpreter(model, options)

            val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
            val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

            tensorWidth = inputShape[1]
            tensorHeight = inputShape[2]
            numChannel = outputShape[1]
            numElements = outputShape[2]

            Log.d("ModelSetup", "Input shape: ${inputShape.joinToString()}")
            Log.d("ModelSetup", "Output shape: ${outputShape.joinToString()}")
            Log.d("ModelSetup", "numChannel: $numChannel, numElements: $numElements")

            // Load labels
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()

            Log.d("ModelSetup", "Loaded ${labels.size} labels")
        } catch (e: IOException) {
            Log.e("ModelSetup", "Error setting up model", e)
        }
    }

    fun clear() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {
            Log.e("Detector", "Error clearing interpreter", e)
        }
    }

    fun detect(frame: Bitmap) {
        // Skip if already processing or not ready
        if (isProcessing || !isReady()) {
            return
        }

        try {
            isProcessing = true

            val startTime = SystemClock.uptimeMillis()

            // Create a safe copy of the bitmap to prevent recycled bitmap issues
            val safeBitmap = frame.copy(frame.config!!, true)

            // Resize image to model input size
            val resizedBitmap = Bitmap.createScaledBitmap(
                safeBitmap,
                tensorWidth,
                tensorHeight,
                false
            )

            val tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resizedBitmap)
            val processedImage = imageProcessor.process(tensorImage)
            val imageBuffer = processedImage.buffer

            val output = TensorBuffer.createFixedSize(
                intArrayOf(1, numChannel, numElements),
                OUTPUT_IMAGE_TYPE
            )

            // Run inference
            interpreter?.run(imageBuffer, output.buffer)

            // Get boxes
            val rawBoxes = bestBox(output.floatArray)
            if (rawBoxes == null) {
                detectorListener.onEmptyDetect()

                // Clean up
                resizedBitmap.recycle()
                safeBitmap.recycle()
                isProcessing = false
                return
            }

            // Apply filtering
            val finalBoxes = filterDetections(rawBoxes, safeBitmap.height)

            // Clean up
            resizedBitmap.recycle()
            safeBitmap.recycle()

            if (finalBoxes.isEmpty()) {
                detectorListener.onEmptyDetect()
                isProcessing = false
                return
            }

            val inferenceTime = SystemClock.uptimeMillis() - startTime
            detectorListener.onDetect(finalBoxes, inferenceTime)

            Log.d("Detector", "Inference completed in ${inferenceTime}ms with ${finalBoxes.size} detections")

        } catch (e: Exception) {
            Log.e("Detector", "Error during detection", e)
            detectorListener.onEmptyDetect()
        } finally {
            isProcessing = false
        }
    }

    private fun bestBox(array: FloatArray): List<BoundingBox>? {
        try {
            val boundingBoxes = mutableListOf<BoundingBox>()

            for (c in 0 until numElements) {
                var maxConf = -1.0f
                var maxIdx = -1
                var j = 4

                // Find max confidence class for this box
                while (j < numChannel) {
                    val arrayIdx = c + numElements * j
                    if (arrayIdx < array.size && array[arrayIdx] > maxConf) {
                        maxConf = array[arrayIdx]
                        maxIdx = j - 4
                    }
                    j++
                }

                // Filter out low confidence detections early
                if (maxConf > CONFIDENCE_THRESHOLD && maxIdx >= 0 && maxIdx < labels.size) {
                    val clsName = labels[maxIdx]

                    // Make sure all indices are within bounds
                    if (c < array.size &&
                        c + numElements < array.size &&
                        c + numElements * 2 < array.size &&
                        c + numElements * 3 < array.size) {

                        val cx = array[c]
                        val cy = array[c + numElements]
                        val w = array[c + numElements * 2]
                        val h = array[c + numElements * 3]

                        // Calculate box coordinates
                        val x1 = cx - (w/2F)
                        val y1 = cy - (h/2F)
                        val x2 = cx + (w/2F)
                        val y2 = cy + (h/2F)

                        // Skip invalid boxes
                        if (x1 < 0F || x1 > 1F || y1 < 0F || y1 > 1F ||
                            x2 < 0F || x2 > 1F || y2 < 0F || y2 > 1F) continue

                        boundingBoxes.add(
                            BoundingBox(
                                x1 = x1.coerceIn(0f, 1f),
                                y1 = y1.coerceIn(0f, 1f),
                                x2 = x2.coerceIn(0f, 1f),
                                y2 = y2.coerceIn(0f, 1f),
                                cx = cx, cy = cy, w = w, h = h,
                                cnf = maxConf, cls = maxIdx, clsName = clsName
                            )
                        )
                    }
                }
            }

            if (boundingBoxes.isEmpty()) return null

            return applyNMS(boundingBoxes)
        } catch (e: Exception) {
            Log.e("Detector", "Error in bestBox", e)
            return null
        }
    }

    private fun applyNMS(boxes: List<BoundingBox>): MutableList<BoundingBox> {
        try {
            val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
            val selectedBoxes = mutableListOf<BoundingBox>()

            while(sortedBoxes.isNotEmpty()) {
                val first = sortedBoxes.first()
                selectedBoxes.add(first)
                sortedBoxes.remove(first)

                val iterator = sortedBoxes.iterator()
                while (iterator.hasNext()) {
                    val nextBox = iterator.next()
                    val iou = calculateIoU(first, nextBox)
                    if (iou >= IOU_THRESHOLD) {
                        iterator.remove()
                    }
                }
            }

            return selectedBoxes
        } catch (e: Exception) {
            Log.e("Detector", "Error in NMS", e)
            return mutableListOf()
        }
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        try {
            val x1 = maxOf(box1.x1, box2.x1)
            val y1 = maxOf(box1.y1, box2.y1)
            val x2 = minOf(box1.x2, box2.x2)
            val y2 = minOf(box1.y2, box2.y2)
            val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
            val box1Area = box1.w * box1.h
            val box2Area = box2.w * box2.h
            return intersectionArea / (box1Area + box2Area - intersectionArea)
        } catch (e: Exception) {
            Log.e("Detector", "Error calculating IoU", e)
            return 0f
        }
    }

    interface DetectorListener {
        fun onEmptyDetect()
        fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long)
    }

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
        private const val IOU_THRESHOLD = 0.5F
    }

    // Simulated positions
    private val userPosition = Pair(3.5f, 2.1f)
    private val poiPosition = Pair(4.0f, 2.5f)
    private val userHeadingDegrees = 45f
    private val userHeadingRad = Math.toRadians(userHeadingDegrees.toDouble())
    private val cosHeading = Math.cos(userHeadingRad)
    private val sinHeading = Math.sin(userHeadingRad)

    data class DetectionMeta(val distance: Float, val time: Long)
    private var lastSpoken: Pair<String, Long>? = null

    private fun filterDetections(boxes: List<BoundingBox>, frameHeight: Int): List<BoundingBox> {
        try {
            val now = System.currentTimeMillis()
            val filtered = mutableListOf<BoundingBox>()

            // For better performance, limit max number of spoken objects
            var spokenCount = 0
            val MAX_SPOKEN_OBJECTS = 1  // Even more conservative for stability

            for (box in boxes) {
                try {
                    val confidence = box.cnf
                    if (confidence < Face.CONFIDENCE_THRESHOLD) continue

                    // Compute area
                    val area = box.w * box.h
                    if (area < MIN_OBJECT_AREA) continue

                    val label = box.clsName.lowercase()
                    val distance = 1.2f // FIXME: Replace with actual distance from sensor if available

                    // POI check
                    val objectX = userPosition.first + distance * cosHeading.toFloat()
                    val objectY = userPosition.second + distance * sinHeading.toFloat()
                    val distToPoi = Math.sqrt(
                        ((objectX - poiPosition.first).toDouble().pow(2) +
                                (objectY - poiPosition.second).toDouble().pow(2))
                    ).toFloat()
                    if (distToPoi < DISTANCE_THRESHOLD_POI) continue

                    // Calculate score
                    val objectPosition = 1 - box.y2 // Assuming y2 is normalized [0,1]
                    val score = 0.6f * confidence + 0.4f * objectPosition

                    val isDangerous = label in dangerousObjects
                    val send = isDangerous || score > SCORE_THRESHOLD
                    if (!send) continue

                    val last = recentDetections[label]
                    val tooOld = last == null || (now - last.time) > CACHE_TIMEOUT_SECONDS * 1000
                    val muchCloser = last == null || (distance < last.distance - DISTANCE_MARGIN)

                    // Add to filtered results for display
                    filtered.add(box)

                    // Only speak if important and not spoken recently
                    val lastLabel = lastSpoken?.first
                    val lastTime = lastSpoken?.second ?: 0
                    val recentlySpoken = label == lastLabel && (now - lastTime) < 5000 // 5 seconds

                    if ((tooOld || muchCloser) && !recentlySpoken && spokenCount < MAX_SPOKEN_OBJECTS) {
                        recentDetections[label] = DetectionMeta(distance, now)
                        lastSpoken = Pair(label, now)
                        spokenCount++

                        // Simplified speech
                        val spokenText = if (isDangerous) {
                            "Warning! $label"
                        } else {
                            "$label detected"
                        }

                        try {
                            speechHelper.speak(spokenText)
                        } catch (e: Exception) {
                            Log.e("Detector", "Error speaking: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Detector", "Error processing box: ${e.message}")
                    // Continue to next box
                }
            }

            return filtered
        } catch (e: Exception) {
            Log.e("Detector", "Error in filterDetections", e)
            return emptyList()
        }
    }
}