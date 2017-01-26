package utils

import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.{Mode, Application}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}

trait GenericTestHelper extends PlaySpec with OneAppPerSuite {

  private val bindModules: Seq[GuiceableModule] = Seq()

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .bindings(bindModules:_*).in(Mode.Test)
    .build()

  implicit val messagesApi = app.injector.instanceOf[MessagesApi]
  implicit val messages = messagesApi.preferred(FakeRequest())

}
