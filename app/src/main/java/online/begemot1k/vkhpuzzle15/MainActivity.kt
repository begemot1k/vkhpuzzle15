package online.begemot1k.vkhpuzzle15

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import online.begemot1k.vkhpuzzle15.Util.Companion.vibrate
import online.begemot1k.vkhpuzzle15.databinding.ActivityMainBinding
import java.util.*
import kotlin.math.min


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var freeRow = 3
    var freeColumn = 3
    var plates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0)
    lateinit var bitmaps: Array<Bitmap>
    lateinit var bitmap: Bitmap
    lateinit var preferences: SharedPreferences
    val preferenceTable = "vkhPuzzle15"
    val preferencePlates = "plates"
    val preferenceRecordTime = "recordTime"
    var gameActive: Boolean = false
    var recordTime: Long = 0
    var startTime: Long = 0
    var timer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        val metrics = this.getSystemService(WindowManager::class.java).currentWindowMetrics
        val buttonSize = min(metrics.bounds.width(), metrics.bounds.height()) / 4

        var linearLayout = LinearLayout(this)
        linearLayout.id = 99
        linearLayout.orientation = LinearLayout.VERTICAL
        for (rowIndex in 0..3) {
            var row = LinearLayout(this)
            row.id = 101 + rowIndex
            row.orientation = LinearLayout.HORIZONTAL
            for (colIndex in 0..3) {
                var btn = ImageView(this)
                btn.layoutParams = LinearLayout.LayoutParams(buttonSize, buttonSize)
                btn.setPadding(2, 2, 2, 2)
                btn.id = rowIndex * 4 + colIndex
                btn.tag = rowIndex * 4 + colIndex
                btn.setBackgroundColor(Color.rgb(240, 240, 240))
                btn.setOnClickListener {
                    movePlates(it.tag as Int)
                    drawPlates()
                    checkWin()
                }
                row.addView(btn)
            }
            linearLayout.addView(row)
        }
        val statusRow = LinearLayout(this)
        statusRow.orientation = LinearLayout.VERTICAL
        val tvRecord = TextView(this)
        tvRecord.layoutParams = ViewGroup.LayoutParams(metrics.bounds.width(), 60)
        tvRecord.id = 97
        statusRow.addView(tvRecord)
        val tvGameActive = TextView(this)
        tvGameActive.layoutParams = ViewGroup.LayoutParams(metrics.bounds.width(), 60)
        tvGameActive.id = 98
        statusRow.addView(tvGameActive)
        linearLayout.addView(statusRow)

        setContentView(binding.root)
        binding.rootLayout.addView(linearLayout)
        binding.btnShuffle.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Перемешивание")
            builder.setMessage("Перемешать фишки?")
            builder.setPositiveButton("Перемешать", { _, _ ->
                run {
                    shuffle()
                    drawPlates()
                }
            })
            builder.setNegativeButton("Отмена", null)
            builder.show()
        }
        init()
        drawPlates()
    }

    fun init() {
        preferences = getSharedPreferences(preferenceTable, Context.MODE_PRIVATE)
        recordTime = preferences.getLong(preferenceRecordTime, 0)
        decodePlates(
            preferences.getString(
                preferencePlates, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0).contentToString()
            )!!
        )
        bitmap = BitmapFactory.decodeResource(resources, R.drawable.rockplate)
        bitmaps = Array<Bitmap>(16, { BitmapFactory.decodeResource(resources, R.drawable.blank512x512) })
        for (i in 0..14) {
            bitmaps[i] = createSingleImageFromMultipleImages(
                bitmap, BitmapFactory.decodeResource(
                    resources, binding.root.resources.getIdentifier(
                        "tile${i + 1}", "drawable", binding.root.context.packageName
                    )
                )
            )!!
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // no exit
                vibrate(binding.root.context)
            }
        })
        updateRecord()
        updateStatus("Игра остановлена. Нажмите перемешать для старта")
    }

    fun encodePlates(): String {
        return plates.contentToString().replace(" ", "").replace("[", "").replace("]", "")
    }

    fun decodePlates(s: String) {
        if (s.matches("""[0-9\,]+""".toRegex())) {
            var oldPlates = plates
            var zeroIndex = 15
            val s1 = s
            var n = 0
            for (arr in s1.split(",")) {
                plates[n] = arr.trim().toInt()
                if (arr.trim().toInt() == 0) zeroIndex = n
                n++
            }
            if (uniqCheck() && containCheck() && isSolvable()) {
                freeRow = zeroIndex.div(4)
                freeColumn = zeroIndex.mod(4)
            } else {
                plates = oldPlates
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val editor = preferences.edit()
        editor?.putString(preferencePlates, encodePlates())
        editor?.putLong(preferenceRecordTime, recordTime)
        editor?.apply()
    }

    fun uniqCheck(): Boolean {
        var result = true
        val list = mutableListOf<Int>()
        for (i in 0..15) {
            result = result && !list.contains(plates[i])
            list.add(plates[i])
        }
        return result
    }

    fun containCheck(): Boolean {
        var result = true
        for (i in 0..15) {
            result = result && plates.contains(i)
        }
        return true
    }

    fun createSingleImageFromMultipleImages(
        firstImage: Bitmap, secondImage: Bitmap
    ): Bitmap? {
        val result = Bitmap.createBitmap(firstImage.width, firstImage.height, firstImage.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(firstImage, 0f, 0f, null)
        canvas.drawBitmap(secondImage, 0f, 0f, null)
        return result
    }

    fun drawPlates() {
        for (i in 0..15) {
            val btn = binding.root.findViewById<ImageView>(i)
            if (plates[i] == 0) {
                btn.setImageResource(R.drawable.blank512x512)
            } else {
                btn.setImageBitmap(
                    bitmaps[plates[i] - 1]
                )
            }
        }
    }

    fun movePlates(plateIndexndex: Int) {
        val plateRow = plateIndexndex.div(4)
        val plateColumn = plateIndexndex.mod(4)

        when {
            (plateColumn == freeColumn && freeRow < plateRow) -> {
                while (freeRow < plateRow) {
                    plates[freeRow * 4 + freeColumn] = plates[(freeRow + 1) * 4 + freeColumn]
                    freeRow++
                }
                plates[freeRow * 4 + freeColumn] = 0
            }

            (plateColumn == freeColumn && freeRow > plateRow) -> {
                while (freeRow > plateRow) {
                    plates[freeRow * 4 + freeColumn] = plates[(freeRow - 1) * 4 + freeColumn]
                    freeRow--
                }
                plates[freeRow * 4 + freeColumn] = 0
            }

            (plateRow == freeRow && freeColumn < plateColumn) -> {
                while (freeColumn < plateColumn) {
                    plates[freeRow * 4 + freeColumn] = plates[(freeRow) * 4 + freeColumn + 1]
                    freeColumn++
                }
                plates[freeRow * 4 + freeColumn] = 0
            }

            (plateRow == freeRow && freeColumn > plateColumn) -> {
                while (freeColumn > plateColumn) {
                    plates[freeRow * 4 + freeColumn] = plates[(freeRow) * 4 + freeColumn - 1]
                    freeColumn--
                }
                plates[freeRow * 4 + freeColumn] = 0
            }

            else -> {
                vibrate(this, true)
                Toast.makeText(this, "нажмите на ряд или колонку с пустой фишкой", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isSolvable(): Boolean {
        var parity = 0
        val gridWidth = 4
        var row = 0 // the current row we are on
        for (i in 0 until plates.size) {
            if (i % gridWidth == 0) { // advance to next row
                row++
            }
            for (j in i + 1 until plates.size) {
                if (plates[i] > plates[j] && plates[j] != 0) {
                    parity++
                }
            }
        }
        return parity.mod(2) == 0
    }

    fun shuffle() {
        var r: Array<Double>
        var solvable = false
        var buf1: Double
        var buf2: Int
        var tries = 0;
        plates = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 0)
        freeRow = 3
        freeColumn = 3
        while (!solvable && tries <= 10) {
            r = Array<Double>(16, { Math.random() })
            for (i in 0..13) {
                for (j in i + 1..14) {
                    if (r[i] > r[j]) {
                        buf1 = r[i]; r[i] = r[j]; r[j] = buf1;
                        buf2 = plates[i]; plates[i] = plates[j]; plates[j] = buf2;
                    }
                }
            }
            solvable = isSolvable()
            tries++;
        }
        startTime = System.currentTimeMillis()
        gameActive = true
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateTimer()
            }
        }, 0, 1000)
    }

    fun updateTimer() {
        runOnUiThread {
            if (startTime.equals(0).not()) {
                if (gameActive) {
                    updateStatus("Игра: ${Util.time2string(System.currentTimeMillis() - startTime)}")
                }
            }
        }
    }

    fun updateRecord() {
        val tvR = findViewById<TextView>(97)
        if (tvR != null) {
            tvR.text = "Рекорд: ${Util.time2string(recordTime)}"
        }
    }

    fun updateStatus(status: String) {
        val tv = findViewById<TextView>(98)
        if (tv != null) {
            tv.text = status
        }

    }

    fun checkWin() {
        if (gameActive) {
            for (i in 0..14) {
                if (plates[i] != i + 1) return
            }
            val time = System.currentTimeMillis() - startTime
            var isNewRecord = recordTime.equals(0) || time < recordTime
            if (isNewRecord) {
                recordTime = time
            }
            startTime = 0
            gameActive = false
            timer.cancel()
            updateRecord()
            updateStatus("Игра остановлена. Нажмите перемешать для старта")

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Победа!")
            if (isNewRecord) {
                builder.setMessage("Пятнашки полностью собраны с новым рекордом ${Util.time2string(recordTime)}!")
            } else {
                builder.setMessage("Пятнашки полностью собраны!\nВремя: ${Util.time2string(time)}")
            }
            builder.setPositiveButton("Ура!") { _, _ -> run {} }
            builder.show()
        }
    }
}