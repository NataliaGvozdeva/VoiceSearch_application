package ru.netology.voicesearch

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    val requestVoiceCode = 123
    lateinit var textToSpeech: TextToSpeech
    var speechRequest = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("netology voice", "start of onCreate function")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.topAppBar))

        val questionInput = findViewById<TextView>(R.id.question_input)

        val searchButton = findViewById<Button>(R.id.search_button)

        searchButton.setOnClickListener {
            textToSpeech.stop()
            askWolfram(questionInput.text.toString())
        }

        val speakButton = findViewById<Button>(R.id.speak_button)
        //Слушатель на кнопку speak_button:
        speakButton.setOnClickListener {
            textToSpeech.stop()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            //Устанавливаем сообщение, которое будет показываться перед распознаванием голоса
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to know?")

            //Отправим запрос в систему с помощью метода startActivityForResult
            try {
                //В метод startActivityForResult передаётся два параметра — наш класс Intent и код запроса.
                //Метод стартует другую программу и затем нужно обработать результат в onActivityResult
                startActivityForResult(intent, requestVoiceCode) //мы проверяем, на наш ли запрос пришёл ответ
            } catch (a: ActivityNotFoundException) {
                //Всплывающее окошечко в котором находится текст
                Toast.makeText(
                    applicationContext,
                    "Sorry your device not supported",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val answerOutput = findViewById<TextView>(R.id.answer_output)

        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener{})
        textToSpeech.language = Locale.US //Устанавливаем английский язык

        findViewById<FloatingActionButton>(R.id.read_answer).setOnClickListener {
            val answer = answerOutput.text.toString()
            //отправляем запрос на воспроизведение
            // TextToSpeech.QUEUE_ADD или QUEUE_FLUSH
            //textToSpeech.speak(answer, TextToSpeech.QUEUE_ADD, null, speechRequest.toString())
            textToSpeech.speak(answer, TextToSpeech.QUEUE_FLUSH, null)
            speechRequest +=1
        }

        Log.d("netology voice", "end of onCreate function")

    }

    //Для получения результата распознавания (т е ответ на startActivityForResult)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //мы вызываем системную реализацию этого метода
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )
        val questionInput = findViewById<TextView>(R.id.question_input)
        if (requestCode == requestVoiceCode) {
            if (resultCode == RESULT_OK && data != null) {
                //проверка, удачно ли выполнился запрос и есть ли какая-то информация в ответе
                val result: ArrayList<String>? =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) //получаем результат из ответного Intent
                val question = result?.get(0)  ?: ""

                if (question.isNotEmpty()) {
                    if (question == "stop"){
                        Toast.makeText(
                            applicationContext,
                            "The application stoped asking process.",
                            Toast.LENGTH_LONG
                        ).show()
                        questionInput.text = question
                        return
                    }
                    else {
                        questionInput.text = question
                        //Отправка распознанного текста на поиск:
                        askWolfram(question)
                        //Вывод распознанного текста в поле вопроса:
                        //findViewById<TextView>(R.id.question_input).text = question  //выводим результат в текстовом поле
                    }
                }
            }
        }
    }

    fun askWolfram(question: String) {
        val wolframAppId = "L9UH63-AQWUU2UA9T" // change to your App Id

        val engine = WAEngine()
        engine.appID = wolframAppId
        engine.addFormat("plaintext")

        val query = engine.createQuery()
        query.input = question

        val answerText = findViewById<TextView>(R.id.answer_output)
        answerText.text = "Let me think..."

        var answer = ""
        var num = 1

        Thread(Runnable {
            val queryResult = engine.performQuery(query)

            answerText.post { //Возвращаемся на Main Thread
                if (queryResult.isError) {
                    Log.e("wolfram error", queryResult.errorMessage)
                    answerText.text = queryResult.errorMessage
                } else if (!queryResult.isSuccess) {
                    Log.e("wolfram error", "Sorry, I don't understand, can you rephrase?")
                    answerText.text = "Sorry, I don't understand, can you rephrase?"
                } else {
                    for (pod in queryResult.pods) {
                        if (!pod.isError) {
                            for (subpod in pod.subpods) {
                                for (element in subpod.contents) {
                                    if (element is WAPlainText) {
                                        Log.d("wolfram", element.text)
                                        answer = answer + "\n" + element.text
                                        answerText.text = answer
                                        num++
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ).start()
    }


}



