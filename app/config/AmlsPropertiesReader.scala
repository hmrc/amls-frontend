package config

import java.util.PropertyResourceBundle
import play.api.Play
import play.api.Play.current

object AmlsPropertiesReader {

  val value  = Play.application.resourceAsStream("amls.properties").getOrElse(throw new
      RuntimeException("amls.properties file couldn't be retrieved."))
  val propertyResource = {
    try {
      new PropertyResourceBundle(value)
    } finally {
      value.close()
    }
  }

  /**
   *
   * @param key
   * @return
   */
  def getProperty(key: String) = propertyResource.getString(key).trim

}
