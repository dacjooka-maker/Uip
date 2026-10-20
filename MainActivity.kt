package com.creatif.studio

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var canvasView: DesignCanvas
    private lateinit var status: TextView
    private val purple = Color.rgb(109, 93, 251)
    private val bg = Color.rgb(16, 16, 27)
    private val paper = Color.rgb(255, 255, 255)
    private val pickImage = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(14), dp(12), dp(14), dp(10))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(TextView(this).apply {
            text = "creatif"
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        })
        titleBox.addView(TextView(this).apply {
            text = "OFFLINE GRAPHICS STUDIO"
            textSize = 9f
            letterSpacing = .16f
            setTextColor(Color.rgb(173, 174, 198))
        })
        header.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(actionButton("Export PNG", purple) { exportDesign() })
        root.addView(header)

        status = TextView(this).apply {
            text = "Untitled design  •  1080 × 1350"
            textSize = 11f
            setTextColor(Color.rgb(183, 184, 205))
            setPadding(2, dp(10), 2, dp(8))
        }
        root.addView(status)

        canvasView = DesignCanvas()
        val canvasHolder = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(29, 29, 43))
            setPadding(dp(8), dp(8), dp(8), dp(8))
            addView(canvasView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        }
        root.addView(canvasHolder, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            bottomMargin = dp(10)
        })

        val toolsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        tools.addView(tool("＋ Text") { addText() })
        tools.addView(tool("▧ Photo") { openImagePicker() })
        tools.addView(tool("◼ Shape") { addShape() })
        tools.addView(tool("🎨 Background") { chooseBackground() })
        tools.addView(tool("↶ Undo") { canvasView.undo() })
        tools.addView(tool("↷ Redo") { canvasView.redo() })
        tools.addView(tool("✕ Delete") { canvasView.deleteSelected() })
        tools.addView(tool("✎ Edit text") { editSelectedText() })
        tools.addView(tool("AI Cutout") { removeBackgroundSelected() })
        tools.addView(tool("Save Cutout PNG") { saveCutoutPng() })
        tools.addView(tool("Layers") { showLayers() })
        tools.addView(tool("A− Smaller") { canvasView.scaleSelected(0.85f) })
        tools.addView(tool("A+ Larger") { canvasView.scaleSelected(1.18f) })
        tools.addView(tool("↑ Forward") { canvasView.moveSelectedLayer(1) })
        tools.addView(tool("↓ Back") { canvasView.moveSelectedLayer(-1) })
        tools.addView(tool("Aa Colour") { changeSelectedText() })
        toolsScroll.addView(tools)
        root.addView(toolsScroll)

        val hint = TextView(this).apply {
            text = "Tap an item to select • Drag to move • Use Delete to remove"
            textSize = 10f
            setTextColor(Color.rgb(157, 160, 184))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(hint)
        setContentView(root)
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(color, 14)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { action() }
        }

    private fun tool(label: String, action: () -> Unit) =
        TextView(this).apply {
            text = label
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.rgb(38, 38, 57), 12)
            isClickable = true
            setOnClickListener { action() }
            val lp = LinearLayout.LayoutParams(-2, dp(46))
            lp.marginEnd = dp(7)
            layoutParams = lp
        }

    private fun addText() {
        val input = EditText(this).apply {
            hint = "Type your headline"
            setText("YOUR DESIGN")
            textSize = 18f
            setSingleLine(false)
        }
        AlertDialog.Builder(this)
            .setTitle("Add text")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) canvasView.addText(value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try { startActivityForResult(intent, pickImage) }
        catch (_: Exception) { toast("No image picker is available.") }
    }

    @Deprecated("Legacy callback used for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImage && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) canvasView.addImage(bitmap)
                    else toast("Couldn't read that image.")
                }
            } catch (_: Exception) { toast("Couldn't open that image.") }
        }
    }

    private fun addShape() {
        val options = arrayOf("Rounded rectangle", "Circle", "Starburst")
        AlertDialog.Builder(this).setTitle("Add a shape")
            .setItems(options) { _, which -> canvasView.addShape(which) }.show()
    }

    private fun chooseBackground() {
        val names = arrayOf("Warm cream", "Midnight", "Lavender", "Coral", "Forest", "White")
        val colors = intArrayOf(
            Color.rgb(247, 237, 220), Color.rgb(20, 24, 43),
            Color.rgb(218, 210, 255), Color.rgb(250, 123, 111),
            Color.rgb(31, 87, 67), Color.WHITE
        )
        AlertDialog.Builder(this).setTitle("Canvas background")
            .setItems(names) { _, which -> canvasView.setCanvasBg(colors[which]) }.show()
    }

    private fun editSelectedText() {
        val selected = canvasView.selectedItem()
        if (selected == null || selected.type != "text") {
            toast("Select a text layer first.")
            return
        }
        val input = EditText(this).apply {
            setText(selected.text)
            textSize = 18f
            minLines = 2
        }
        AlertDialog.Builder(this).setTitle("Edit text")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) canvasView.updateSelectedText(value)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showLayers() {
        val names = canvasView.layerNames()
        if (names.isEmpty()) { toast("Your design has no layers."); return }
        AlertDialog.Builder(this).setTitle("Layers — tap to select")
            .setItems(names.toTypedArray()) { _, which -> canvasView.selectLayer(which) }
            .setNegativeButton("Close", null).show()
    }

    private fun removeBackgroundSelected() {
        val bitmap = canvasView.selectedBitmap()
        if (bitmap == null) {
            toast("Add a photo, then select its image layer first.")
            return
        }
        status.text = "AI cutout is processing… keep Creatif open"
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
        val segmenter = Segmentation.getClient(options)
        try {
            segmenter.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { mask ->
                    try {
                        val mw = mask.width
                        val mh = mask.height
                        val buffer = mask.buffer
                        buffer.rewind()
                        val probabilities = FloatArray(mw * mh)
                        for (i in probabilities.indices) probabilities[i] = buffer.float
                        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        val pixels = IntArray(output.width * output.height)
                        output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
                        for (y in 0 until output.height) {
                            val my = (y.toFloat() * mh / output.height).toInt().coerceIn(0, mh - 1)
                            for (x in 0 until output.width) {
                                val mx = (x.toFloat() * mw / output.width).toInt().coerceIn(0, mw - 1)
                                val i = y * output.width + x
                                val confidence = probabilities[my * mw + mx].coerceIn(0f, 1f)
                                val old = pixels[i]
                                val alpha = (Color.alpha(old) * confidence).toInt().coerceIn(0, 255)
                                pixels[i] = Color.argb(alpha, Color.red(old), Color.green(old), Color.blue(old))
                            }
                        }
                        output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
                        canvasView.replaceSelectedBitmap(output)
                        status.text = "AI cutout ready • offline person segmentation"
                        toast("Background removed. Export PNG to keep transparency.")
                    } catch (_: Exception) {
                        status.text = "AI cutout failed"
                        toast("Could not apply the cutout to this image.")
                    } finally { segmenter.close() }
                }
                .addOnFailureListener {
                    status.text = "AI cutout failed"
                    toast("AI cutout failed. Try a clear photo of a person.")
                    segmenter.close()
                }
        } catch (_: Exception) {
            segmenter.close()
            status.text = "AI cutout unavailable"
            toast("AI cutout could not start on this device.")
        }
    }

    private fun saveCutoutPng() {
        val bitmap = canvasView.selectedBitmap()
        if (bitmap == null) {
            toast("Select a photo layer after using AI Cutout.")
            return
        }
        try {
            val filename = "Creatif_Cutout_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Creatif/Cutouts")
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) { toast("Couldn't save the cutout."); return }
            contentResolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IllegalStateException("PNG encoding failed")
                }
            } ?: throw IllegalStateException("No output stream")
            toast("Transparent cutout saved to Pictures/Creatif/Cutouts")
        } catch (_: Exception) {
            toast("Couldn't save the cutout. Try again.")
        }
    }

    private fun changeSelectedText() {
        val selected = canvasView.selectedItem()
        if (selected == null || selected.type != "text") {
            toast("Select a text item first.")
            return
        }
        val options = arrayOf("White", "Black", "Purple", "Gold", "Coral")
        val colors = intArrayOf(Color.WHITE, Color.BLACK, purple, Color.rgb(245, 190, 78), Color.rgb(239, 91, 105))
        AlertDialog.Builder(this).setTitle("Text colour")
            .setItems(options) { _, which ->
                selected.color = colors[which]
                canvasView.invalidate()
            }.show()
    }

    private fun exportDesign() {
        try {
            val bitmap = canvasView.renderExport()
            val filename = "Creatif_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Creatif")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) { toast("Export failed."); return }
            val stream: OutputStream? = contentResolver.openOutputStream(uri)
            stream.use { out ->
                if (out == null) throw IllegalStateException("No output stream")
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            toast("Saved to Pictures/Creatif")
        } catch (e: Exception) {
            toast("Couldn't export. Please try again.")
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }

    inner class DesignCanvas : View(this@MainActivity) {
        data class Layer(var type: String, var text: String = "", var bitmap: Bitmap? = null,
                         var color: Int = Color.BLACK, var shape: Int = 0,
                         var x: Float = .5f, var y: Float = .5f, var scale: Float = 1f)

        private val layers = mutableListOf<Layer>()
        private data class Snapshot(val layers: List<Layer>, val background: Int)
        private val undoStack = ArrayDeque<Snapshot>()
        private val redoStack = ArrayDeque<Snapshot>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var canvasBg = Color.WHITE
        private var selected: Int = -1
        private var downX = 0f
        private var downY = 0f
        private var moving = false
        private val logicalW = 1080
        private val logicalH = 1350

        init {
            isFocusable = true
            layers.add(Layer("text", "YOUR EVENT", color = Color.rgb(31, 35, 58), x = .5f, y = .36f, scale = 1.0f))
            layers.add(Layer("text", "Add your message here", color = Color.rgb(91, 83, 140), x = .5f, y = .47f, scale = .5f))
        }

        fun selectedItem(): Layer? = layers.getOrNull(selected)
        fun selectedBitmap(): Bitmap? = layers.getOrNull(selected)?.takeIf { it.type == "image" }?.bitmap
        fun replaceSelectedBitmap(bitmap: Bitmap) {
            if (selected !in layers.indices) return
            saveUndo()
            layers[selected].bitmap = bitmap
            invalidate()
        }
        fun updateSelectedText(value: String) {
            if (selected !in layers.indices) return
            saveUndo()
            layers[selected].text = value
            invalidate()
        }
        fun scaleSelected(factor: Float) {
            if (selected !in layers.indices) { toast("Select an item first."); return }
            saveUndo()
            layers[selected].scale = (layers[selected].scale * factor).coerceIn(.18f, 2.2f)
            invalidate()
        }
        fun moveSelectedLayer(direction: Int) {
            if (selected !in layers.indices) { toast("Select an item first."); return }
            val target = (selected + direction).coerceIn(0, layers.lastIndex)
            if (target == selected) return
            saveUndo()
            val item = layers.removeAt(selected)
            layers.add(target, item)
            selected = target
            invalidate()
        }
        fun layerNames(): List<String> = layers.mapIndexed { index, layer ->
            val name = when (layer.type) {
                "text" -> "Text: ${layer.text.replace("\n", " ").take(28)}"
                "image" -> "Photo layer ${index + 1}"
                else -> "Shape layer ${index + 1}"
            }
            "${index + 1}. $name"
        }
        fun selectLayer(index: Int) { if (index in layers.indices) { selected = index; invalidate() } }
        fun setCanvasBg(color: Int) { saveUndo(); canvasBg = color; invalidate() }
        fun addText(text: String) {
            saveUndo()
            layers.add(Layer("text", text, color = if (canvasBg == Color.WHITE || canvasBg == Color.rgb(247,237,220)) Color.BLACK else Color.WHITE,
                x = .5f, y = .58f, scale = .7f))
            selected = layers.lastIndex
            invalidate()
        }
        fun addImage(bitmap: Bitmap) {
            saveUndo()
            val maxSide = 900
            val ratio = min(1f, maxSide.toFloat() / max(bitmap.width, bitmap.height))
            val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width*ratio).toInt(), (bitmap.height*ratio).toInt(), true) else bitmap
            layers.add(Layer("image", bitmap = scaled, x = .5f, y = .55f, scale = .55f))
            selected = layers.lastIndex
            invalidate()
        }
        fun addShape(which: Int) {
            saveUndo()
            layers.add(Layer("shape", color = intArrayOf(purple, Color.rgb(246, 173, 78), Color.rgb(239, 91, 105))[which],
                shape = which, x = .5f, y = .6f, scale = .65f))
            selected = layers.lastIndex
            invalidate()
        }
        fun deleteSelected() {
            if (selected !in layers.indices) { toast("Select an item first."); return }
            saveUndo()
            layers.removeAt(selected)
            selected = -1
            invalidate()
        }
        fun undo() {
            if (undoStack.isEmpty()) { toast("Nothing to undo yet."); return }
            redoStack.addLast(Snapshot(layers.map { it.copy() }, canvasBg))
            val previous = undoStack.removeLast()
            layers.clear(); layers.addAll(previous.layers.map { it.copy() })
            canvasBg = previous.background
            selected = -1
            invalidate()
        }
        fun redo() {
            if (redoStack.isEmpty()) { toast("Nothing to redo yet."); return }
            undoStack.addLast(Snapshot(layers.map { it.copy() }, canvasBg))
            val next = redoStack.removeLast()
            layers.clear(); layers.addAll(next.layers.map { it.copy() })
            canvasBg = next.background
            selected = -1
            invalidate()
        }
        private fun saveUndo() {
            undoStack.addLast(Snapshot(layers.map { it.copy() }, canvasBg))
            while (undoStack.size > 30) undoStack.removeFirst()
            redoStack.clear()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(29, 29, 43))
            val scale = min(width.toFloat()/logicalW, height.toFloat()/logicalH)
            val cw = logicalW * scale
            val ch = logicalH * scale
            val left = (width - cw) / 2f
            val top = (height - ch) / 2f
            canvas.save()
            canvas.translate(left, top)
            canvas.scale(scale, scale)
            paint.color = canvasBg
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, logicalW.toFloat(), logicalH.toFloat(), paint)
            drawDesign(canvas, logicalW.toFloat(), logicalH.toFloat(), false)
            canvas.restore()
            // subtle canvas frame
            paint.color = Color.rgb(70, 70, 92)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1).toFloat()
            canvas.drawRect(left, top, left+cw, top+ch, paint)
            paint.style = Paint.Style.FILL
        }

        private fun drawDesign(canvas: Canvas, w: Float, h: Float, exporting: Boolean) {
            layers.forEachIndexed { index, layer ->
                val cx = layer.x * w
                val cy = layer.y * h
                canvas.save()
                canvas.translate(cx, cy)
                when (layer.type) {
                    "text" -> {
                        paint.color = layer.color
                        paint.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
                        paint.textAlign = Paint.Align.CENTER
                        paint.textSize = 64f * layer.scale
                        val lines = wrapText(layer.text, 20)
                        val lineHeight = paint.textSize * 1.15f
                        val startY = -((lines.size - 1) * lineHeight) / 2f
                        lines.forEachIndexed { i, line -> canvas.drawText(line, 0f, startY + i*lineHeight, paint) }
                    }
                    "image" -> {
                        val b = layer.bitmap
                        if (b != null) {
                            val maxW = 520f * layer.scale
                            val ratio = min(maxW / b.width, 520f * layer.scale / b.height)
                            val bw = b.width * ratio; val bh = b.height * ratio
                            canvas.drawBitmap(b, null, RectF(-bw/2, -bh/2, bw/2, bh/2), paint)
                        }
                    }
                    "shape" -> {
                        paint.color = layer.color
                        val size = 150f * layer.scale
                        when (layer.shape) {
                            0 -> canvas.drawRoundRect(RectF(-size, -size*.55f, size, size*.55f), 28f, 28f, paint)
                            1 -> canvas.drawCircle(0f, 0f, size*.7f, paint)
                            else -> {
                                val path = Path()
                                for (i in 0 until 10) {
                                    val angle = Math.PI * i / 5.0 - Math.PI/2
                                    val r = if (i % 2 == 0) size else size*.48f
                                    val x = (kotlin.math.cos(angle)*r).toFloat()
                                    val y = (kotlin.math.sin(angle)*r).toFloat()
                                    if (i == 0) path.moveTo(x,y) else path.lineTo(x,y)
                                }
                                path.close()
                                canvas.drawPath(path, paint)
                            }
                        }
                    }
                }
                if (index == selected && !exporting) {
                    paint.color = purple
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 3f
                    canvas.drawRoundRect(RectF(-200f*layer.scale, -75f*layer.scale, 200f*layer.scale, 75f*layer.scale), 12f, 12f, paint)
                    paint.style = Paint.Style.FILL
                }
                canvas.restore()
            }
        }

        private fun wrapText(text: String, maxChars: Int): List<String> {
            val result = mutableListOf<String>()
            text.split("\n").forEach { paragraph ->
                var rest = paragraph
                while (rest.length > maxChars) {
                    var cut = rest.lastIndexOf(' ', maxChars)
                    if (cut <= 0) cut = maxChars
                    result.add(rest.substring(0, cut).trim())
                    rest = rest.substring(cut).trim()
                }
                result.add(rest)
            }
            return result
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val scale = min(width.toFloat()/logicalW, height.toFloat()/logicalH)
            val left = (width - logicalW*scale)/2f
            val top = (height - logicalH*scale)/2f
            val x = (event.x-left)/scale/logicalW
            val y = (event.y-top)/scale/logicalH
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = x; downY = y
                    selected = findLayer(x, y)
                    moving = selected >= 0
                    if (moving) saveUndo()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (moving && selected in layers.indices) {
                        layers[selected].x = x.coerceIn(.04f, .96f)
                        layers[selected].y = y.coerceIn(.04f, .96f)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> { moving = false; performClick(); return true }
            }
            return true
        }

        private fun findLayer(x: Float, y: Float): Int {
            for (i in layers.indices.reversed()) {
                val l = layers[i]
                val radiusX = if (l.type == "shape") .15f*l.scale else if (l.type == "image") .28f*l.scale else .28f*l.scale
                val radiusY = if (l.type == "shape") .12f*l.scale else .09f*l.scale
                if (kotlin.math.abs(x-l.x) < radiusX && kotlin.math.abs(y-l.y) < radiusY) return i
            }
            return -1
        }

        override fun performClick(): Boolean { super.performClick(); return true }

        fun renderExport(): Bitmap {
            val bitmap = Bitmap.createBitmap(logicalW, logicalH, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            c.drawColor(canvasBg)
            drawDesign(c, logicalW.toFloat(), logicalH.toFloat(), true)
            return bitmap
        }
    }
}
