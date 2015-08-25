package config

import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

trait ApplicationConfig {

}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  lazy val basePath = baseUrl("amls")

}
