package dev.dotworld.dialogflowchatbot

import android.app.Activity
import android.os.AsyncTask
import com.google.cloud.dialogflow.v2beta1.*

class DialogflowResponse internal constructor(
    var activity: Activity,
    private val session: SessionName,
    private val sessionsClient: SessionsClient,
    private val queryInput: QueryInput
) :
    AsyncTask<Void?, Void?, DetectIntentResponse?>() {

    override fun doInBackground(vararg params: Void?): DetectIntentResponse? {
        try {
            val detectIntentRequest: DetectIntentRequest = DetectIntentRequest.newBuilder()
                .setSession(session.toString())
                .setQueryInput(queryInput)
                .build()
            return sessionsClient.detectIntent(detectIntentRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(response: DetectIntentResponse?) {
        (activity as MainActivity).callbackChatBot(response)
    }


}