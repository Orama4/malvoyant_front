package com.example.malvoayant.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.FaceDetector.Face
import android.media.FaceDetector.Face.CONFIDENCE_THRESHOLD
import android.os.SystemClock
import android.util.Log
import com.example.malvoayant.ui.utils.Detectorsimple.DetectionMeta
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
private val recentDetections = mutableMapOf<String, DetectionMeta>()
private val dangerousObjects = setOf("fire")
private const val CACHE_TIMEOUT_SECONDS = 5
private const val MIN_OBJECT_AREA = 0.005f
private const val DISTANCE_THRESHOLD_POI = 0.3f
private const val DISTANCE_MARGIN = 0.2f
private const val SCORE_THRESHOLD = 0.25f
class Detectorsimple(
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

    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()

    fun setup() {

        val model = FileUtil.loadMappedFile(context, modelPath)
        val options = Interpreter.Options()
        options.numThreads = 4
        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        numChannel = outputShape[1]
        numElements = outputShape[2]
        Log.d("ModelSetup", "Setting up model...")
        try {
            Log.d("ModelSetup", "Setting up model...")
            val inputStream: InputStream = context.assets.open(labelPath)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String? = reader.readLine()

            while (line != null && line != "") {
                labels.add(line)
                line = reader.readLine()
            }
            Log.d("ModelSetup", "Setting up model...")
            Log.d("ModelSetup", "Input shape: ${inputShape.joinToString()}")
            Log.d("ModelSetup", "Output shape: ${outputShape.joinToString()}")
            Log.d("ModelSetup", "numChannel: $numChannel, numElements: $numElements")

            reader.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clear() {
        interpreter?.close()
        interpreter = null
    }

    fun detect(frame: Bitmap) {
        Log.d("hello","bbbbb")
        interpreter ?: return
        if (tensorWidth == 0) return
        if (tensorHeight == 0) return
        if (numChannel == 0) return
        if (numElements == 0) return


        var inferenceTime = SystemClock.uptimeMillis()

        val resizedBitmap = Bitmap.createScaledBitmap(frame, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter?.run(imageBuffer, output.buffer)
        Log.d("Detector", "Output size: ${output.floatArray.size}")


        val rawBoxes = bestBox(output.floatArray)
        if (rawBoxes == null) {
            detectorListener.onEmptyDetect()
            return
        }
        Log.d("Abla", "Final boxes size: ${rawBoxes.size}")
// Ajoute la ligne suivante pour appliquer le filtre :
        val finalBoxes = filterDetections(rawBoxes, frame.height)


        if (finalBoxes.isEmpty()) {
            detectorListener.onEmptyDetect()
            return
        }

        detectorListener.onDetect(finalBoxes, inferenceTime)

        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

    }

    private fun bestBox(array: FloatArray) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
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
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
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


    // Simulated positions (à adapter selon ton contexte réel)
    private val userPosition = Pair(3.5f, 2.1f)
    private val poiPosition = Pair(4.0f, 2.5f)
    private val userHeadingDegrees = 45f
    private val userHeadingRad = Math.toRadians(userHeadingDegrees.toDouble())
    private val cosHeading = Math.cos(userHeadingRad)
    private val sinHeading = Math.sin(userHeadingRad)

    data class DetectionMeta(val distance: Float, val time: Long)
    private var lastSpoken: Pair<String, Long>? = null // <-- Add this at the top-level of Detector.kt

    private fun filterDetections(boxes: List<BoundingBox>, frameHeight: Int): List<BoundingBox> {
        val now = System.currentTimeMillis()
        val filtered = mutableListOf<BoundingBox>()

        for (box in boxes) {
            Log.d("detector", "Box: ${box.clsName}")
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

            val send = label in dangerousObjects || score > SCORE_THRESHOLD
            if (!send) continue

            val last = recentDetections[label]
            val tooOld = last == null || (now - last.time) > CACHE_TIMEOUT_SECONDS * 1000
            val muchCloser = last == null || (distance < last.distance - DISTANCE_MARGIN)

            // Only speak if it's new or much closer AND not spoken recently
            val lastLabel = lastSpoken?.first
            val lastTime = lastSpoken?.second ?: 0
            val recentlySpoken = label == lastLabel && (now - lastTime) < 3000 // 3 seconds
            Log.d("Detector", "Detected $label at $distance meters")
            if ((tooOld || muchCloser) && !recentlySpoken) {

                filtered.add(box)
                recentDetections[label] = DetectionMeta(distance, now)
                lastSpoken = Pair(label, now)

                val spokenText = "$label detected at ${"%.1f".format(distance)} meters"
                speechHelper.speak(spokenText)
            }
        }

        return filtered
    }

}
