package config

import java.util.PropertyResourceBundle
import play.api.Play
import play.api.Play.current

trait AmlsPropertiesReader {

  val value  = Play.application.resourceAsStream("amls.properties").getOrElse(throw new
      RuntimeException("amls.properties file couldn't be retrieved."))
  val propertyResource = {
    try {
      new PropertyResourceBundle(value)
    } finally {
      value.close()
    }
  }
  def getProperty(key: String) = propertyResource.getString(key).trim
  def getIntFromProperty(key: String) = propertyResource.getString(key).trim.toInt

}
object AmlsPropertiesReader extends AmlsPropertiesReader