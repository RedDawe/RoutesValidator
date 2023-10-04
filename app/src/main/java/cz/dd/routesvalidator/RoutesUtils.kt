package cz.dd.routesvalidator

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import cz.dd.routesvalidator.datamodel.Route

fun isRouteShortest(route: Route): Boolean {

    // Instantiate the RequestQueue.
    val queue = Volley.newRequestQueue(this)
    val url = "https://www.google.com"

    // Request a string response from the provided URL.
    val stringRequest = StringRequest(
        Request.Method.GET, url, { response ->
            // Display the first 500 characters of the response string.
            val receivedResponse = "Response is: ${response.substring(0, 500)}"
        },
        { val errorReceived = "That didn't work!" })

    // Add the request to the RequestQueue.
    queue.add(stringRequest)

}