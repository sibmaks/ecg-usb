package xyz.dma.ecg_usb.util

import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser


/**
 * Created by maksim.drobyshev on 05-Apr-21.
 */
object ResourceUtils {
    fun getHashMapResource(context: Context, hashMapResId: Int): Map<String, String> {
        val map = HashMap<String, String>()
        val parser: XmlResourceParser = context.resources.getXml(hashMapResId)
        var key: String? = null
        var value: String? = null
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "entry") {
                        key = parser.getAttributeValue(null, "key")
                        if (null == key) {
                            parser.close()
                            throw IllegalStateException("Key is null")
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.name == "entry") {
                        if(key != null && value != null) {
                            map[key] = value
                        }
                        key = null
                        value = null
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (null != key) {
                        value = parser.text
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            throw RuntimeException("Parsing config exception", e)
        }
        return map
    }
}