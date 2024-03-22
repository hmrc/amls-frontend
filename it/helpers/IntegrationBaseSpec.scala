package helpers

import config.ApplicationConfig
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

trait IntegrationBaseSpec extends PlaySpec with Matchers with GuiceOneServerPerSuite {

  def servicesConfig: Map[String, String] = Map(
    "appCache.mongo.encryptionEnabled" -> "true"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(servicesConfig)
    .build()

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val appConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
}